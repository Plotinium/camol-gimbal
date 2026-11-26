package plotinium.camolgimbal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.slf4j.Logger;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.item.GimbalItem;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.rules.Planner;

/**
 * The preview (D-19): the result of a right click, drawn see-through on every block it would change.
 *
 * <p>The changes come from {@link Planner}, which the server runs too, with the client's copy of the camouflages. They
 * are computed once per tick and drawn every frame. The client knows nothing of claims, and the preview does not count
 * the items carried.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, value = Dist.CLIENT)
public final class GimbalPreview {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int ALPHA = 150;
    /** Larger than Camol's own drawing (1.005), so that the two do not flicker through each other. */
    private static final float SCALE = 1.01F;

    private static List<Planner.Change> changes = List.of();
    private static ClientLevel level;
    private static boolean failureLogged;

    private GimbalPreview() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        level = minecraft.level;
        try {
            changes = plan(minecraft);
        } catch (RuntimeException exception) {
            // Another mod's block can fail when asked for its placement or its rotation on the client.
            changes = List.of();
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("Cannot compute the Gimbal preview", exception);
            }
        }
    }

    private static List<Planner.Change> plan(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || player.isSpectator()
                || player.isSecondaryUseActive() || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return List.of();
        }
        ItemStack gimbal = clickingGimbal(player);
        SavedCopy copy = gimbal.get(ModRegistry.COPY);
        if (copy == null) {
            return List.of();
        }
        GimbalSettings settings = GimbalItem.settings(gimbal);
        if (!settings.preview()) {
            return List.of();
        }
        return Planner.plan(minecraft.level, ClientCamos.VIEW, player, copy, settings,
                new Planner.Hit(hit.getBlockPos(), hit.getDirection(), hit.getLocation()), player.getDirection(),
                GimbalKeys.SERIES.isDown());
    }

    /**
     * The Gimbal a right click would use: in the main hand, or in the off hand while the main hand is empty. An item
     * in the main hand usually takes the click.
     */
    private static ItemStack clickingGimbal(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModRegistry.GIMBAL)) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return main.isEmpty() && off.is(ModRegistry.GIMBAL) ? off : ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || changes.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level != level || minecraft.screen != null
                || minecraft.options.hideGui) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType target = RenderType.translucentMovingBlock();
        VertexConsumer consumer = new Ghost(buffers.getBuffer(target));
        BlockRenderDispatcher blocks = minecraft.getBlockRenderer();
        RandomSource random = RandomSource.create();
        for (Planner.Change change : changes) {
            BlockPos pos = change.pos();
            BlockState state = change.after().state();
            BakedModel model = blocks.getBlockModel(state);
            ModelData data = model.getModelData(minecraft.level, pos, state, ModelData.EMPTY);
            long seed = state.getSeed(pos);
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.scale(SCALE, SCALE, SCALE);
            pose.translate(-0.5F, -0.5F, -0.5F);
            random.setSeed(seed);
            for (RenderType type : model.getRenderTypes(state, random, data)) {
                blocks.getModelRenderer().tesselateWithoutAO(minecraft.level, model, state, pos, pose, consumer, true,
                        random, seed, OverlayTexture.NO_OVERLAY, data, type);
            }
            pose.popPose();
        }
        buffers.endBatch(target);
    }

    /** Half transparent and fully lit: inside a solid block the world's light would leave the preview black. */
    private static final class Ghost extends VertexConsumerWrapper {
        private Ghost(VertexConsumer parent) {
            super(parent);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            parent.setColor(red, green, blue, alpha * ALPHA / 255);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            parent.setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >> 16 & 0xFFFF);
            return this;
        }
    }
}
