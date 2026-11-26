package plotinium.camolgimbal.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Whether a player may change the camouflage at a position: spawn protection and world border, build rights, and a
 * posted {@link BlockEvent.EntityPlaceEvent} that nobody cancels (claim mods protect placement; FTB Chunks through
 * Architectury's place event). The snapshot holds the block's current state, so the event describes no change.
 *
 * <p>Never {@link BlockEvent.BreakEvent}: Camol listens to it, drops the camo item and erases the camouflage.
 */
public final class Permissions {
    private Permissions() {
    }

    public static boolean canEdit(ServerPlayer player, BlockPos pos, Direction face, ItemStack held) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(pos) || !player.mayInteract(level, pos) || !player.mayUseItemAt(pos, face, held)) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        BlockEvent.EntityPlaceEvent event =
                new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.relative(face)), player);
        return !NeoForge.EVENT_BUS.post(event).isCanceled();
    }
}
