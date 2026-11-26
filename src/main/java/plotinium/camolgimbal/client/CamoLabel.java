package plotinium.camolgimbal.client;

import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * The label under the crosshair while a Gimbal is held and the crosshair is on a camouflage: the camo block, its type
 * with Camol's own item names, and its states. State names and values use the addon's translations when it has one,
 * the plain name otherwise. {@code waterlogged} is left out: the camo draws no water.
 */
public final class CamoLabel {
    private static final int COLOR = 0xFFFFFF;
    private static final int STATE_COLOR = 0xC0C0C0;

    private CamoLabel() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null
                || GimbalInput.gimbalHand(player) == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        CamoPosition camo = CamoHelper.getClientCamoBlockState(hit.getBlockPos());
        if (camo.state().isAir()) {
            return;
        }
        List<Component> lines = lines(camo);
        Font font = minecraft.font;
        int y = graphics.guiHeight() / 2 + 12;
        int centre = graphics.guiWidth() / 2;
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            graphics.drawString(font, line, centre - font.width(line) / 2, y, i < 2 ? COLOR : STATE_COLOR, true);
            y += font.lineHeight + 1;
        }
    }

    static List<Component> lines(CamoPosition camo) {
        BlockState state = camo.state();
        List<Component> lines = new ArrayList<>();
        lines.add(state.getBlock().getName());
        lines.add(Component.translatable(SavedCopy.SOLID.equals(camo.camoType())
                ? "item.camol.solid_camo_item" : "item.camol.camo_item"));
        for (Property<?> property : state.getProperties()) {
            if (property != BlockStateProperties.WATERLOGGED) {
                lines.add(stateLine(state, property));
            }
        }
        return lines;
    }

    private static <T extends Comparable<T>> Component stateLine(BlockState state, Property<T> property) {
        String name = property.getName();
        String value = property.getName(state.getValue(property));
        return Component.translatable("label.camolgimbal.state",
                Component.translatableWithFallback("state.camolgimbal." + name, name),
                Component.translatableWithFallback("state.camolgimbal.value." + value, value));
    }
}
