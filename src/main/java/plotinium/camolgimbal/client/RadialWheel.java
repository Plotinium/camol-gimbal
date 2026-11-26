/*
 * Adapted from Create's RadialWrenchMenu
 * (src/main/java/com/simibubi/create/content/contraptions/wrench/RadialWrenchMenu.java, branch mc1.21.1/dev,
 * https://github.com/Creators-of-Create/Create).
 * Copyright (c) The Create Team / The Creators of Create. MIT License, see THIRD_PARTY_NOTICES.md.
 * Rewritten to draw with Minecraft's own functions.
 */
package plotinium.camolgimbal.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4f;

/**
 * The wheel of the Gimbal menu: one state at a time, one sector per value, sectors clockwise from the top, each with a
 * 3D preview of the block in that value.
 */
final class RadialWheel {
    private static final int SELECTED_INNER = 0x50CCCC33;
    private static final int SELECTED_OUTER = 0xA0CCCC33;
    private static final int PLAIN_INNER = 0x18FFFFFF;
    private static final int PLAIN_OUTER = 0x48FFFFFF;
    private static final int CURRENT_RING = 0xC0FFFFFF;
    private static final int INDICATOR = 0xC0CCCCCC;
    private static final int TEXT = 0xFFFFFF;
    private static final int VALUE_TEXT = 0xFFFF80;
    private static final int DIM_TEXT = 0x909090;
    /** Above this many values the sectors are too narrow for a preview each. */
    private static final int MAX_PREVIEWS = 32;
    private static final float GAP_DEGREES = 1.0F;
    private static final float DEGREES_PER_SEGMENT = 4.0F;
    /** Text over the previews. */
    private static final float TEXT_DEPTH = 250;

    private final int centreX;
    private final int centreY;
    private final int innerRadius;
    private final int outerRadius;

    RadialWheel(int centreX, int centreY, int outerRadius) {
        this.centreX = centreX;
        this.centreY = centreY;
        this.outerRadius = outerRadius;
        this.innerRadius = Math.round(outerRadius * 0.5F);
    }

    int centreX() {
        return centreX;
    }

    int centreY() {
        return centreY;
    }

    int outerRadius() {
        return outerRadius;
    }

    /** The block in each value of one state, in the order of the values. */
    static <T extends Comparable<T>> List<BlockState> valuesOf(BlockState state, Property<T> property) {
        List<BlockState> values = new ArrayList<>();
        for (T value : property.getPossibleValues()) {
            values.add(state.setValue(property, value));
        }
        return values;
    }

    /** The sector the mouse points at by its angle, or -1 when it is inside the ring's hole. */
    int sectorAt(double mouseX, double mouseY, int sectors) {
        double dx = mouseX - centreX;
        double dy = mouseY - centreY;
        if (sectors < 1 || Math.sqrt(dx * dx + dy * dy) <= innerRadius - 5) {
            return -1;
        }
        float size = 360F / sectors;
        return (int) Math.floor((Mth.positiveModulo(angle(dx, dy) + size / 2, 360F)) / size) % sectors;
    }

    /** Degrees clockwise from the top. */
    private static float angle(double dx, double dy) {
        return (float) Math.toDegrees(Mth.atan2(dx, -dy));
    }

