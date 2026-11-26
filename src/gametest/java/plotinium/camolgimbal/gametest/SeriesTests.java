package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.server.GimbalActions;
import plotinium.camolgimbal.server.SeriesKeys;

/** Many blocks (D-11, plan task 7): shapes, limits, running out of items, a protected block inside. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeriesTests {
    private SeriesTests() {
    }

    /** Fills the box between two relative corners with stone. */
    private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (BlockPos pos : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), Blocks.STONE.defaultBlockState());
        }
    }

    /** The relative positions in the test area that have a camo. */
    private static List<BlockPos> camouflaged(GameTestHelper helper) {
        return BlockPos.betweenClosedStream(0, 0, 0, 7, 7, 7)
                .filter(pos -> Kit.camoAt(helper, helper.absolutePos(pos)) != null)
                .map(BlockPos::immutable)
                .collect(Collectors.toList());
    }

    private static FakePlayer seriesPlayer(GameTestHelper helper, SeriesShape shape, int limit, boolean creative) {
        FakePlayer player = creative ? Kit.creative(helper) : Kit.survival(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState()), Kit.series(shape, limit)));
        SeriesKeys.set(player, true);
        return player;
    }

    private static GimbalActions.Outcome click(GameTestHelper helper, FakePlayer player, int x, int y, int z, Direction face) {
        return GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(Kit.abs(helper, x, y, z), face));
    }

    /** Connected: every face neighbour of the same kind; a block touching only by an edge or of another kind stays out. */
    @GameTest(template = Kit.EMPTY)
    public static void connectedShape(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 3, 3, 3);
        helper.getLevel().setBlockAndUpdate(Kit.abs(helper, 4, 4, 3), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(Kit.abs(helper, 4, 2, 2), Blocks.DIRT.defaultBlockState());
        FakePlayer player = seriesPlayer(helper, SeriesShape.CONNECTED, 64, true);
        click(helper, player, 2, 3, 2, Direction.UP);
        helper.assertValueEqual(camouflaged(helper).size(), 27, "camouflaged blocks");
        helper.assertTrue(Kit.camoAt(helper, Kit.abs(helper, 4, 4, 3)) == null, "an edge neighbour joined");
        helper.assertTrue(Kit.camoAt(helper, Kit.abs(helper, 4, 2, 2)) == null, "dirt joined a stone series");
        helper.succeed();
    }

    /** Face: the plane of the clicked face. */
    @GameTest(template = Kit.EMPTY)
    public static void faceShape(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 3, 3, 3);
        FakePlayer player = seriesPlayer(helper, SeriesShape.FACE, 64, true);
        click(helper, player, 2, 3, 2, Direction.UP);
        List<BlockPos> done = camouflaged(helper);
        helper.assertValueEqual(done.size(), 9, "camouflaged blocks");
        helper.assertTrue(done.stream().allMatch(pos -> pos.getY() == 3), "a block outside the top layer: " + done);
        helper.succeed();
    }

    /** Row on a wall: left to right, both ways from the clicked block. */
    @GameTest(template = Kit.EMPTY)
    public static void rowShape(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 5, 3, 1);
        FakePlayer player = seriesPlayer(helper, SeriesShape.ROW, 64, true);
        click(helper, player, 3, 2, 1, Direction.SOUTH);
        List<BlockPos> done = camouflaged(helper);
        helper.assertValueEqual(done.size(), 5, "camouflaged blocks");
        helper.assertTrue(done.stream().allMatch(pos -> pos.getY() == 2 && pos.getZ() == 1), "not one row: " + done);
        helper.succeed();
    }

    /** Column on a wall: bottom to top; on a floor: away from the player. */
    @GameTest(template = Kit.EMPTY)
    public static void columnShape(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 3, 3, 1);
        FakePlayer wall = seriesPlayer(helper, SeriesShape.COLUMN, 64, true);
        click(helper, wall, 2, 2, 1, Direction.SOUTH);
        List<BlockPos> wallDone = camouflaged(helper);
        helper.assertValueEqual(wallDone.size(), 3, "wall column blocks");
        helper.assertTrue(wallDone.stream().allMatch(pos -> pos.getX() == 2), "not one column: " + wallDone);

        fill(helper, 5, 1, 1, 7, 1, 7);
        FakePlayer floor = seriesPlayer(helper, SeriesShape.COLUMN, 64, true);
        click(helper, floor, 6, 1, 4, Direction.UP);
        List<BlockPos> floorDone = camouflaged(helper).stream().filter(pos -> pos.getX() >= 5).toList();
        helper.assertValueEqual(floorDone.size(), 7, "floor column blocks");
        helper.assertTrue(floorDone.stream().allMatch(pos -> pos.getX() == 6), "not along the look: " + floorDone);
        helper.succeed();
    }

    private static void limit(GameTestHelper helper, int limit) {
        fill(helper, 0, 1, 0, 7, 2, 7);
        FakePlayer player = seriesPlayer(helper, SeriesShape.CONNECTED, limit, true);
        GimbalActions.Outcome outcome = click(helper, player, 3, 2, 3, Direction.UP);
        helper.assertValueEqual(outcome.written(), limit, "written");
        helper.assertValueEqual(camouflaged(helper).size(), limit, "camouflaged blocks");
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void limit8(GameTestHelper helper) {
        limit(helper, 8);
    }

    @GameTest(template = Kit.EMPTY)
    public static void limit16(GameTestHelper helper) {
        limit(helper, 16);
    }

    @GameTest(template = Kit.EMPTY)
    public static void limit32(GameTestHelper helper) {
        limit(helper, 32);
    }

    @GameTest(template = Kit.EMPTY)
    public static void limit64(GameTestHelper helper) {
        limit(helper, 64);
    }

    /** Without the series key one block only, though the same blocks are there. */
    @GameTest(template = Kit.EMPTY)
    public static void withoutTheKeyOneBlock(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 5, 1, 1);
        FakePlayer player = seriesPlayer(helper, SeriesShape.ROW, 64, true);
        SeriesKeys.set(player, false);
        click(helper, player, 3, 1, 1, Direction.UP);
        helper.assertValueEqual(camouflaged(helper).size(), 1, "camouflaged blocks without the key");
        helper.succeed();
    }

    /** The series stops at the last camo paid for. */
    @GameTest(template = Kit.EMPTY)
    public static void runsOutOfItems(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 5, 1, 1);
        FakePlayer player = seriesPlayer(helper, SeriesShape.ROW, 64, false);
        Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 3));
        GimbalActions.Outcome outcome = click(helper, player, 3, 1, 1, Direction.UP);
        helper.assertTrue(outcome.written() == 3 && outcome.ranOutOfItems(), "outcome " + outcome);
        helper.assertValueEqual(camouflaged(helper).size(), 3, "camouflaged blocks");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "camo items left");
        helper.succeed();
    }

    /** A protected block inside a series is skipped, and the series goes on past it. */
    @GameTest(template = Kit.EMPTY)
    public static void protectedBlockInsideIsSkipped(GameTestHelper helper) {
        fill(helper, 1, 1, 1, 5, 1, 1);
        BlockPos denied = Kit.abs(helper, 2, 1, 1);
        Deny.add(denied);
        try {
            FakePlayer player = seriesPlayer(helper, SeriesShape.ROW, 64, false);
            Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 10));
            GimbalActions.Outcome outcome = click(helper, player, 3, 1, 1, Direction.UP);
            helper.assertTrue(outcome.written() == 4 && outcome.refused() == 1, "outcome " + outcome);
            helper.assertTrue(Kit.camoAt(helper, denied) == null, "the protected block took a camo");
            helper.assertTrue(Kit.camoAt(helper, Kit.abs(helper, 1, 1, 1)) != null, "control: the block past it took no camo");
            helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 6, "camo items left");
        } finally {
            Deny.clear(denied);
        }
        helper.succeed();
    }
}
