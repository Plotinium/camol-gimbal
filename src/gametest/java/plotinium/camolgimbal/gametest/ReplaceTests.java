package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.server.GimbalActions;

/** Replace the block (D-19, plan task 8): the new camo is paid for, the old one comes back once. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReplaceTests {
    private ReplaceTests() {
    }

    private static BlockPos birchCamo(GameTestHelper helper, int x, int z) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        Kit.camo(helper, pos, Blocks.BIRCH_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        return pos;
    }

    private static FakePlayer replacer(GameTestHelper helper, ItemStack... stacks) {
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState()), Kit.replacing()));
        Kit.give(player, stacks);
        return player;
    }

    private static boolean isOak(GameTestHelper helper, BlockPos pos) {
        CamoPosition camo = Kit.camoAt(helper, pos);
        return camo != null && camo.state().is(Blocks.OAK_PLANKS);
    }

    @GameTest(template = Kit.EMPTY)
    public static void exchangeReturnsTheOldCamoOnce(GameTestHelper helper) {
        BlockPos pos = birchCamo(helper, 2, 2);
        FakePlayer player = replacer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        helper.assertTrue(isOak(helper, pos), "the block was not replaced");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "oak camo left");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 1, "birch camos returned");
        helper.assertValueEqual(Kit.dropped(helper), 0, "items dropped");
        helper.succeed();
    }

    /** With a full inventory the old camo drops at the player. */
    @GameTest(template = Kit.EMPTY)
    public static void fullInventoryDropsTheOldCamo(GameTestHelper helper) {
        BlockPos pos = birchCamo(helper, 2, 2);
        FakePlayer player = replacer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 2));
        while (player.getInventory().getFreeSlot() >= 0) {
            player.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
        }
        player.getInventory().offhand.set(0, new ItemStack(Items.COBBLESTONE, 64));
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        helper.assertTrue(isOak(helper, pos), "the block was not replaced");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 0, "birch camo in a full inventory");
        helper.assertValueEqual(Kit.dropped(helper, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 1, "birch camos dropped");
        helper.succeed();
    }

    /** Without the items the old camo stays and nothing is returned; with them, next to it, the block changes. */
    @GameTest(template = Kit.EMPTY)
    public static void missingItemsKeepTheOldCamo(GameTestHelper helper) {
        BlockPos unpaid = birchCamo(helper, 1, 1);
        FakePlayer poor = replacer(helper);
        GimbalActions.apply(poor, InteractionHand.MAIN_HAND, Kit.hit(unpaid, Direction.UP));
        CamoPosition kept = Kit.camoAt(helper, unpaid);
        helper.assertTrue(kept != null && kept.state().is(Blocks.BIRCH_PLANKS), "the old camo changed: " + kept);
        helper.assertValueEqual(Kit.carried(poor, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 0, "birch camo returned");
        helper.assertValueEqual(Kit.dropped(helper), 0, "items dropped");

        BlockPos paid = birchCamo(helper, 5, 1);
        FakePlayer rich = replacer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        GimbalActions.apply(rich, InteractionHand.MAIN_HAND, Kit.hit(paid, Direction.UP));
        helper.assertTrue(isOak(helper, paid), "control: the paid block was not replaced");
        helper.succeed();
    }

    /** The same block is no exchange: nothing is spent and nothing returns, though the type changes. */
    @GameTest(template = Kit.EMPTY)
    public static void sameBlockSpendsNothing(GameTestHelper helper) {
        BlockPos pos = Kit.abs(helper, 2, 1, 2);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        Kit.camo(helper, pos, Blocks.OAK_PLANKS.defaultBlockState(), SavedCopy.SOLID);
        FakePlayer player = replacer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        CamoPosition after = Kit.camoAt(helper, pos);
        helper.assertTrue(after != null && after.camoType().equals(SavedCopy.NORMAL), "the type did not change: " + after);
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 1, "oak camo spent");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.SOLID_CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "a camo returned");
        helper.assertValueEqual(Kit.dropped(helper), 0, "items dropped");
        helper.succeed();
    }
}
