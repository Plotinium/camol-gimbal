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
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
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
    /** Above this many values the sectors are too narrow for a preview each. */
    private static final int MAX_PREVIEWS = 32;
    private static final float GAP_DEGREES = 1.0F;
    private static final float DEGREES_PER_SEGMENT = 4.0F;
    private static final List<Direction> COMPASS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH,
            Direction.WEST, Direction.UP, Direction.DOWN);

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

    /** The block in each value of one state, in the order of the sectors. */
    static <T extends Comparable<T>> List<BlockState> valuesOf(BlockState state, Property<T> property) {
        List<T> ordered = new ArrayList<>(property.getPossibleValues());
        // Directions go round the wheel as on a compass: north at the top, then east, south, west, up and down.
        ordered.sort(Comparator.comparingInt(RadialWheel::compassPlace));
        List<BlockState> values = new ArrayList<>();
        for (T value : ordered) {
            values.add(state.setValue(property, value));
        }
        return values;
    }

    /** Every other value keeps its own order. */
    private static int compassPlace(Object value) {
        return value instanceof Direction direction ? COMPASS.indexOf(direction) : 0;
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
        float blockSize = Mth.clamp(Math.min(arcLength * 0.7F, (outerRadius - innerRadius) * 0.6F), 8F, 28F);
        for (int i = 0; i < sectors; i++) {
            if (sectors > MAX_PREVIEWS && i != selected && (selected >= 0 || i != current)) {
                continue;
            }
            float radians = i * size * Mth.DEG_TO_RAD;
            BlockPreview.draw(graphics, values.get(i), centreX + Mth.sin(radians) * middle,
                    centreY - Mth.cos(radians) * middle, blockSize, pitch, yaw);
        }
    }

    /** In the hole: the state being changed and the value pointed at, or the current one. */
    void drawCentre(GuiGraphics graphics, Font font, Component name, Component value) {
        int width = (int) (innerRadius * 1.7F);
        List<Component> lines = Texts.wrap(font, value, width, 2);
        int y = centreY - (lines.size() + 1) * font.lineHeight / 2 - 1;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, BlockPreview.TEXT_DEPTH);
        graphics.drawCenteredString(font, Texts.fit(font, name, width), centreX, y, TEXT);
        for (Component line : lines) {
            y += font.lineHeight + 1;
            graphics.drawCenteredString(font, line, centreX, y, VALUE_TEXT);
        }
        graphics.pose().popPose();
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
