/*
 * The way the menu picks a state (mouse angle, mouse wheel, release of the key, clicks) is adapted from Create's
 * RadialWrenchMenu
 * (src/main/java/com/simibubi/create/content/contraptions/wrench/RadialWrenchMenu.java, branch mc1.21.1/dev,
 * https://github.com/Creators-of-Create/Create).
 * Copyright (c) The Create Team / The Creators of Create. MIT License, see THIRD_PARTY_NOTICES.md.
 */
package plotinium.camolgimbal.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import cy.jdkdigital.camol.utils.CamoPosition;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;
import plotinium.camolgimbal.data.GimbalSettings;

/**
 * The Gimbal menu (D-18): the main page, the settings page and the list of states to paste.
 *
 * <p>Held mode (menu key): releasing the key applies the value the mouse points at and closes. Otherwise (Shift and
 * right click) the menu stays open. In both, a left click on a sector applies it and the menu stays; a right click or
 * Esc closes. Leaving the main page ends held mode: releasing the key never closes a page the player is reading.
 */
public final class GimbalMenuScreen extends Screen {
    enum Page {
        MAIN, SETTINGS, STATES
    }

    private final MenuState data;
    private boolean held;
    private Page page = Page.MAIN;
    @Nullable
    private MenuPage view;
    /** What the widgets were built from; a change of it builds them again before the next frame. */
    @Nullable
    private Built built;
    private int selectedState;
    private int firstRow;
    private int firstTick;

    private GimbalMenuScreen(boolean held, MenuState data) {
        super(Component.translatable("menu.camolgimbal.title"));
        this.held = held;
        this.data = data;
    }

    /**
     * @param hit the block in front, or null
     * @param held whether the menu key opened it, so that releasing the key closes it
     */
    static void open(@Nullable BlockHitResult hit, boolean held) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null
                || GimbalInput.gimbalHand(minecraft.player) == null) {
            return;
        }
        CamoPosition camo = hit == null ? null : ClientCamos.VIEW.camoAt(hit.getBlockPos());
        minecraft.setScreen(new GimbalMenuScreen(held, camo == null
                ? new MenuState(null, null)
                : new MenuState(hit.getBlockPos().immutable(), camo.state().getBlock())));
    }

    void show(Page next) {
        page = next;
        if (next != Page.MAIN) {
            held = false;
        }
        built = null;
    }

    /** The state the wheel shows, as an index into the states offered. */
    int selectedState(int states) {
        return Math.max(0, Math.min(states - 1, selectedState));
    }

    void selectState(int index) {
        selectedState = index;
        built = null;
    }

    int firstRow() {
        return firstRow;
    }

    void setFirstRow(int row) {
        firstRow = row;
    }

    int firstTick() {
        return firstTick;
    }

    void setFirstTick(int row) {
        if (row != firstTick) {
            firstTick = row;
            built = null;
        }
    }

    @Override
    protected void init() {
        view = switch (page) {
            case MAIN -> new MainPage(this, data, font);
            case SETTINGS -> new SettingsPage(this, data, font);
            case STATES -> new StateTicksPage(this, data, font);
        };
        view.build(width, height, this::addRenderableWidget);
        built = Built.of(data);
    }

    @Override
    public void tick() {
        if (minecraft.player == null || MenuState.gimbal().isEmpty() || data.targetLost()) {
            onClose();
            return;
        }
        data.tick();
        // The key can be released before the menu is open, and then no release reaches it.
        if (held && !menuKeyDown()) {
            releaseMenuKey();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (built == null || !built.equals(Built.of(data))) {
            rebuildWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        view.render(graphics, mouseX, mouseY);
    }

    /** A light shade instead of the blur: the block being edited stays visible. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0x50101010, 0x70101010);
        view.renderBoxes(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            onClose();
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return button == InputConstants.MOUSE_BUTTON_LEFT && view instanceof MainPage main
                && main.apply(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (held && GimbalKeys.MENU.matchesMouse(button)) {
            releaseMenuKey();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The wheel picks the state on the main page and scrolls the list of ticks. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            view.scroll(-(int) Math.signum(scrollY));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // The key repeating while it is held does nothing.
        if (GimbalKeys.MENU.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (held && GimbalKeys.MENU.matches(keyCode, scanCode)) {
            releaseMenuKey();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void releaseMenuKey() {
        // A window losing focus releases every key: that applies nothing.
        if (minecraft.isWindowActive() && view instanceof MainPage main) {
            Window window = minecraft.getWindow();
            main.apply(minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth(),
                    minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        }
        onClose();
    }

    private boolean menuKeyDown() {
        InputConstants.Key key = GimbalKeys.MENU.getKey();
        long window = minecraft.getWindow().getWindow();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            // GLFW cannot be asked about a scancode: only the release event closes the menu.
            case SCANCODE -> true;
        };
    }

    /** The copy's state counts whole: the height of its box follows its values. */
    private record Built(@Nullable Block edited, @Nullable BlockState copied, GimbalSettings settings) {
        static Built of(MenuState data) {
            BlockState edited = data.editedState();
            return new Built(edited == null ? null : edited.getBlock(), data.copyState(), data.settings());
        }
    }
}
