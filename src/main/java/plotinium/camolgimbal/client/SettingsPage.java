package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SeriesShape;

/**
 * The settings page: the switches of D-19 in three groups, one row each, "label ... value button". Hovering a row
 * explains it.
 */
final class SettingsPage extends MenuPage {
    private static final int MAX_WIDTH = 320;
    private static final int VALUE_WIDTH = 104;
    private static final int ROW_HEIGHT = 16;
    /** Rows closer together on a screen too low for the others. */
    private static final int TIGHT_ROW_HEIGHT = 14;
    private static final int ROWS = 8;
    private static final int HEADING_HEIGHT = 13;
    private static final int TITLE_HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int BACK_WIDTH = 100;
    private static final int HEADING_TEXT = 0xFFD070;
    private static final int TOOLTIP_WIDTH = 190;

    private final List<Row> rows = new ArrayList<>();
    private final List<Heading> headings = new ArrayList<>();
    @Nullable
    private Box box;
    private int rowHeight = ROW_HEIGHT;

    SettingsPage(GimbalMenuScreen screen, MenuState data, Font font) {
        super(screen, data, font);
    }

    @Override
    void build(int width, int height, Consumer<AbstractWidget> add) {
        rows.clear();
        headings.clear();
        GimbalSettings settings = data.settings();
        int boxWidth = Math.min(MAX_WIDTH, width - 2 * MARGIN);
        int fixed = PADDING + TITLE_HEIGHT + 3 * HEADING_HEIGHT + 4 + BUTTON_HEIGHT + PADDING;
        rowHeight = fixed + ROWS * ROW_HEIGHT <= height - MARGIN - HOTBAR ? ROW_HEIGHT : TIGHT_ROW_HEIGHT;
        int boxHeight = fixed + ROWS * rowHeight;
        box = addBox((width - boxWidth) / 2, boxTop(height, boxHeight), boxWidth, boxHeight);
        Layout layout = new Layout(box.y() + PADDING + TITLE_HEIGHT);

        layout.heading(text("group.paste"));
        choice(layout, add, "paste_type", List.of(true, false), settings.pasteType(),
                copied -> text(copied ? "paste_type.copied" : "paste_type.keeps"),
                (draft, value) -> draft.pasteType = value);
        choice(layout, add, "direction", List.of(DirectionMode.values()), settings.direction(),
                mode -> text("direction." + mode.getSerializedName()),
                (draft, value) -> draft.direction = value);
        choice(layout, add, "replace", List.of(false, true), settings.replaceBlock(),
                replaced -> text(replaced ? "replace.replaced" : "replace.kept"),
                (draft, value) -> draft.replaceBlock = value);
        statesRow(layout, add, settings);

        layout.heading(text("group.series", GimbalKeys.SERIES.getTranslatedKeyMessage()));
        choice(layout, add, "shape", List.of(SeriesShape.values()), settings.shape(),
                shape -> text("shape." + shape.getSerializedName()),
                (draft, value) -> draft.shape = value);
        choice(layout, add, "limit", GimbalSettings.LIMITS, settings.limit(),
                limit -> Component.literal(String.valueOf(limit)),
                (draft, value) -> draft.limit = value);

        layout.heading(text("group.view"));
        choice(layout, add, "see_through", List.of(true, false), settings.seeThrough(), SettingsPage::yesNo,
                (draft, value) -> draft.seeThrough = value);
        choice(layout, add, "preview", List.of(true, false), settings.preview(), SettingsPage::yesNo,
                (draft, value) -> draft.preview = value);

        add.accept(button(box.x() + (boxWidth - BACK_WIDTH) / 2, layout.y + 4, BACK_WIDTH, text("back"),
                () -> screen.show(GimbalMenuScreen.Page.MAIN)));
    }

    /** Which states of the copy are pasted: the button says how many and opens their list. */
    private void statesRow(Layout layout, Consumer<AbstractWidget> add, GimbalSettings settings) {
        BlockState copied = data.copyState();
        List<Property<?>> states = copied == null ? List.of() : BlockLooks.visibleStates(copied.getBlock());
        long ticked = states.stream().filter(state -> !settings.skippedStates().contains(state.getName())).count();
        Component label = copied == null ? text("no_copy")
                : ticked == states.size() ? text("states.all") : text("states.some", ticked, states.size());
        Button open = button(valueX(), layout.row(text("states"), text("states.tooltip")), VALUE_WIDTH, label,
                () -> screen.show(GimbalMenuScreen.Page.STATES));
        open.active = !states.isEmpty();
        add.accept(open);
    }

    private <T> void choice(Layout layout, Consumer<AbstractWidget> add, String name, List<T> values, T value,
            Function<T, Component> label, BiConsumer<MenuState.Draft, T> setter) {
        Component title = text(name);
        int y = layout.row(title, text(name + ".tooltip"));
        add.accept(CycleButton.<T>builder(label)
                .withValues(values)
                .withInitialValue(value)
                .displayOnlyValue()
                .create(valueX(), y, VALUE_WIDTH, BUTTON_HEIGHT, title,
                        (button, next) -> data.change(draft -> setter.accept(draft, next))));
    }

    private int valueX() {
        return box.x() + box.width() - PADDING - VALUE_WIDTH;
    }

    private static Component yesNo(boolean value) {
        return Component.translatable("state.camolgimbal.value." + value);
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (box == null) {
            return;
        }
        graphics.drawCenteredString(font, text("settings"), box.x() + box.width() / 2, box.y() + PADDING, TEXT);
        int left = box.x() + PADDING;
        for (Heading heading : headings) {
            graphics.drawString(font, Texts.fit(font, heading.text(), box.width() - 2 * PADDING), left, heading.y(),
                    HEADING_TEXT);
        }
        int labelWidth = valueX() - left - 4;
        for (Row row : rows) {
            graphics.drawString(font, Texts.fit(font, row.label(), labelWidth), left, row.y() + 3, TEXT);
            if (mouseX >= box.x() && mouseX < box.x() + box.width() && mouseY >= row.y()
                    && mouseY < row.y() + rowHeight) {
                screen.setTooltipForNextRenderPass(font.split(row.tooltip(), TOOLTIP_WIDTH));
            }
        }
    }

    private record Row(int y, Component label, Component tooltip) {
    }

    private record Heading(int y, Component text) {
    }

    /** Places headings and rows one under the other. */
    private final class Layout {
        private int y;

        private Layout(int top) {
            this.y = top;
        }

        private void heading(Component text) {
            headings.add(new Heading(y + 3, text));
            y += HEADING_HEIGHT;
        }

        /** The y of the next row's button. */
        private int row(Component label, Component tooltip) {
            int top = y;
            rows.add(new Row(top, label, tooltip));
            y += rowHeight;
            return top;
        }
    }
}
