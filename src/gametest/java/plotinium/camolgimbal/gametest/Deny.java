package plotinium.camolgimbal.gametest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import plotinium.camolgimbal.CamolGimbal;

/** Plays a claim mod in the tests: it cancels the place event at the absolute positions a test registers. */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID)
public final class Deny {
    private static final Set<BlockPos> POSITIONS = ConcurrentHashMap.newKeySet();

    private Deny() {
    }

    static void add(BlockPos absolute) {
        POSITIONS.add(absolute.immutable());
    }

    static void clear(BlockPos absolute) {
        POSITIONS.remove(absolute);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (POSITIONS.contains(event.getPos())) {
            event.setCanceled(true);
        }
    }
}
