package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.network.PacketDistributor;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.net.ClearCopyPayload;
import plotinium.camolgimbal.net.SettingsPayload;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * The side panel of the Gimbal menu: the saved copy, the button that clears it and the switches of D-19.
 *
 * <p>A change is sent at once and also written on the client's own stack, so that the preview and the see-through
 * drawing follow without waiting. From its first change on, the panel shows its own value: an older value arriving
 * from the server meanwhile changes nothing here. After the clear button, the copy stays gone in the same way.
 */
final class GimbalPanel {
    private static final int PADDING = 4;
    private static final int HEADER_HEIGHT = 34;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_STEP = 15;
    private static final int HEADING_HEIGHT = 11;
    private static final int TICK_HEIGHT = 12;
    private static final int PREVIEW_WIDTH = 30;
    private static final int BACKGROUND = 0xB0000000;
    private static final int TEXT = 0xFFFFFF;
    private static final int DIM_TEXT = 0xC0C0C0;
    private static final ResourceLocation TICKED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation TICKED_HOVERED =
            ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final ResourceLocation UNTICKED = ResourceLocation.withDefaultNamespace("widget/checkbox");
    private static final ResourceLocation UNTICKED_HOVERED =
            ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");

    /** The value of the switches since the first change made here; null before it. */
    @Nullable
    private GimbalSettings changed;
    private boolean cleared;
    private int scroll;

    private int x;
    private int y;
    private int width;
    private int height;
    private int headingY;
    private int shownRows;
    private int totalRows;
    @Nullable
    private Block builtBlock;
    @Nullable
    private GimbalSettings builtSettings;

