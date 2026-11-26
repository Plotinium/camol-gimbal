package plotinium.camolgimbal.server;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import plotinium.camolgimbal.CamolGimbal;

/** Which players hold the series key, as their clients report it. The server cannot see the key itself. */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID)
public final class SeriesKeys {
    private static final Set<UUID> HELD = ConcurrentHashMap.newKeySet();

    private SeriesKeys() {
    }

    public static boolean isHeld(Player player) {
        return HELD.contains(player.getUUID());
    }

    public static void set(Player player, boolean held) {
        if (held) {
            HELD.add(player.getUUID());
        } else {
            HELD.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        HELD.remove(event.getEntity().getUUID());
    }
}