    /**
     * @param values the block in each value of the state
     * @param current the index of the value the block has now
     * @param selected the index the mouse points at, or -1
     */
    void drawSectors(GuiGraphics graphics, List<BlockState> values, int current, int selected, float pitch,
            float yaw, double mouseX, double mouseY) {
        int sectors = values.size();
        float size = 360F / sectors;
        float gap = Math.min(GAP_DEGREES, size / 8);
        for (int i = 0; i < sectors; i++) {
            float from = i * size - size / 2 + gap / 2;
            float to = i * size + size / 2 - gap / 2;
            boolean chosen = i == selected;
            arc(graphics, innerRadius, outerRadius, from, to, chosen ? SELECTED_INNER : PLAIN_INNER,
                    chosen ? SELECTED_OUTER : PLAIN_OUTER);
            arc(graphics, innerRadius - 3, innerRadius - 2, from, to, PLAIN_OUTER, PLAIN_OUTER);
            if (chosen) {
                arc(graphics, outerRadius + 2, outerRadius + 3, from, to, SELECTED_OUTER, SELECTED_OUTER);
            }
            if (i == current) {
                arc(graphics, outerRadius - 3, outerRadius - 1, from, to, CURRENT_RING, CURRENT_RING);
            }
        }
        if (selected >= 0) {
            drawIndicator(graphics, angle(mouseX - centreX, mouseY - centreY));
        }
        graphics.flush();

        float middle = (innerRadius + outerRadius) / 2F;
        float arcLength = Mth.TWO_PI * middle / sectors;
        float blockSize = Mth.clamp(Math.min(arcLength * 0.6F, (outerRadius - innerRadius) * 0.45F), 8F, 24F);
        for (int i = 0; i < sectors; i++) {
            if (sectors > MAX_PREVIEWS && i != selected && (selected >= 0 || i != current)) {
                continue;
            }
            float radians = i * size * Mth.DEG_TO_RAD;
            BlockPreview.draw(graphics, values.get(i), centreX + Mth.sin(radians) * middle,
                    centreY - Mth.cos(radians) * middle, blockSize, pitch, yaw);
        }
    }

    /** In the hole: the state being changed, its value, and the states before and after it. */
    void drawCentre(GuiGraphics graphics, Font font, @Nullable Component previous, Component name, Component value,
            @Nullable Component next, @Nullable Component position) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, TEXT_DEPTH);
        if (previous != null) {
            line(graphics, font, previous, -28, DIM_TEXT);
        }
        line(graphics, font, name, -13, TEXT);
        line(graphics, font, value, -1, VALUE_TEXT);
        if (position != null) {
            line(graphics, font, position, 10, DIM_TEXT);
        }
        if (next != null) {
            line(graphics, font, next, 21, DIM_TEXT);
        }
        graphics.pose().popPose();
    }

    /** One line centred in the hole, cut to the width the hole has at that height. */
    private void line(GuiGraphics graphics, Font font, Component text, int offsetY, int color) {
        int furthest = Math.max(Math.abs(offsetY), Math.abs(offsetY + font.lineHeight));
        int radius = innerRadius - 4;
        int width = furthest >= radius ? 0 : (int) (2 * Math.sqrt(radius * radius - furthest * furthest));
        Component shown = Texts.fit(font, text, Math.max(width, innerRadius));
        graphics.drawCenteredString(font, shown, centreX, centreY + offsetY, color);
    }

    /** A part of a ring between two angles, with a colour at each edge. */
    private void arc(GuiGraphics graphics, float inner, float outer, float from, float to, int innerColor,
            int outerColor) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();
        int segments = Math.max(1, Mth.ceil((to - from) / DEGREES_PER_SEGMENT));
        float step = (to - from) / segments;
        for (int i = 0; i < segments; i++) {
            float a = (from + i * step) * Mth.DEG_TO_RAD;
            float b = (from + (i + 1) * step) * Mth.DEG_TO_RAD;
            // The same winding as GuiGraphics.fill, so the quads are not culled.
            vertex(buffer, matrix, outer, a, outerColor);
            vertex(buffer, matrix, inner, a, innerColor);
            vertex(buffer, matrix, inner, b, innerColor);
            vertex(buffer, matrix, outer, b, outerColor);
        }
    }

    /** A small arrow at the edge of the hole, towards the mouse. */
    private void drawIndicator(GuiGraphics graphics, float degrees) {
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix = graphics.pose().last().pose();
        float radians = degrees * Mth.DEG_TO_RAD;
        float side = 5F / innerRadius;
        // A triangle drawn as a quad whose first two corners meet at the tip.
        vertex(buffer, matrix, innerRadius - 4, radians, INDICATOR);
        vertex(buffer, matrix, innerRadius - 4, radians, INDICATOR);
        vertex(buffer, matrix, innerRadius - 10, radians - side, INDICATOR);
        vertex(buffer, matrix, innerRadius - 10, radians + side, INDICATOR);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, float radius, float radians, int color) {
        buffer.addVertex(matrix, centreX + Mth.sin(radians) * radius, centreY - Mth.cos(radians) * radius, 0)
                .setColor(color);
    }
}
