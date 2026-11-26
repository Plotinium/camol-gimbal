package plotinium.camolgimbal.item;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Where the item opens the menu on the client (Shift and right click). Common code holds only this interface and a
 * no-op default, so a dedicated server never loads a client class; the client sets the real opener at setup.
 */
public final class MenuHook {
    @FunctionalInterface
    public interface Opener {
        /** @param hit the block clicked, or null for a click in the air */
        void open(InteractionHand hand, @Nullable BlockHitResult hit);
    }

    private static volatile Opener opener = (hand, hit) -> {
    };

    private MenuHook() {
    }

    public static void set(Opener newOpener) {
        opener = newOpener;
    }

    public static void open(InteractionHand hand, @Nullable BlockHitResult hit) {
        opener.open(hand, hit);
    }
}
