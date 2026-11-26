package plotinium.camolgimbal.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import plotinium.camolgimbal.CamolGimbal;

/**
 * Short messages on the action bar: every click with the Gimbal says what it did. The texts are in
 * {@code en_us.json} and {@code it_it.json}; block names go through {@link Block#getName()}, so they show in the
 * player's language.
 *
 * <p>Holding the right button repeats the use every 4 ticks. A repeat that has nothing left to do ({@link #quiet})
 * stays silent while it follows a success closely, so it never replaces the message of the click before it.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID)
public final class Messages {
    public static final String NOTHING_COPIED = "message.camolgimbal.nothing_copied";
    public static final String NOTHING_TO_COPY = "message.camolgimbal.nothing_to_copy";
    public static final String NO_CAMO = "message.camolgimbal.no_camo";
    public static final String ITEMS_MISSING = "message.camolgimbal.items_missing";
    public static final String SERIES_STOPPED = "message.camolgimbal.series_stopped";
    public static final String REFUSED = "message.camolgimbal.refused";
    public static final String REFUSED_SOME = "message.camolgimbal.refused_some";
    public static final String COPIED = "message.camolgimbal.copied";
    public static final String PASTED = "message.camolgimbal.pasted";
    public static final String BLOCKS_CHANGED = "message.camolgimbal.blocks_changed";
    public static final String ALREADY_LIKE_COPY = "message.camolgimbal.already_like_copy";
    public static final String CANNOT_TAKE_CAMO = "message.camolgimbal.cannot_take_camo";
    public static final String REMOVED = "message.camolgimbal.removed";
    public static final String CAMO_EDITED = "message.camolgimbal.camo_edited";
    public static final String COPY_EDITED = "message.camolgimbal.copy_edited";
    public static final String COPY_CLEARED = "message.camolgimbal.copy_cleared";

    /** Longer than the 4 ticks between two repeats of a held right button. */
    static final int REPEAT_WINDOW = 6;

    /** Per player: the last tick a success was shown, or a quiet repeat followed one. */
    private static final Map<UUID, Integer> LAST_SUCCESS = new ConcurrentHashMap<>();

    /** Sees every message shown, for the game tests: fake players drop the packet. Unset in play. */
    @Nullable
    private static volatile BiConsumer<Player, Component> observer;

    private Messages() {
    }

    public static void show(Player player, String key, Object... args) {
        Component message = Component.translatable(key, args);
        BiConsumer<Player, Component> watching = observer;
        if (watching != null) {
            watching.accept(player, message);
        }
        player.displayClientMessage(message, true);
    }

    /** A message that reports something done. */
    static void success(ServerPlayer player, String key, Object... args) {
        LAST_SUCCESS.put(player.getUUID(), player.server.getTickCount());
        show(player, key, args);
    }

    /**
     * A message for a click that had nothing to do. Right after a success it is a repeat of a held button: it stays
     * silent and keeps the window open, so holding the button never hides the success.
     */
    static void quiet(ServerPlayer player, String key, Object... args) {
        int now = player.server.getTickCount();
        Integer last = LAST_SUCCESS.get(player.getUUID());
        if (last != null && now - last <= REPEAT_WINDOW) {
            LAST_SUCCESS.put(player.getUUID(), now);
            return;
        }
        show(player, key, args);
    }

    public static void itemsMissing(Player player, Block block, String type) {
        show(player, ITEMS_MISSING, Payment.camoItem(type).getDescription(), block.getName());
    }

    public static void setObserver(@Nullable BiConsumer<Player, Component> newObserver) {
        observer = newObserver;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SUCCESS.remove(event.getEntity().getUUID());
    }
}
