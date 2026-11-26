package plotinium.camolgimbal.client;

import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import plotinium.camolgimbal.data.SavedCopy;

/**
 * The label shown while a Gimbal is held and the crosshair is on a camouflage (D-21): a small box at the left edge, at
 * mid height, with the block's icon and name and, in one compact line, the values of the states the Gimbal offers
 * ({@link BlockLooks}). It is wrapped and cut so that it stays clear of the crosshair, and hidden with F1, F3 and any
 * screen.
 */
public final class CamoLabel {
    private static final int LEFT = 4;
    private static final int PADDING = 4;
    private static final int ICON = 16;
    private static final int GAP = 4;
    private static final int LINE = 10;
    private static final int MAX_WIDTH = 170;
    /** The room left between the box and the middle of the screen. */
    private static final int CLEARANCE = 20;
    private static final int MAX_NAME_LINES = 3;
    private static final int MAX_VALUE_LINES = 4;

    private CamoLabel() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null
                || minecraft.getDebugOverlay().showDebugScreen() || GimbalInput.gimbalHand(player) == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        CamoPosition camo = ClientCamos.VIEW.camoAt(hit.getBlockPos());
        if (camo == null) {
            return;
        }
        BlockState state = camo.state();
        Font font = minecraft.font;
        int widest = Math.min(MAX_WIDTH, graphics.guiWidth() / 2 - CLEARANCE - LEFT);
        int textWidth = widest - 2 * PADDING - ICON - GAP;
        List<Component> names = names(font, camo, textWidth);
        List<Component> values = Texts.joined(font, StateText.phrases(state), textWidth, MAX_VALUE_LINES);

        int used = 0;
        for (Component line : names) {
            used = Math.max(used, font.width(line));
        }
        for (Component line : values) {
            used = Math.max(used, font.width(line));
        }
        int width = 2 * PADDING + ICON + GAP + used;
        int textHeight = (names.size() + values.size()) * LINE - 1;
        int height = Math.max(ICON, textHeight) + 2 * PADDING;
        int top = (graphics.guiHeight() - height) / 2;
        MenuPage.drawBox(graphics, LEFT, top, width, height);

        ItemStack icon = new ItemStack(state.getBlock().asItem());
        int iconX = LEFT + PADDING;
        int iconY = top + (height - ICON) / 2;
        if (icon.isEmpty()) {
            // A block without an item is drawn as itself.
            BlockPreview.draw(graphics, state, iconX + ICON / 2F, iconY + ICON / 2F, 10, 30, 45);
        } else {
            graphics.renderItem(icon, iconX, iconY);
        }

        int x = iconX + ICON + GAP;
        int y = top + (height - textHeight) / 2;
        for (Component line : names) {
            graphics.drawString(font, line, x, y, MenuPage.TEXT, true);
            y += LINE;
        }
        for (Component line : values) {
            graphics.drawString(font, line, x, y, MenuPage.DIM_TEXT, true);
            y += LINE;
        }
    }

    /** The block's name; a Shape Retaining camo says so after it, or on a line of its own when both do not fit. */
    private static List<Component> names(Font font, CamoPosition camo, int width) {
        Component name = camo.state().getBlock().getName();
        if (!SavedCopy.SOLID.equals(camo.camoType())) {
            return Texts.wrap(font, name, width, MAX_NAME_LINES);
        }
        Component both = Component.translatable("label.camolgimbal.solid", name);
        if (font.width(both) <= width) {
            return List.of(both);
        }
        List<Component> lines = new ArrayList<>(Texts.wrap(font, name, width, MAX_NAME_LINES - 1));
        lines.add(Texts.fit(font, Component.translatable("menu.camolgimbal.solid"), width));
        return lines;
    }
}
