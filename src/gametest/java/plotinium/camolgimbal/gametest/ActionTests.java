package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.server.GimbalActions;
import plotinium.camolgimbal.server.Permissions;

/** Copy, remove and apply on one block, with and without permission. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ActionTests {
    private ActionTests() {
    }

    private static BlockPos stone(GameTestHelper helper, int x, int z) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        return pos;
    }

    /** Copying needs a placed camouflage: a plain block copies nothing. */
    @GameTest(template = Kit.EMPTY)
    public static void copyNeedsACamo(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = new ItemStack(ModRegistry.GIMBAL.get());
        Kit.hold(player, gimbal);
        BlockPos plain = stone(helper, 1, 1);
        helper.assertFalse(GimbalActions.copy(player, plain, Direction.UP), "copied a block without a camo");
        helper.assertTrue(gimbal.get(ModRegistry.COPY) == null, "a copy was saved from a plain block");

        BlockPos camo = stone(helper, 5, 1);
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        Kit.camo(helper, camo, log, SavedCopy.SOLID);
        helper.assertTrue(GimbalActions.copy(player, camo, Direction.UP), "control: the camo was not copied");
        SavedCopy copy = gimbal.get(ModRegistry.COPY);
        helper.assertTrue(copy != null && copy.state().equals(log) && copy.type().equals(SavedCopy.SOLID)
                && copy.look() == Direction.NORTH, "wrong copy: " + copy);
        helper.succeed();
    }

    /** A denied position is refused by apply, copy and remove; the same actions work next to it. */
    @GameTest(template = Kit.EMPTY)
    public static void deniedPermissionRefusesEveryAction(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState()));
        Kit.hold(player, gimbal);
        Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 4));

        BlockPos applyDenied = stone(helper, 1, 1);
        BlockPos applyAllowed = stone(helper, 1, 5);
        BlockPos removeDenied = stone(helper, 3, 1);
        BlockPos removeAllowed = stone(helper, 3, 5);
        BlockPos copyDenied = stone(helper, 5, 1);
        BlockPos copyAllowed = stone(helper, 5, 5);
        BlockState birch = Blocks.BIRCH_PLANKS.defaultBlockState();
        Kit.camo(helper, removeDenied, birch, SavedCopy.NORMAL);
        Kit.camo(helper, removeAllowed, birch, SavedCopy.NORMAL);
        Kit.camo(helper, copyDenied, Blocks.SPRUCE_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        Kit.camo(helper, copyAllowed, Blocks.JUNGLE_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        Deny.add(applyDenied);
        Deny.add(removeDenied);
        Deny.add(copyDenied);
        try {
            GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(applyDenied, Direction.UP));
            helper.assertTrue(Kit.camoAt(helper, applyDenied) == null, "applied on a denied block");
            helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 4, "paid for a denied block");
            GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(applyAllowed, Direction.UP));
            helper.assertTrue(Kit.camoAt(helper, applyAllowed) != null, "control: apply refused");

            helper.assertFalse(GimbalActions.remove(player, removeDenied, Direction.UP), "removed a denied camo");
            helper.assertTrue(Kit.camoAt(helper, removeDenied) != null, "a denied camo is gone");
            helper.assertTrue(GimbalActions.remove(player, removeAllowed, Direction.UP), "control: remove refused");
            helper.assertTrue(Kit.camoAt(helper, removeAllowed) == null, "control: camo still there");
            helper.assertValueEqual(Kit.dropped(helper, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 1, "birch camos dropped");

            helper.assertFalse(GimbalActions.copy(player, copyDenied, Direction.UP), "copied a denied camo");
            helper.assertTrue(gimbal.get(ModRegistry.COPY).state().is(Blocks.OAK_PLANKS), "the copy changed");
            helper.assertTrue(GimbalActions.copy(player, copyAllowed, Direction.UP), "control: copy refused");
            helper.assertTrue(gimbal.get(ModRegistry.COPY).state().is(Blocks.JUNGLE_PLANKS), "control: copy not saved");
        } finally {
            Deny.clear(applyDenied);
            Deny.clear(removeDenied);
            Deny.clear(copyDenied);
        }
        helper.succeed();
    }

    /** Removing drops the camo item in survival and nothing in creative. */
    @GameTest(template = Kit.EMPTY)
    public static void removeDropsOnlyInSurvival(GameTestHelper helper) {
        BlockPos creativePos = stone(helper, 1, 1);
        Kit.camo(helper, creativePos, Blocks.OAK_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        FakePlayer creative = Kit.creative(helper);
        Kit.hold(creative, new ItemStack(ModRegistry.GIMBAL.get()));
        helper.assertTrue(GimbalActions.remove(creative, creativePos, Direction.UP), "creative remove refused");
        helper.assertTrue(Kit.camoAt(helper, creativePos) == null, "creative remove left the camo");
        helper.assertValueEqual(Kit.dropped(helper), 0, "creative remove dropped items");

        BlockPos survivalPos = stone(helper, 5, 1);
        Kit.camo(helper, survivalPos, Blocks.BIRCH_PLANKS.defaultBlockState(), SavedCopy.SOLID);
        FakePlayer survival = Kit.survival(helper);
        Kit.hold(survival, new ItemStack(ModRegistry.GIMBAL.get()));
        helper.assertTrue(GimbalActions.remove(survival, survivalPos, Direction.UP), "survival remove refused");
        helper.assertValueEqual(Kit.dropped(helper, Kit.isFilled(Camol.SOLID_CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 1, "survival drops");
        helper.assertValueEqual(Kit.dropped(helper), 1, "items dropped in all");
        helper.succeed();
    }

    /** Applying wins over the block's own right click: the lever takes the camo and does not switch. */
    @GameTest(template = Kit.EMPTY)
    public static void applyReplacesTheBlocksOwnAction(GameTestHelper helper) {
        BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR);
        BlockPos withGimbal = Kit.abs(helper, 2, 1, 2);
        BlockPos withHand = Kit.abs(helper, 6, 1, 2);
        helper.getLevel().setBlockAndUpdate(withGimbal.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(withHand.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(withGimbal, lever);
        helper.getLevel().setBlockAndUpdate(withHand, lever);

        FakePlayer player = Kit.creative(helper);
        ItemStack gimbal = Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState()));
        Kit.hold(player, gimbal);
        player.gameMode.useItemOn(player, helper.getLevel(), gimbal, InteractionHand.MAIN_HAND,
                new BlockHitResult(withGimbal.getCenter(), Direction.UP, withGimbal, false));
        helper.assertFalse(helper.getLevel().getBlockState(withGimbal).getValue(LeverBlock.POWERED), "the lever switched");
        helper.assertTrue(Kit.camoAt(helper, withGimbal) != null, "the lever took no camo");

        Kit.hold(player, ItemStack.EMPTY);
        player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(withHand.getCenter(), Direction.UP, withHand, false));
        helper.assertTrue(helper.getLevel().getBlockState(withHand).getValue(LeverBlock.POWERED), "control: the lever did not switch by hand");
        helper.succeed();
    }

    /**
     * The permission check leaves a camouflage in place and drops nothing. The control posts the break event Camol
     * listens to on another camouflaged block: that one drops its camo item and is cleared, so the test can see a drop.
     */
    @GameTest(template = Kit.EMPTY)
    public static void permissionCheckLeavesTheCamo(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = new ItemStack(ModRegistry.GIMBAL.get());
        Kit.hold(player, gimbal);
        BlockPos checked = stone(helper, 1, 1);
        BlockPos broken = stone(helper, 5, 1);
        BlockState oak = Blocks.OAK_PLANKS.defaultBlockState();
        Kit.camo(helper, checked, oak, SavedCopy.NORMAL);
        Kit.camo(helper, broken, oak, SavedCopy.NORMAL);

        helper.assertTrue(Permissions.canEdit(player, checked, Direction.UP, gimbal), "the check refused");
        CamoPosition after = Kit.camoAt(helper, checked);
        helper.assertTrue(after != null && after.state().equals(oak), "the check changed the camo");
        helper.assertValueEqual(Kit.dropped(helper), 0, "the check dropped items");

        NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), broken, helper.getLevel().getBlockState(broken), player));
        helper.assertTrue(Kit.camoAt(helper, broken) == null, "control: Camol kept the camo on a break event");
        helper.assertValueEqual(Kit.dropped(helper), 1, "control: items dropped by the break event");
        helper.succeed();
    }
}