    /** The Gimbal the server changes for the menu: the main hand if it holds one, else the off hand. */
    static ItemStack gimbal() {
        LocalPlayer player = Minecraft.getInstance().player;
        InteractionHand hand = player == null ? null : GimbalInput.gimbalHand(player);
        return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    GimbalSettings settings() {
        return changed != null ? changed : gimbal().getOrDefault(ModRegistry.SETTINGS, GimbalSettings.DEFAULT);
    }

    @Nullable
    SavedCopy copy() {
        return cleared ? null : gimbal().get(ModRegistry.COPY);
    }

    /** Whether the widgets no longer match what they show, so the screen must build them again. */
    boolean outdated() {
        return builtBlock != copiedBlock() || !settings().equals(builtSettings);
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Scrolls the list of states; true when the widgets must be built again. */
    boolean scroll(double amount) {
        int next = Math.max(0, Math.min(totalRows - shownRows, scroll - (int) Math.signum(amount)));
        if (next == scroll) {
            return false;
        }
        scroll = next;
        builtSettings = null;
        return true;
    }

    void build(int left, int panelWidth, int screenHeight, Consumer<AbstractWidget> add) {
        GimbalSettings settings = settings();
        Block block = copiedBlock();
        builtBlock = block;
        builtSettings = settings;
        List<Property<?>> states = block == null ? List.of() : BlockLooks.visibleStates(block);

        x = left;
        width = panelWidth;
        // Header, clear button and six rows of switches.
        int fixed = PADDING + HEADER_HEIGHT + 7 * BUTTON_STEP + PADDING;
        totalRows = states.size();
        int room = screenHeight - 2 * PADDING - fixed - HEADING_HEIGHT;
        shownRows = Math.max(0, Math.min(totalRows, room / TICK_HEIGHT));
        scroll = Math.max(0, Math.min(scroll, totalRows - shownRows));
        height = fixed + (totalRows == 0 ? 0 : HEADING_HEIGHT + shownRows * TICK_HEIGHT);
        y = Math.max(PADDING, (screenHeight - height) / 2);

        int buttonX = x + PADDING;
        int buttonWidth = width - 2 * PADDING;
        int row = y + PADDING + HEADER_HEIGHT;

        Button clear = Button.builder(text("clear"), button -> clear())
                .bounds(buttonX, row, buttonWidth, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(text("clear.tooltip")))
                .build();
        clear.active = block != null;
        add.accept(clear);
        row += BUTTON_STEP;

        add.accept(onOff(buttonX, row, buttonWidth, "paste_type", settings.pasteType(),
                (draft, value) -> draft.pasteType = value));
        row += BUTTON_STEP;
        add.accept(onOff(buttonX, row, buttonWidth, "see_through", settings.seeThrough(),
                (draft, value) -> draft.seeThrough = value));
        row += BUTTON_STEP;
        add.accept(choice(buttonX, row, buttonWidth, "direction", List.of(DirectionMode.values()),
                settings.direction(), mode -> text("direction." + mode.getSerializedName()),
                mode -> text("direction." + mode.getSerializedName() + ".tooltip"),
                (draft, value) -> draft.direction = value));
        row += BUTTON_STEP;

        int shapeWidth = (buttonWidth - 2) * 2 / 3;
        Component seriesKey = GimbalKeys.SERIES.getTranslatedKeyMessage();
        add.accept(choice(buttonX, row, shapeWidth, "shape", List.of(SeriesShape.values()), settings.shape(),
                shape -> text("shape." + shape.getSerializedName()),
                shape -> Component.translatable("menu.camolgimbal.shape." + shape.getSerializedName() + ".tooltip",
                        seriesKey),
                (draft, value) -> draft.shape = value));
        add.accept(choice(buttonX + shapeWidth + 2, row, buttonWidth - shapeWidth - 2, "limit",
                GimbalSettings.LIMITS, settings.limit(), limit -> Component.literal(String.valueOf(limit)),
                limit -> Component.translatable("menu.camolgimbal.limit.tooltip", seriesKey),
                (draft, value) -> draft.limit = value));
        row += BUTTON_STEP;

        add.accept(onOff(buttonX, row, buttonWidth, "replace", settings.replaceBlock(),
                (draft, value) -> draft.replaceBlock = value));
        row += BUTTON_STEP;
        add.accept(onOff(buttonX, row, buttonWidth, "preview", settings.preview(),
                (draft, value) -> draft.preview = value));
        row += BUTTON_STEP;

        headingY = row + 2;
        row += HEADING_HEIGHT;
        for (int i = scroll; i < scroll + shownRows; i++) {
            add.accept(new StateTick(buttonX, row, buttonWidth - 4, states.get(i)));
            row += TICK_HEIGHT;
        }
    }

    void renderBackground(GuiGraphics graphics) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
    }

    /** The saved copy, the heading of the list and its scroll bar. */
    void render(GuiGraphics graphics, Font font) {
        SavedCopy copy = copy();
        int top = y + PADDING;
        if (copy == null) {
            graphics.drawCenteredString(font, Texts.fit(font, text("no_copy"), width - 2 * PADDING),
                    x + width / 2, top + (HEADER_HEIGHT - font.lineHeight) / 2 - 1, DIM_TEXT);
        } else {
            BlockPreview.draw(graphics, copy.state(), x + PADDING + PREVIEW_WIDTH / 2F,
                    top + HEADER_HEIGHT / 2F - 2, 17, 30, 45);
            int textX = x + PADDING + PREVIEW_WIDTH + 2;
            int textWidth = x + width - PADDING - textX;
            graphics.drawString(font, Texts.fit(font, copy.state().getBlock().getName(), textWidth), textX, top + 1,
                    TEXT);
            // Camol's name of the type, on up to two lines.
            List<FormattedCharSequence> type = font.split(CamoLabel.typeName(copy.type()), textWidth);
            for (int i = 0; i < Math.min(2, type.size()); i++) {
                graphics.drawString(font, type.get(i), textX, top + 11 + i * font.lineHeight, DIM_TEXT);
            }
        }
        if (totalRows > 0) {
            graphics.drawString(font, Texts.fit(font, text("states"), width - 2 * PADDING), x + PADDING, headingY,
                    TEXT);
        }
        if (totalRows > shownRows && shownRows > 0) {
            int trackTop = headingY - 2 + HEADING_HEIGHT;
            int track = shownRows * TICK_HEIGHT;
            int thumb = Math.max(4, track * shownRows / totalRows);
            int thumbTop = trackTop + (track - thumb) * scroll / (totalRows - shownRows);
            int barX = x + width - PADDING - 2;
            graphics.fill(barX, trackTop, barX + 2, trackTop + track, 0x40FFFFFF);
            graphics.fill(barX, thumbTop, barX + 2, thumbTop + thumb, 0xFFC0C0C0);
        }
    }

    @Nullable
    private Block copiedBlock() {
        SavedCopy copy = copy();
        return copy == null ? null : copy.state().getBlock();
    }

    private CycleButton<Boolean> onOff(int buttonX, int buttonY, int buttonWidth, String name, boolean value,
            Setter<Boolean> setter) {
        Tooltip tooltip = Tooltip.create(text(name + ".tooltip"));
        return CycleButton.onOffBuilder(value)
                .withTooltip(shown -> tooltip)
                .create(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, text(name),
                        (button, next) -> change(draft -> setter.set(draft, next)));
    }

    private <T> CycleButton<T> choice(int buttonX, int buttonY, int buttonWidth, String name, List<T> values,
            T value, Function<T, Component> label, Function<T, Component> tooltip, Setter<T> setter) {
        return CycleButton.<T>builder(label)
                .withValues(values)
                .withInitialValue(value)
                .withTooltip(shown -> Tooltip.create(tooltip.apply(shown)))
                .create(buttonX, buttonY, buttonWidth, BUTTON_HEIGHT, text(name),
                        (button, next) -> change(draft -> setter.set(draft, next)));
    }

    private void change(Consumer<Draft> edit) {
        Draft draft = new Draft(settings());
        edit.accept(draft);
        GimbalSettings next = draft.build();
        changed = next;
        // The widgets already show it.
        builtSettings = next;
        store(next);
        PacketDistributor.sendToServer(new SettingsPayload(next));
    }

    /** The clear button: the copy goes, and the unticked states with it (D-19), as the server does. */
    private void clear() {
        Draft draft = new Draft(settings());
        draft.skipped.clear();
        changed = draft.build();
        cleared = true;
        store(changed);
        gimbal().remove(ModRegistry.COPY);
        PacketDistributor.sendToServer(ClearCopyPayload.INSTANCE);
    }

    private static void store(GimbalSettings settings) {
        ItemStack gimbal = gimbal();
        if (gimbal.isEmpty()) {
            return;
        }
        if (settings.equals(GimbalSettings.DEFAULT)) {
            gimbal.remove(ModRegistry.SETTINGS);
        } else {
            gimbal.set(ModRegistry.SETTINGS, settings);
        }
    }

    private static Component text(String key) {
        return Component.translatable("menu.camolgimbal." + key);
    }

    @FunctionalInterface
    private interface Setter<T> {
        void set(Draft draft, T value);
    }

    /** The switches while one of them changes. */
    private static final class Draft {
        private boolean pasteType;
        private final Set<String> skipped;
        private boolean seeThrough;
        private DirectionMode direction;
        private SeriesShape shape;
        private int limit;
        private boolean replaceBlock;
        private boolean preview;

        private Draft(GimbalSettings from) {
            pasteType = from.pasteType();
            skipped = new HashSet<>(from.skippedStates());
            seeThrough = from.seeThrough();
            direction = from.direction();
            shape = from.shape();
            limit = from.limit();
            replaceBlock = from.replaceBlock();
            preview = from.preview();
        }

        private GimbalSettings build() {
            return new GimbalSettings(pasteType, Set.copyOf(new ArrayList<>(skipped)), seeThrough, direction, shape,
                    limit, replaceBlock, preview);
        }
    }

    /** One state of the saved copy: ticked, it is pasted. */
    private final class StateTick extends Button {
        private final String state;

        private StateTick(int tickX, int tickY, int tickWidth, Property<?> property) {
            super(tickX, tickY, tickWidth, TICK_HEIGHT - 1, CamoLabel.stateName(property), button -> {
            }, DEFAULT_NARRATION);
            this.state = property.getName();
            setTooltip(Tooltip.create(text("states.tooltip")));
        }

        private boolean ticked() {
            return !settings().skippedStates().contains(state);
        }

        @Override
        public void onPress() {
            change(draft -> {
                if (!draft.skipped.remove(state)) {
                    draft.skipped.add(state);
                }
            });
        }

        @Override
        protected net.minecraft.network.chat.MutableComponent createNarrationMessage() {
            return CommonComponents.optionStatus(getMessage(), ticked()).copy();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            ResourceLocation box = ticked()
                    ? hovered ? TICKED_HOVERED : TICKED
                    : hovered ? UNTICKED_HOVERED : UNTICKED;
            graphics.blitSprite(box, getX(), getY(), height, height);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, Texts.fit(font, getMessage(), getWidth() - height - 4), getX() + height + 4,
                    getY() + 2, ticked() ? TEXT : DIM_TEXT);
        }
    }
}
