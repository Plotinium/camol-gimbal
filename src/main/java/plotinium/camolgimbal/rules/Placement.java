package plotinium.camolgimbal.rules;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The state a block takes when placed at a camouflaged position, on the face that was really clicked.
 *
 * <p>A plain {@link BlockPlaceContext} would move to the neighbour in front of the face, or take the "replacing"
 * branch on grass and snow. Here the clicked position is the camo position and nothing is being replaced. The
 * overrides read the hit result, which {@code UseOnContext} stores before this constructor body runs: the super
 * constructor already calls {@code getClickedPos()} through {@code canBeReplaced}.
 */
public final class Placement extends BlockPlaceContext {
    private Placement(Level level, @Nullable Player player, Block block, BlockHitResult hit) {
        super(level, player, InteractionHand.MAIN_HAND, block.asItem().getDefaultInstance(), hit);
        this.replaceClicked = false;
    }

    /** The placement state, or the block's default state when the block gives none or fails on this context. */
    public static BlockState stateFor(Level level, @Nullable Player player, Block block, BlockPos pos, Direction face,
            Vec3 clickLocation) {
        try {
            BlockState placed = block.getStateForPlacement(
                    new Placement(level, player, block, new BlockHitResult(clickLocation, face, pos, false)));
            if (placed != null && placed.is(block)) {
                return placed;
            }
        } catch (RuntimeException ignored) {
            // Some blocks expect a player or a real placement; they get their default state.
        }
        return block.defaultBlockState();
    }

    @Override
    public BlockPos getClickedPos() {
        return getHitResult().getBlockPos();
    }

    @Override
    public boolean replacingClickedOnBlock() {
        return false;
    }

    @Override
    public boolean canPlace() {
        return true;
    }
}
