/*
 * The wheel's part in the menu (which sector the mouse points at, and applying it) is adapted from Create's
 * RadialWrenchMenu
 * (src/main/java/com/simibubi/create/content/contraptions/wrench/RadialWrenchMenu.java, branch mc1.21.1/dev,
 * https://github.com/Creators-of-Create/Create).
 * Copyright (c) The Create Team / The Creators of Create. MIT License, see THIRD_PARTY_NOTICES.md.
 */
package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import plotinium.camolgimbal.data.SavedCopy;

/**
 * The main page (D-18): the list of states on the left, the wheel of the selected state in the middle, the saved copy
 * and the way to the settings on the right. The wheel sits in the middle of the screen. With neither a camouflage in
 * front nor a saved copy, only the right column shows, in the middle.
 */
final class MainPage extends MenuPage {
    private static final int GAP = 8;
    private static final int MIN_SIDE = 90;
    private static final int MAX_SIDE = 150;
    /** The wheel the side columns leave room for on a small screen. */
    private static final int PREFERRED_RADIUS = 75;
    private static final int MAX_RADIUS = 100;
    private static final int ROW_HEIGHT = 14;
    private static final int PADDING = 4;
    private static final int PREVIEW_HEIGHT = 36;
    private static final int LINE = 10;
    private static final int MAX_VALUE_LINES = 8;
    private static final float MIN_PITCH = 20;

    private List<Property<?>> states = List.of();
    @Nullable
    private RadialWheel wheel;
    @Nullable
    private Box list;
    @Nullable
    private Box copyBox;
    private int firstRow;
    private int shownRows;

    MainPage(GimbalMenuScreen screen, MenuState data, Font font) {
        super(screen, data, font);
    }

    @Override
    void build(int width, int height, Consumer<AbstractWidget> add) {
        BlockState edited = data.editedState();
        states = edited == null ? List.of() : BlockLooks.visibleStates(edited.getBlock());

        int free = width - 2 * MARGIN - 2 * GAP;
        int side = Mth.clamp((free - 2 * (PREFERRED_RADIUS + 4)) / 2, MIN_SIDE, MAX_SIDE);
        int radius = Math.max(30, Math.min(MAX_RADIUS, Math.min((free - 2 * side) / 2 - 4, height / 2 - 24)));
        int centreX = width / 2;
        int centreY = height / 2 + 6;
        wheel = edited == null ? null : new RadialWheel(centreX, centreY, radius);

        if (!states.isEmpty()) {
            buildList(centreX - radius - 4 - GAP - side, side, height, add);
        }
        int rightX = edited == null ? centreX - side / 2 : centreX + radius + 4 + GAP;
        buildCopy(rightX, side, height, add);
    }

    private void buildList(int x, int width, int height, Consumer<AbstractWidget> add) {
        shownRows = Math.min(states.size(), (height - MARGIN - HOTBAR - 2 * PADDING) / ROW_HEIGHT);
        int selected = screen.selectedState(states.size());
        // The list follows the selection.
        firstRow = Mth.clamp(screen.firstRow(), Math.max(0, selected - shownRows + 1),
                Math.min(selected, states.size() - shownRows));
        screen.setFirstRow(firstRow);
        int boxHeight = shownRows * ROW_HEIGHT + 2 * PADDING;
        list = addBox(x, boxTop(height, boxHeight), width, boxHeight);
        for (int i = 0; i < shownRows; i++) {
            add.accept(new StateRow(x + 2, list.y() + PADDING + i * ROW_HEIGHT,
                    width - (states.size() > shownRows ? 8 : 4), firstRow + i));
        }
    }

    private void buildCopy(int x, int width, int height, Consumer<AbstractWidget> add) {
        SavedCopy copy = data.copy();
        int boxHeight = PADDING + LINE + 2;
        if (copy == null) {
            boxHeight += 2 * LINE;
        } else {
            BlockState state = data.copyState();
            boxHeight += PREVIEW_HEIGHT + copyLines(copy, state, width - 2 * PADDING).size() * LINE;
        }
        boxHeight += BUTTON_HEIGHT + PADDING + 2;
        int total = boxHeight + PADDING + BUTTON_HEIGHT + 2;
        int top = boxTop(height, total);
        copyBox = addBox(x, top, width, boxHeight);

        Button clear = button(x + PADDING, top + boxHeight - PADDING - BUTTON_HEIGHT, width - 2 * PADDING,
                text("clear"), data::clear);
        clear.setTooltip(Tooltip.create(text("clear.tooltip")));
        clear.active = copy != null;
        add.accept(clear);

        // The settings button has its own box, so that releasing the menu key beside it applies nothing.
        Box settings = addBox(x, top + boxHeight + PADDING, width, BUTTON_HEIGHT + 2);
        add.accept(button(settings.x() + 1, settings.y() + 1, width - 2, text("settings"),
                () -> screen.show(GimbalMenuScreen.Page.SETTINGS)));
    }

    /** The text under the preview: the block's name, its type when Shape Retaining, its values. */
    private List<CopyLine> copyLines(SavedCopy copy, BlockState state, int width) {
        List<CopyLine> lines = new ArrayList<>();
        for (Component line : Texts.wrap(font, state.getBlock().getName(), width, 2)) {
            lines.add(new CopyLine(line, TEXT));
        }
        if (SavedCopy.SOLID.equals(copy.type())) {
            lines.add(new CopyLine(Texts.fit(font, text("solid"), width), DIM_TEXT));
        }
        for (Component line : Texts.joined(font, StateText.phrases(state), width, MAX_VALUE_LINES)) {
            lines.add(new CopyLine(line, DIM_TEXT));
        }
        return lines;
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        renderCopy(graphics);
        if (list != null) {
            scrollBar(graphics, list.x() + list.width() - 5, list.y() + PADDING, shownRows * ROW_HEIGHT, firstRow,
                    shownRows, states.size());
        }
        renderWheel(graphics, mouseX, mouseY);
    }

