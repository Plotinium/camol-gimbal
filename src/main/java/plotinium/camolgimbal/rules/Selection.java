package plotinium.camolgimbal.rules;

import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import plotinium.camolgimbal.data.SeriesShape;

/**
 * The blocks of a series (D-11): neighbours touching by a face, of the same kind as the clicked block, nearest first,
 * up to the limit, in loaded surroundings only. The clicked block is always first.
 *
 * <p>Same kind: where the clicked block has a camouflage, camouflages of the same camo block; otherwise blocks
 * without a camouflage whose real block is the same.
 */
public final class Selection {
    private Selection() {
    }

    /** @param look the player's horizontal facing, used by row and column on a floor or ceiling */
    public static List<BlockPos> select(Level level, CamoView view, BlockPos origin, Direction face, Direction look,
            SeriesShape shape, int limit) {
        Predicate<BlockPos> sameKind = sameKind(level, view, origin);
        Direction[] steps = steps(shape, face, look);

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos start = origin.immutable();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos pos = queue.poll();
            result.add(pos);
            for (Direction step : steps) {
                BlockPos next = pos.relative(step);
                if (seen.add(next) && neighbourhoodLoaded(level, next) && sameKind.test(next)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    /**
     * A position joins a series only with its own chunk and the chunks of its neighbours loaded: planning it reads the
     * neighbours (the placement of stairs, fences, walls), and on the server reading an unloaded chunk loads it on the
     * spot.
     */
    public static boolean neighbourhoodLoaded(Level level, BlockPos pos) {
        return level.isLoaded(pos)
                && level.hasChunksAt(pos.getX() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getZ() + 1);
    }

    private static Predicate<BlockPos> sameKind(Level level, CamoView view, BlockPos origin) {
        CamoPosition originCamo = view.camoAt(origin);
        if (originCamo != null) {
            Block camoBlock = originCamo.state().getBlock();
            return pos -> {
                CamoPosition camo = view.camoAt(pos);
                return camo != null && camo.state().is(camoBlock);
            };
        }
        Block realBlock = level.getBlockState(origin).getBlock();
        return pos -> view.camoAt(pos) == null && level.getBlockState(pos).is(realBlock);
    }

    /** The directions a series may grow in. Row and column grow both ways from the clicked block. */
    static Direction[] steps(SeriesShape shape, Direction face, Direction look) {
        Direction horizontalLook = look.getAxis().isHorizontal() ? look : Direction.NORTH;
        boolean wall = face.getAxis().isHorizontal();
        return switch (shape) {
            case CONNECTED -> Direction.values();
            case FACE -> java.util.Arrays.stream(Direction.values())
                    .filter(direction -> direction.getAxis() != face.getAxis())
                    .toArray(Direction[]::new);
            case ROW -> {
                Direction right = wall ? face.getClockWise() : horizontalLook.getClockWise();
                yield new Direction[] {right, right.getOpposite()};
            }
            case COLUMN -> wall
                    ? new Direction[] {Direction.UP, Direction.DOWN}
                    : new Direction[] {horizontalLook, horizontalLook.getOpposite()};
        };
    }
}
