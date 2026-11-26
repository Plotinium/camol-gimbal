package plotinium.camolgimbal.server;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.common.item.CamoItem;
import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.rules.CamoView;

/**
 * A camouflage drops as an item when its block disappears without a player breaking it (D-12): the position becomes
 * air, a liquid block or a moving piston.
 *
 * <p>{@link #onBlockChanged} runs on every block change of every chunk, so it rejects as early and cheaply as it can.
 * Positions are only noted, in a concurrent queue (other mods write blocks from other threads), and handled at the
 * end of the level tick: if the position is still empty and the camo is still there, it drops and is cleared. The
 * delay steps aside for Camol: a player break clears the camo on the break event, before the block goes; Camol's
 * Sable support moves the camo right after moving the blocks. Either way the camo is gone at the end of the tick and
 * nothing drops twice. A block removed and placed again within the tick keeps its camo.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID)
public final class LostCamos {
    private static final Map<ResourceKey<Level>, Queue<BlockPos>> PENDING = new ConcurrentHashMap<>();

    private LostCamos() {
    }

    /** Called by the {@code LevelChunk.setBlockState} mixin after a real change. */
    public static void onBlockChanged(LevelChunk chunk, BlockPos pos, BlockState newState) {
        if (!(chunk.getLevel() instanceof ServerLevel level) || !isEmpty(newState)
                || !chunk.hasData(Camol.CAMO_BLOCK_MAP)) {
            return;
        }
        CamoPosition camo = chunk.getData(Camol.CAMO_BLOCK_MAP).get(String.valueOf(pos.asLong()));
        if (camo != null && !camo.state().isAir()) {
            PENDING.computeIfAbsent(level.dimension(), key -> new ConcurrentLinkedQueue<>()).add(pos.immutable());
        }
    }

    static boolean isEmpty(BlockState state) {
        return state.isAir() || state.getBlock() instanceof LiquidBlock || state.is(Blocks.MOVING_PISTON);
    }

    @SubscribeEvent
    public static void onLevelTickEnd(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            drain(level);
        }
    }

    private static void drain(ServerLevel level) {
        Queue<BlockPos> queue = PENDING.get(level.dimension());
        if (queue == null) {
            return;
        }
        CamoView view = CamoView.of(level);
        BlockPos pos;
        while ((pos = queue.poll()) != null) {
            if (!level.isLoaded(pos) || !isEmpty(level.getBlockState(pos))) {
                continue;
            }
            CamoPosition camo = view.camoAt(pos);
            if (camo == null) {
                continue;
            }
            CamoHelper.setCamo(level, Map.of(pos, new CamoPosition(SavedCopy.NORMAL, Blocks.AIR.defaultBlockState())));
            Block.popResource(level, pos, CamoItem.getCamoItem(camo.state(), SavedCopy.SOLID.equals(camo.camoType())));
        }
    }
}
