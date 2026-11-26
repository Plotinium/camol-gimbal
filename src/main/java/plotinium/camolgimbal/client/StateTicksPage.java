package plotinium.camolgimbal.client;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** The states to paste (D-19): one tick per state of the saved copy that the Gimbal offers. */
final class StateTicksPage extends MenuPage {
    private static final int MAX_WIDTH = 220;
    private static final int ROW_HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int BACK_WIDTH = 100;
    private static final ResourceLocation TICKED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation TICKED_HOVERED =
            ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final ResourceLocation UNTICKED = ResourceLocation.withDefaultNamespace("widget/checkbox");
    private static final ResourceLocation UNTICKED_HOVERED =
            ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");

    @Nullable
    private Box box;
    private int firstRow;
    private int shownRows;
    private int totalRows;

    StateTicksPage(GimbalMenuScreen screen, MenuState data, Font font) {
        super(screen, data, font);
    }

    @Override
    void build(int width, int height, Consumer<AbstractWidget> add) {
        BlockState copied = data.copyState();
        List<Property<?>> states = copied == null ? List.of() : BlockLooks.visibleStates(copied.getBlock());
        int fixed = PADDING + TITLE_HEIGHT + 6 + BUTTON_HEIGHT + PADDING;
        totalRows = states.size();
        shownRows = Math.max(0, Math.min(totalRows, (height - MARGIN - HOTBAR - fixed) / ROW_HEIGHT));
        firstRow = Mth.clamp(screen.firstTick(), 0, totalRows - shownRows);
        screen.setFirstTick(firstRow);

        int boxWidth = Math.min(MAX_WIDTH, width - 2 * MARGIN);
        int boxHeight = fixed + shownRows * ROW_HEIGHT;
        box = addBox((width - boxWidth) / 2, boxTop(height, boxHeight), boxWidth, boxHeight);
        int y = box.y() + PADDING + TITLE_HEIGHT;
        for (int i = firstRow; i < firstRow + shownRows; i++) {
            add.accept(new Tick(box.x() + PADDING, y, boxWidth - 2 * PADDING - 6, states.get(i)));
            y += ROW_HEIGHT;
        }
        add.accept(button(box.x() + (boxWidth - BACK_WIDTH) / 2, y + 6, BACK_WIDTH, text("back"),
                () -> screen.show(GimbalMenuScreen.Page.SETTINGS)));
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (box == null) {
            return;
        }
        graphics.drawCenteredString(font, text("states"), box.x() + box.width() / 2, box.y() + PADDING, TEXT);
        scrollBar(graphics, box.x() + box.width() - 5, box.y() + PADDING + TITLE_HEIGHT, shownRows * ROW_HEIGHT,
                firstRow, shownRows, totalRows);
    }

    @Override
    void scroll(int step) {
        screen.setFirstTick(Mth.clamp(firstRow + step, 0, totalRows - shownRows));
    }

    /** One state of the saved copy: ticked, it is pasted. */
    private final class Tick extends Button {
        private final String state;

        private Tick(int x, int y, int width, Property<?> property) {
            super(x, y, width, ROW_HEIGHT - 1, StateText.name(property), button -> {
            }, DEFAULT_NARRATION);
            this.state = property.getName();
            setTooltip(Tooltip.create(text("states.tick.tooltip")));
        }

        private boolean ticked() {
            return !data.settings().skippedStates().contains(state);
        }

        @Override
        public void onPress() {
            data.change(draft -> {
                if (!draft.skipped.remove(state)) {
                    draft.skipped.add(state);
                }
            });
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return CommonComponents.optionStatus(getMessage(), ticked()).copy();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            ResourceLocation sprite = ticked()
                    ? hovered ? TICKED_HOVERED : TICKED
                    : hovered ? UNTICKED_HOVERED : UNTICKED;
            graphics.blitSprite(sprite, getX(), getY(), height, height);
            graphics.drawString(font, Texts.fit(font, getMessage(), getWidth() - height - 6),
                    getX() + height + 5, getY() + 3, ticked() ? TEXT : DIM_TEXT);
        }
    }
}
