package plotinium.camolgimbal.server;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

/** Short messages on the action bar. The texts are in {@code en_us.json} and {@code it_it.json}. */
public final class Messages {
    public static final String NOTHING_COPIED = "message.camolgimbal.nothing_copied";
    public static final String NOTHING_TO_COPY = "message.camolgimbal.nothing_to_copy";
    public static final String NO_CAMO = "message.camolgimbal.no_camo";
    public static final String ITEMS_MISSING = "message.camolgimbal.items_missing";
    public static final String SERIES_STOPPED = "message.camolgimbal.series_stopped";
    public static final String REFUSED = "message.camolgimbal.refused";
    public static final String REFUSED_SOME = "message.camolgimbal.refused_some";

    private Messages() {
    }

    public static void show(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    public static void itemsMissing(Player player, Block block, String type) {
        show(player, ITEMS_MISSING, Payment.camoItem(type).getDescription(), block.getName());
    }
}
