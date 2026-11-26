package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** One page of the Gimbal menu: translucent dark boxes with a thin light border, and the buttons in them. */
abstract class MenuPage {
    static final int MARGIN = 4;
    /** The bottom of the screen the boxes keep clear of: the hotbar shows through them otherwise. */
    static final int HOTBAR = 24;
    static final int BUTTON_HEIGHT = 14;
    static final int TEXT = 0xFFFFFF;
    static final int DIM_TEXT = 0xB0B0B0;
    static final int HIGHLIGHT_TEXT = 0xFFFF55;
    static final int HIGHLIGHT_FILL = 0x60C8B400;
    static final int HOVER_FILL = 0x30FFFFFF;
    private static final int BOX_FILL = 0xC0101010;
    private static final int BOX_BORDER = 0x90C0C0C0;

    final GimbalMenuScreen screen;
    final MenuState data;
    final Font font;
    private final List<Box> boxes = new ArrayList<>();

    MenuPage(GimbalMenuScreen screen, MenuState data, Font font) {
        this.screen = screen;
        this.data = data;
        this.font = font;
    }

    /** Lays the page out on a screen of this size and adds its buttons. */
    abstract void build(int width, int height, Consumer<AbstractWidget> add);

    /** Everything that is neither a box nor a button. */
    abstract void render(GuiGraphics graphics, int mouseX, int mouseY);

    /** The mouse wheel, one step: positive downwards. */
    void scroll(int step) {
    }

    final void renderBoxes(GuiGraphics graphics) {
        boxes.forEach(box -> drawBox(graphics, box.x, box.y, box.width, box.height));
    }

    static void drawBox(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BOX_FILL);
        graphics.renderOutline(x, y, width, height, BOX_BORDER);
    }

    final Box addBox(int x, int y, int width, int height) {
        Box box = new Box(x, y, width, height);
        boxes.add(box);
        return box;
    }

    final boolean overBox(double mouseX, double mouseY) {
        return boxes.stream().anyMatch(box -> box.contains(mouseX, mouseY));
    }

    /** The y that centres a box of this height, moved up where it would reach the hotbar. */
    static int boxTop(int screenHeight, int boxHeight) {
        return Math.max(MARGIN, Math.min((screenHeight - boxHeight) / 2, screenHeight - HOTBAR - boxHeight));
    }

    static Button button(int x, int y, int width, Component label, Runnable action) {
        return Button.builder(label, pressed -> action.run()).bounds(x, y, width, BUTTON_HEIGHT).build();
    }

    static Component text(String key, Object... args) {
        return Component.translatable("menu.camolgimbal." + key, args);
    }

    /** A scroll bar beside a list that shows {@code shown} of {@code total} rows from {@code first}. */
    static void scrollBar(GuiGraphics graphics, int x, int y, int height, int first, int shown, int total) {
        if (shown <= 0 || total <= shown) {
            return;
        }
        int thumb = Math.max(4, height * shown / total);
        int thumbTop = y + (height - thumb) * first / (total - shown);
        graphics.fill(x, y, x + 2, y + height, 0x40FFFFFF);
        graphics.fill(x, thumbTop, x + 2, thumbTop + thumb, 0xFFC0C0C0);
    }

    record Box(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
