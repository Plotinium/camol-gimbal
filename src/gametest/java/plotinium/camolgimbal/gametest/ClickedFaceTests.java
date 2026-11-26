package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.utils.CamoPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.Config;

/**
 * D-13: Camol's camo item places the camouflage as placing the block on the clicked face would, with
 * air in front of the clicked face. The player faces north. With the server setting off, Camol's own placement.
 */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClickedFaceTests {
    private ClickedFaceTests() {
    }

    /** Uses a filled camo item of {@code camo} on a stone at (x, 1, z), on {@code face} at height {@code clickY}. */
    private static BlockState useCamoItem(GameTestHelper helper, FakePlayer player, Block camo, int x, int z,
            Direction face, double clickY) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        helper.assertTrue(helper.getLevel().getBlockState(pos.relative(face)).isAir(), "no air in front of the face");
        ItemStack stack = Kit.filledNormal(camo, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 click = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5))
                .with(Direction.Axis.Y, face.getAxis().isVertical() ? pos.getY() + 0.5 + face.getStepY() * 0.5 : pos.getY() + clickY);
        stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(click, face, pos, false)));
        CamoPosition placed = Kit.camoAt(helper, pos);
        helper.assertTrue(placed != null && placed.state().is(camo), "no camo placed at " + pos);
        return placed.state();
    }

    @GameTest(template = Kit.EMPTY)
    public static void followsTheClickedFace(GameTestHelper helper) {
        helper.assertTrue(Config.followClickedFace(), "the setting is not on by default");
        FakePlayer player = Kit.creative(helper);

        BlockState log = useCamoItem(helper, player, Blocks.OAK_LOG, 1, 1, Direction.UP, 1);
        helper.assertValueEqual(log.getValue(RotatedPillarBlock.AXIS), Direction.Axis.Y, "log on a top face");

        BlockState low = useCamoItem(helper, player, Blocks.OAK_TRAPDOOR, 1, 4, Direction.EAST, 0.25);
        helper.assertTrue(low.getValue(TrapDoorBlock.FACING) == Direction.EAST && low.getValue(TrapDoorBlock.HALF) == Half.BOTTOM,
                "trapdoor on the low half of a wall face: " + low);
        BlockState high = useCamoItem(helper, player, Blocks.OAK_TRAPDOOR, 4, 4, Direction.EAST, 0.75);
        helper.assertTrue(high.getValue(TrapDoorBlock.FACING) == Direction.EAST && high.getValue(TrapDoorBlock.HALF) == Half.TOP,
                "trapdoor on the high half of a wall face: " + high);
        BlockState top = useCamoItem(helper, player, Blocks.OAK_TRAPDOOR, 4, 1, Direction.UP, 1);
        helper.assertTrue(top.getValue(TrapDoorBlock.HALF) == Half.BOTTOM && top.getValue(TrapDoorBlock.FACING) == Direction.SOUTH,
                "trapdoor on a top face: " + top);
        helper.succeed();
    }

    /** With the setting off, Camol's placement: the player's facing as the clicked face. */
    @GameTest(template = Kit.EMPTY)
    public static void settingOffKeepsCamolsPlacement(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        BlockState on = useCamoItem(helper, player, Blocks.OAK_LOG, 6, 6, Direction.UP, 1);
        helper.assertValueEqual(on.getValue(RotatedPillarBlock.AXIS), Direction.Axis.Y, "control: log with the setting on");

        Config.FOLLOW_CLICKED_FACE.set(false);
        try {
            BlockState log = useCamoItem(helper, player, Blocks.OAK_LOG, 1, 1, Direction.UP, 1);
            helper.assertValueEqual(log.getValue(RotatedPillarBlock.AXIS), Direction.Axis.Z, "log with the setting off");
            BlockState trapdoor = useCamoItem(helper, player, Blocks.OAK_TRAPDOOR, 1, 4, Direction.EAST, 0.25);
            helper.assertTrue(trapdoor.getValue(TrapDoorBlock.FACING) == Direction.SOUTH
                    && trapdoor.getValue(TrapDoorBlock.HALF) == Half.TOP, "trapdoor with the setting off: " + trapdoor);
        } finally {
            Config.FOLLOW_CLICKED_FACE.set(true);
        }
        helper.succeed();
    }
}