    private void renderCopy(GuiGraphics graphics) {
        if (copyBox == null) {
            return;
        }
        int centre = copyBox.x() + copyBox.width() / 2;
        int width = copyBox.width() - 2 * PADDING;
        int y = copyBox.y() + PADDING;
        graphics.drawCenteredString(font, Texts.fit(font, text("saved_copy"), width), centre, y, TEXT);
        y += LINE + 2;
        SavedCopy copy = data.copy();
        if (copy == null) {
            graphics.drawCenteredString(font, Texts.fit(font, text("no_copy"), width), centre, y + 4, DIM_TEXT);
            return;
        }
        BlockState state = data.copyState();
        BlockPreview.draw(graphics, state, centre, y + PREVIEW_HEIGHT / 2F - 2, 20, 30, 45);
        y += PREVIEW_HEIGHT;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, BlockPreview.TEXT_DEPTH);
        for (CopyLine line : copyLines(copy, state, width)) {
            graphics.drawCenteredString(font, line.text(), centre, y, line.color());
            y += LINE;
        }
        graphics.pose().popPose();
    }

    private void renderWheel(GuiGraphics graphics, int mouseX, int mouseY) {
        BlockState state = data.editedState();
        LocalPlayer player = Minecraft.getInstance().player;
        if (wheel == null || state == null || player == null) {
            return;
        }
        Component title = text(data.editsCamo() ? "this_camo" : "editing_copy");
        graphics.drawCenteredString(font, title, wheel.centreX(),
                Math.max(2, wheel.centreY() - wheel.outerRadius() - 14), TEXT);
        if (states.isEmpty()) {
            graphics.drawCenteredString(font, text("nothing_to_change"), wheel.centreX(),
                    wheel.centreY() - font.lineHeight / 2, DIM_TEXT);
            return;
        }
        Property<?> property = states.get(screen.selectedState(states.size()));
        List<BlockState> values = RadialWheel.valuesOf(state, property);
        int selected = sectorAt(mouseX, mouseY, values.size());

        // Seen as the player looks at it, tilted enough to show a second face.
        float pitch = player.getXRot();
        if (Math.abs(pitch) < MIN_PITCH) {
            pitch = pitch < 0 ? -MIN_PITCH : MIN_PITCH;
        }
        wheel.drawSectors(graphics, values, values.indexOf(state), selected, pitch, player.getYRot(), mouseX, mouseY);
        wheel.drawCentre(graphics, font, StateText.name(property),
                StateText.value(selected < 0 ? state : values.get(selected), property));
    }

    /** The sector the mouse points at, or -1 in the hole and over the boxes. */
    private int sectorAt(double mouseX, double mouseY, int sectors) {
        return wheel == null || overBox(mouseX, mouseY) ? -1 : wheel.sectorAt(mouseX, mouseY, sectors);
    }

    /** Applies the value the mouse points at, if it differs; false when the mouse points at no sector. */
    boolean apply(double mouseX, double mouseY) {
        BlockState state = data.editedState();
        if (state == null || states.isEmpty()) {
            return false;
        }
        Property<?> property = states.get(screen.selectedState(states.size()));
        // The block can change between two frames, before the page is built again.
        if (!state.hasProperty(property)) {
            return false;
        }
        List<BlockState> values = RadialWheel.valuesOf(state, property);
        int selected = sectorAt(mouseX, mouseY, values.size());
        if (selected < 0) {
            return false;
        }
        if (!values.get(selected).equals(state)) {
            data.edit(values.get(selected));
        }
        return true;
    }

    @Override
    void scroll(int step) {
        if (!states.isEmpty()) {
            screen.selectState(Mth.clamp(screen.selectedState(states.size()) + step, 0, states.size() - 1));
        }
    }

    private record CopyLine(Component text, int color) {
    }

    /** One row of the list: a state and its value now. A click shows it in the wheel. */
    private final class StateRow extends Button {
        private final int index;

        private StateRow(int x, int y, int width, int index) {
            super(x, y, width, ROW_HEIGHT, StateText.name(states.get(index)),
                    button -> screen.selectState(index), DEFAULT_NARRATION);
            this.index = index;
        }

        private Component value() {
            BlockState state = data.editedState();
            Property<?> property = states.get(index);
            return state != null && state.hasProperty(property)
                    ? StateText.value(state, property) : CommonComponents.EMPTY;
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return CommonComponents.optionNameValue(getMessage(), value()).copy();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = index == screen.selectedState(states.size());
            if (selected) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), HIGHLIGHT_FILL);
            } else if (isHoveredOrFocused()) {
                graphics.fill(getX(), getY(), getRight(), getBottom(), HOVER_FILL);
            }
            int textY = getY() + 3;
            Component value = Texts.fit(font, value(), getWidth() * 3 / 5);
            int valueWidth = font.width(value);
            graphics.drawString(font, Texts.fit(font, getMessage(), getWidth() - valueWidth - 10), getX() + 3, textY,
                    selected ? HIGHLIGHT_TEXT : TEXT);
            graphics.drawString(font, value, getRight() - 3 - valueWidth, textY,
                    selected ? HIGHLIGHT_TEXT : DIM_TEXT);
        }
    }
}
