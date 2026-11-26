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
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.net.EditCamoPayload;
import plotinium.camolgimbal.net.EditCopyPayload;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * The Gimbal menu (D-18): the wheel of the camouflage in front, or of the saved copy, and the side panel.
 *
 * <p>Held mode (menu key): releasing the key applies the selection and closes. Otherwise (Shift and right click) the
 * menu stays open. In both, a left click on a sector applies and closes, a right click or Esc closes.
 */
public final class GimbalMenuScreen extends Screen {
    private static final int MAX_RADIUS = 110;
    private static final int PANEL_WIDTH = 150;
    private static final int NARROW_PANEL_WIDTH = 130;
    private static final int MARGIN = 4;
    private static final float MIN_PITCH = 20;
    private static final int TEXT = 0xFFFFFF;

    private final boolean held;
    /** The camouflage being edited, or null when the wheel edits the saved copy. */
    @Nullable
    private final BlockPos target;
    @Nullable
    private final Block targetBlock;
    private final GimbalPanel panel = new GimbalPanel();
    private RadialWheel wheel;
    private int stateIndex;

    private boolean opened;
    /** The mouse when the menu opened: releasing the key selects nothing until it has moved. */
    private double openMouseX;
    private double openMouseY;

    private GimbalMenuScreen(boolean held, @Nullable BlockPos target, @Nullable Block targetBlock) {
        super(Component.translatable("menu.camolgimbal.title"));
        this.held = held;
        this.target = target;
        this.targetBlock = targetBlock;
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
        minecraft.setScreen(camo == null
                ? new GimbalMenuScreen(held, null, null)
                : new GimbalMenuScreen(held, hit.getBlockPos().immutable(), camo.state().getBlock()));
    }

    @Override
    protected void init() {
        int panelWidth = width >= 360 ? PANEL_WIDTH : NARROW_PANEL_WIDTH;
        int panelX = width - panelWidth - MARGIN;
        panel.build(panelX, panelWidth, height, this::addRenderableWidget);

        int area = panelX - MARGIN;
        int radius = Math.max(40, Math.min(MAX_RADIUS, Math.min(height / 2 - 16, area / 2 - MARGIN)));
        // In the middle of the screen when it fits beside the panel, else in the middle of the space left.
        int centreX = width / 2 + radius + 6 <= panelX ? width / 2 : area / 2;
        wheel = new RadialWheel(centreX, height / 2 + 4, radius);

        if (!opened) {
            opened = true;
            openMouseX = minecraft.mouseHandler.xpos();
            openMouseY = minecraft.mouseHandler.ypos();
            // The mouse starts in the hole of the wheel, where nothing is selected.
            if (editedState() != null && centreX != width / 2) {
                Window window = minecraft.getWindow();
                GLFW.glfwSetCursorPos(window.getWindow(),
                        (double) wheel.centreX() * window.getScreenWidth() / window.getGuiScaledWidth(),
                        (double) wheel.centreY() * window.getScreenHeight() / window.getGuiScaledHeight());
            }
        }
    }

