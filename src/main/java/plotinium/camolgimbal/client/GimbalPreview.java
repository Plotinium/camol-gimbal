package plotinium.camolgimbal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import net.minecraft.world.level.block.Block;
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
import plotinium.camolgimbal.server.Payment;

/**
 * The preview (D-19): the result of a right click, drawn see-through on every block it would change.
 *
 * <p>The changes come from {@link Planner}, which the server runs too, with the client's copy of the camouflages. They
 * are computed once per tick and drawn every frame, cut where the items carried run out ({@link Payment#affordable},
 * the server's own count). The client knows nothing of claims. Only a Gimbal in the main hand shows it: from the off
 * hand, a block's own action comes first. A block whose model fails to draw is left out from then on, logged once.
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
    private static final Set<Block> FAILED = new HashSet<>();

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
        ItemStack gimbal = player.getMainHandItem();
        if (!gimbal.is(ModRegistry.GIMBAL)) {
            return List.of();
        }
        SavedCopy copy = gimbal.get(ModRegistry.COPY);
        if (copy == null) {
            return List.of();
        }
        GimbalSettings settings = GimbalItem.settings(gimbal);
        if (!settings.preview()) {
            return List.of();
        }
        List<Planner.Change> planned = Planner.plan(minecraft.level, ClientCamos.VIEW, player, copy, settings,
                new Planner.Hit(hit.getBlockPos(), hit.getDirection(), hit.getLocation()), player.getDirection(),
                GimbalKeys.SERIES.isDown());
        return planned.subList(0, Payment.affordable(player, planned));
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
        try {
            for (Planner.Change change : changes) {
                BlockState state = change.after().state();
                if (FAILED.contains(state.getBlock())) {
                    continue;
                }
                pose.pushPose();
                try {
                    draw(minecraft, blocks, pose, consumer, random, camera, change.pos(), state);
                } catch (RuntimeException exception) {
                    // A modded model can fail outside a real chunk: that block is left out of the preview.
                    FAILED.add(state.getBlock());
                    LOGGER.warn("Cannot draw {} in the Gimbal preview; it is left out", state, exception);
                } finally {
                    pose.popPose();
                }
            }
        } finally {
            try {
                buffers.endBatch(target);
            } catch (RuntimeException exception) {
                changes = List.of();
                if (!failureLogged) {
                    failureLogged = true;
                    LOGGER.warn("Cannot draw the Gimbal preview", exception);
                }
            }
        }
    }

    private static void draw(Minecraft minecraft, BlockRenderDispatcher blocks, PoseStack pose,
            VertexConsumer consumer, RandomSource random, Vec3 camera, BlockPos pos, BlockState state) {
        BakedModel model = blocks.getBlockModel(state);
        ModelData data = model.getModelData(minecraft.level, pos, state, ModelData.EMPTY);
        long seed = state.getSeed(pos);
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5F, -0.5F, -0.5F);
        random.setSeed(seed);
        for (RenderType type : model.getRenderTypes(state, random, data)) {
            blocks.getModelRenderer().tesselateWithoutAO(minecraft.level, model, state, pos, pose, consumer, true,
                    random, seed, OverlayTexture.NO_OVERLAY, data, type);
        }
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
