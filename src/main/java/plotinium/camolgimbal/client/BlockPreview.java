package plotinium.camolgimbal.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;

/** A small 3D block on a screen, drawn with Minecraft's own block renderer. */
final class BlockPreview {
    /** Above the screen's flat drawing; text over a preview must be drawn higher than this plus the block's size. */
    private static final float DEPTH = 150;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<Block> FAILED = new HashSet<>();

    private BlockPreview() {
    }

    /**
     * @param x the centre of the block on the screen
     * @param size the pixels one block edge takes
     * @param pitch how far the viewer looks down, in degrees
     * @param yaw the viewer's yaw, in degrees as on a player
     */
    static void draw(GuiGraphics graphics, BlockState state, float x, float y, float size, float pitch, float yaw) {
        if (FAILED.contains(state.getBlock())) {
            return;
        }
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, DEPTH);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 180));
        pose.translate(-0.5F, -0.5F, -0.5F);
        Lighting.setupFor3DItems();
        try {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        } catch (RuntimeException exception) {
            FAILED.add(state.getBlock());
            LOGGER.warn("Cannot draw {} in the Gimbal menu", state, exception);
        }
        graphics.flush();
        pose.popPose();
    }
}