    @Override
    public void tick() {
        if (minecraft.player == null || GimbalPanel.gimbal().isEmpty() || targetLost()) {
            onClose();
            return;
        }
        // The key can be released before the menu is open, and then no release reaches it.
        if (held && !menuKeyDown()) {
            releaseMenuKey();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (panel.outdated()) {
            rebuildWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        panel.render(graphics, font);
        renderWheel(graphics, mouseX, mouseY);
    }

    /** A light shade instead of the blur: the block being edited stays visible. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0x60101010, 0x80101010);
        panel.renderBackground(graphics);
    }

    private void renderWheel(GuiGraphics graphics, int mouseX, int mouseY) {
        BlockState state = editedState();
        LocalPlayer player = minecraft.player;
        if (state == null || player == null) {
            return;
        }
        Component title = Component.translatable(target != null
                ? "menu.camolgimbal.this_camo" : "menu.camolgimbal.saved_copy");
        graphics.drawCenteredString(font, title, wheel.centreX(),
                Math.max(2, wheel.centreY() - wheel.outerRadius() - 13), TEXT);

        List<Property<?>> states = BlockLooks.visibleStates(state.getBlock());
        if (states.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("menu.camolgimbal.nothing_to_change"),
                    wheel.centreX(), wheel.centreY() - font.lineHeight / 2, TEXT);
            return;
        }
        Property<?> property = states.get(selectedProperty(states));
        List<BlockState> values = RadialWheel.valuesOf(state, property);
        int selected = mouseHasMoved() ? selection(mouseX, mouseY, values.size()) : -1;

        // Seen as the player looks at it, tilted enough to show a second face.
        float pitch = player.getXRot();
        if (Math.abs(pitch) < MIN_PITCH) {
            pitch = pitch < 0 ? -MIN_PITCH : MIN_PITCH;
        }
        wheel.drawSectors(graphics, values, values.indexOf(state), selected, pitch, player.getYRot(), mouseX, mouseY);

        int index = selectedProperty(states);
        wheel.drawCentre(graphics, font,
                index > 0 ? CamoLabel.stateName(states.get(index - 1)) : null,
                CamoLabel.stateName(property),
                CamoLabel.valueName(selected < 0 ? state : values.get(selected), property),
                index < states.size() - 1 ? CamoLabel.stateName(states.get(index + 1)) : null,
                states.size() > 1 ? Component.literal((index + 1) + "/" + states.size()) : null);
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
        if (button == InputConstants.MOUSE_BUTTON_LEFT && apply(mouseX, mouseY)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (held && GimbalKeys.MENU.matchesMouse(button)) {
            releaseMenuKey();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The wheel changes state; over the panel it scrolls the list of states. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panel.contains(mouseX, mouseY)) {
            if (panel.scroll(scrollY)) {
                rebuildWidgets();
            }
            return true;
        }
        BlockState state = editedState();
        if (state != null && scrollY != 0) {
            List<Property<?>> states = BlockLooks.visibleStates(state.getBlock());
            if (!states.isEmpty()) {
                stateIndex = Math.max(0,
                        Math.min(states.size() - 1, selectedProperty(states) - (int) Math.signum(scrollY)));
            }
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
        if (minecraft.isWindowActive() && mouseHasMoved()) {
            Window window = minecraft.getWindow();
            apply(minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth(),
                    minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        }
        onClose();
    }

    /** Sends the value the mouse points at, if it differs; false when the mouse points at no sector. */
    private boolean apply(double mouseX, double mouseY) {
        BlockState state = editedState();
        if (state == null || targetLost()) {
            return false;
        }
        List<Property<?>> states = BlockLooks.visibleStates(state.getBlock());
        if (states.isEmpty()) {
            return false;
        }
        List<BlockState> values = RadialWheel.valuesOf(state, states.get(selectedProperty(states)));
        int selected = selection(mouseX, mouseY, values.size());
        if (selected < 0) {
            return false;
        }
        BlockState chosen = values.get(selected);
        if (!chosen.equals(state)) {
            if (target != null) {
                PacketDistributor.sendToServer(new EditCamoPayload(target, chosen));
            } else {
                SavedCopy copy = panel.copy();
                if (copy != null) {
                    PacketDistributor.sendToServer(new EditCopyPayload(chosen));
                    // The preview follows at once.
                    GimbalPanel.gimbal().set(ModRegistry.COPY, copy.withState(chosen));
                }
            }
        }
        return true;
    }

    private int selection(double mouseX, double mouseY, int sectors) {
        return panel.contains(mouseX, mouseY) ? -1 : wheel.sectorAt(mouseX, mouseY, sectors);
    }

    private int selectedProperty(List<Property<?>> states) {
        return Math.max(0, Math.min(states.size() - 1, stateIndex));
    }

    /** The state the wheel shows now, or null when there is nothing to show. */
    @Nullable
    private BlockState editedState() {
        if (target != null) {
            CamoPosition camo = ClientCamos.VIEW.camoAt(target);
            return camo == null ? null : camo.state();
        }
        SavedCopy copy = panel.copy();
        return copy == null ? null : copy.state();
    }

    private boolean targetLost() {
        if (target == null) {
            return false;
        }
        CamoPosition camo = ClientCamos.VIEW.camoAt(target);
        return camo == null || !camo.state().is(targetBlock);
    }

    private boolean mouseHasMoved() {
        return minecraft.mouseHandler.xpos() != openMouseX || minecraft.mouseHandler.ypos() != openMouseY;
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
}
