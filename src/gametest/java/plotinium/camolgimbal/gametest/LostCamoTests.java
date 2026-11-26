package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoHelper;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SavedCopy;

/** D-12 (plan task 10): a camo drops once when its block disappears, and stays when the block only changes. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LostCamoTests {
    private LostCamoTests() {
    }

    private static BlockPos camouflaged(GameTestHelper helper, int x, int y, int z, Block real, Block camo) {
        BlockPos pos = Kit.abs(helper, x, y, z);
        helper.getLevel().setBlockAndUpdate(pos, real.defaultBlockState());
        Kit.camo(helper, pos, camo.defaultBlockState(), SavedCopy.NORMAL);
        return pos;
    }

    private static int dropsOf(GameTestHelper helper, Block camo) {
        return Kit.dropped(helper, Kit.isFilled(Camol.CAMO_ITEM.get(), camo));
    }

    /** Air and a liquid drop the camo; a block turned into another keeps it. */
    @GameTest(template = Kit.EMPTY)
    public static void removedBlockDropsChangedBlockKeeps(GameTestHelper helper) {
        BlockPos removed = camouflaged(helper, 1, 1, 1, Blocks.STONE, Blocks.OAK_PLANKS);
        BlockPos flooded = camouflaged(helper, 3, 1, 1, Blocks.STONE, Blocks.SPRUCE_PLANKS);
        BlockPos changed = camouflaged(helper, 5, 1, 1, Blocks.STONE, Blocks.BIRCH_PLANKS);
        helper.getLevel().setBlockAndUpdate(removed, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(flooded, Blocks.WATER.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(changed, Blocks.COBBLESTONE.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Kit.camoAt(helper, removed) == null, "the camo of a removed block stayed");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "drops of the removed block");
            helper.assertTrue(Kit.camoAt(helper, flooded) == null, "the camo of a flooded block stayed");
            helper.assertValueEqual(dropsOf(helper, Blocks.SPRUCE_PLANKS), 1, "drops of the flooded block");
            helper.assertTrue(Kit.camoAt(helper, changed) != null, "a block turned into another lost its camo");
            helper.assertValueEqual(dropsOf(helper, Blocks.BIRCH_PLANKS), 0, "drops of the changed block");
            helper.succeed();
        });
    }

    /** Removed and placed again within the tick: the camo stays. Removed for good, next to it: it drops. */
    @GameTest(template = Kit.EMPTY)
    public static void blockReplacedWithinTheTickKeeps(GameTestHelper helper) {
        BlockPos replaced = camouflaged(helper, 1, 1, 1, Blocks.STONE, Blocks.BIRCH_PLANKS);
        BlockPos removed = camouflaged(helper, 5, 1, 1, Blocks.STONE, Blocks.OAK_PLANKS);
        helper.getLevel().setBlockAndUpdate(replaced, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(replaced, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(removed, Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Kit.camoAt(helper, replaced) != null, "a block put back within the tick lost its camo");
            helper.assertValueEqual(dropsOf(helper, Blocks.BIRCH_PLANKS), 0, "drops of the replaced block");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "control: drops of the removed block");
            helper.succeed();
        });
    }

    /** An explosion drops the camo once; a camo out of its reach stays. */
    @GameTest(template = Kit.EMPTY)
    public static void explosionDropsTheCamo(GameTestHelper helper) {
        BlockPos blown = camouflaged(helper, 1, 1, 1, Blocks.DIRT, Blocks.OAK_PLANKS);
        BlockPos far = camouflaged(helper, 6, 1, 6, Blocks.DIRT, Blocks.BIRCH_PLANKS);
        helper.getLevel().explode(null, blown.getX() + 0.5, blown.getY() + 0.5, blown.getZ() + 0.5, 1.5F,
                Level.ExplosionInteraction.TNT);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockNotPresent(Blocks.DIRT, new BlockPos(1, 1, 1));
            helper.assertTrue(Kit.camoAt(helper, blown) == null, "the camo of a blown block stayed");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "drops of the blown block");
            helper.assertTrue(Kit.camoAt(helper, far) != null, "a camo out of reach was lost");
            helper.succeed();
        });
    }

    /** A pushed block leaves its position: the camo drops there and does not travel. */
    @GameTest(template = Kit.EMPTY, timeoutTicks = 40)
    public static void pistonPushDropsTheCamo(GameTestHelper helper) {
        BlockPos piston = Kit.abs(helper, 1, 1, 3);
        helper.getLevel().setBlockAndUpdate(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
        BlockPos pushed = camouflaged(helper, 2, 1, 3, Blocks.DIRT, Blocks.OAK_PLANKS);
        helper.getLevel().setBlockAndUpdate(piston.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(6, () -> {
            helper.assertBlockPresent(Blocks.DIRT, new BlockPos(3, 1, 3));
            helper.assertTrue(Kit.camoAt(helper, pushed) == null, "the camo stayed where the block left");
            helper.assertTrue(Kit.camoAt(helper, pushed.east()) == null, "the camo travelled with the block");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "drops of the pushed block");
            helper.succeed();
        });
    }

    /** A player break: Camol drops the camo on the break event, the addon adds nothing. */
    @GameTest(template = Kit.EMPTY)
    public static void playerBreakDropsExactlyOne(GameTestHelper helper) {
        BlockPos pos = camouflaged(helper, 2, 1, 2, Blocks.DIRT, Blocks.OAK_PLANKS);
        FakePlayer player = Kit.survival(helper);
        helper.assertTrue(player.gameMode.destroyBlock(pos), "the player could not break the block");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Kit.camoAt(helper, pos) == null, "the camo stayed after the break");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "camo items dropped by a player break");
            helper.succeed();
        });
    }

    /** Camol moves a camo itself after moving its block (its Sable support): nothing drops, the camo is moved. */
    @GameTest(template = Kit.EMPTY)
    public static void camoMovedByCamolDoesNotDrop(GameTestHelper helper) {
        BlockPos from = camouflaged(helper, 1, 1, 1, Blocks.STONE, Blocks.BIRCH_PLANKS);
        BlockPos to = Kit.abs(helper, 1, 3, 1);
        helper.getLevel().setBlockAndUpdate(to, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
        CamoHelper.moveCamo(helper.getLevel(), helper.getLevel(), List.of(from), pos -> pos.above(2), state -> state);
        BlockPos removed = camouflaged(helper, 5, 1, 1, Blocks.STONE, Blocks.OAK_PLANKS);
        helper.getLevel().setBlockAndUpdate(removed, Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Kit.camoAt(helper, to) != null, "the moved camo is not at its new place");
            helper.assertValueEqual(dropsOf(helper, Blocks.BIRCH_PLANKS), 0, "drops of the moved camo");
            helper.assertValueEqual(dropsOf(helper, Blocks.OAK_PLANKS), 1, "control: drops of the removed block");
            helper.succeed();
        });
    }
}
