package plotinium.camolgimbal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The Gimbal's key mappings, in their own controls category.
 *
 * <p>They share their default keys with other mappings on purpose: NeoForge presses every mapping bound to a key
 * (left Ctrl presses sprint and the series key; left Alt presses Create's wrench menu and ours). The context is
 * {@code IN_GAME}: a context that does not conflict with it would make NeoForge treat the key's own modifier as
 * pressed and never report the mapping as down. Both are inactive while a screen is open, so a screen that must see
 * the menu key released matches it in {@code keyReleased} with {@link KeyMapping#matches}.
 */
public final class GimbalKeys {
    public static final String CATEGORY = "key.categories.camolgimbal";

    /** Opens the menu while held; the menu applies and closes when it is released (D-18). */
    public static final KeyMapping MENU = new KeyMapping("key.camolgimbal.menu", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    /** Held while applying: many blocks (D-11). */
    public static final KeyMapping SERIES = new KeyMapping("key.camolgimbal.series", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, CATEGORY);

    private GimbalKeys() {
    }
}
