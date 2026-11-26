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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.server.GimbalActions;

/** What the server refuses before it touches a camo: reach, unloaded chunks, no Gimbal, adventure, a claim. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuardTests {
    private static final BlockState OAK = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState LYING_LOG = LOG.setValue(BlockStateProperties.AXIS, Direction.Axis.X);

    private GuardTests() {
    }

    private static BlockPos camo(GameTestHelper helper, int x, int z, BlockState camo) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        Kit.camo(helper, pos, camo, SavedCopy.NORMAL);
        return pos;
    }

    private static void standNear(FakePlayer player, BlockPos pos) {
        Vec3 at = Vec3.atBottomCenterOf(pos.above());
        player.moveTo(at.x, at.y, at.z, 180, 0);
    }

    /** Out of reach, or in a chunk that is not loaded: copy, remove and editCamo change nothing and load nothing. */
    @GameTest(template = Kit.EMPTY)
    public static void reachAndUnloadedAreRefused(GameTestHelper helper) {
        BlockPos pos = camo(helper, 2, 2, LOG);
        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = new ItemStack(ModRegistry.GIMBAL.get());
        Kit.hold(player, gimbal);

        standNear(player, pos.offset(30, 0, 0));
        helper.assertFalse(GimbalActions.copy(player, pos, Direction.UP), "copied out of reach");
        helper.assertFalse(GimbalActions.editCamo(player, pos, LYING_LOG), "edited out of reach");
        helper.assertFalse(GimbalActions.remove(player, pos, Direction.UP), "removed out of reach");
        helper.assertTrue(Kit.camoAt(helper, pos).state().equals(LOG) && gimbal.get(ModRegistry.COPY) == null, "out of reach changed something");

        BlockPos far = pos.offset(0, 0, 30000);
        standNear(player, far);
        helper.assertFalse(GimbalActions.copy(player, far, Direction.UP), "copied in an unloaded chunk");
        helper.assertFalse(GimbalActions.editCamo(player, far, LYING_LOG), "edited in an unloaded chunk");
        helper.assertFalse(GimbalActions.remove(player, far, Direction.UP), "removed in an unloaded chunk");
        helper.assertFalse(helper.getLevel().isLoaded(far), "an action loaded the chunk");

        standNear(player, pos.offset(1, 0, 0));
        helper.assertTrue(GimbalActions.editCamo(player, pos, LYING_LOG), "control: edit refused within reach");
        helper.assertTrue(GimbalActions.copy(player, pos, Direction.UP), "control: copy refused within reach");
        helper.assertTrue(GimbalActions.remove(player, pos, Direction.UP), "control: remove refused within reach");
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void editCamoUnderADeniedPosition(GameTestHelper helper) {
        BlockPos denied = camo(helper, 1, 1, LOG);
        BlockPos allowed = camo(helper, 5, 1, LOG);
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, new ItemStack(ModRegistry.GIMBAL.get()));
        Deny.add(denied);
        try {
            helper.assertFalse(GimbalActions.editCamo(player, denied, LYING_LOG), "edited a denied camo");
            helper.assertTrue(Kit.camoAt(helper, denied).state().equals(LOG), "a denied camo changed");
            helper.assertTrue(GimbalActions.editCamo(player, allowed, LYING_LOG), "control: edit refused");
        } finally {
            Deny.clear(denied);
        }
        helper.succeed();
    }

    /** The menu packets need a Gimbal in a hand; one elsewhere in the inventory is not touched. */
    @GameTest(template = Kit.EMPTY)
    public static void menuPacketsNeedAGimbalInHand(GameTestHelper helper) {
        BlockPos pos = camo(helper, 2, 2, LOG);
        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = Kit.gimbal(Kit.copy(LOG));
        player.getInventory().setItem(5, gimbal);
        Kit.hold(player, new ItemStack(Items.STONE));
        helper.assertFalse(GimbalActions.editCamo(player, pos, LYING_LOG), "edit camo without a Gimbal in hand");
        helper.assertFalse(GimbalActions.editCopy(player, LYING_LOG), "edit copy without a Gimbal in hand");
        helper.assertFalse(GimbalActions.changeSettings(player, GimbalSettings.DEFAULT), "settings without a Gimbal in hand");
        helper.assertFalse(GimbalActions.clearCopy(player), "clear without a Gimbal in hand");
        helper.assertTrue(Kit.camoAt(helper, pos).state().equals(LOG), "the camo changed");
        helper.assertTrue(gimbal.get(ModRegistry.COPY) != null && gimbal.get(ModRegistry.COPY).state().equals(LOG), "the Gimbal in the inventory changed");

        player.getInventory().setItem(5, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, gimbal);
        helper.assertTrue(GimbalActions.editCopy(player, LYING_LOG), "control: edit copy refused from the off hand");
        helper.succeed();
    }

    /** Adventure mode may not change blocks: apply, copy and remove are refused; in survival they work. */
    @GameTest(template = Kit.EMPTY)
    public static void adventureModeIsRefused(GameTestHelper helper) {
        BlockPos copyPos = camo(helper, 1, 1, LOG);
        BlockPos removePos = camo(helper, 3, 1, LOG);
        BlockPos applyPos = Kit.abs(helper, 5, 1, 1);
        helper.getLevel().setBlockAndUpdate(applyPos, Blocks.STONE.defaultBlockState());
        FakePlayer adventurer = Kit.player(helper, GameType.ADVENTURE);
        ItemStack gimbal = Kit.gimbal(Kit.copy(OAK));
        Kit.hold(adventurer, gimbal);
        Kit.give(adventurer, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        GimbalActions.apply(adventurer, InteractionHand.MAIN_HAND, Kit.hit(applyPos, Direction.UP));
        helper.assertTrue(Kit.camoAt(helper, applyPos) == null, "applied in adventure mode");
        helper.assertValueEqual(Kit.carried(adventurer, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 1, "spent in adventure mode");
        helper.assertFalse(GimbalActions.copy(adventurer, copyPos, Direction.UP), "copied in adventure mode");
        helper.assertTrue(gimbal.get(ModRegistry.COPY).state().equals(OAK), "the copy changed in adventure mode");
        helper.assertFalse(GimbalActions.remove(adventurer, removePos, Direction.UP), "removed in adventure mode");
        helper.assertTrue(Kit.camoAt(helper, removePos) != null, "a camo was removed in adventure mode");

        FakePlayer survivor = Kit.survival(helper);
        Kit.hold(survivor, Kit.gimbal(Kit.copy(OAK)));
        Kit.give(survivor, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        GimbalActions.apply(survivor, InteractionHand.MAIN_HAND, Kit.hit(applyPos, Direction.UP));
        helper.assertTrue(Kit.camoAt(helper, applyPos) != null, "control: not applied in survival");
        helper.assertTrue(GimbalActions.remove(survivor, removePos, Direction.UP), "control: not removed in survival");
        helper.succeed();
    }

    /** The menu never changes {@code waterlogged}; the rest of the edit goes through. */
    @GameTest(template = Kit.EMPTY)
    public static void editCamoKeepsWaterlogged(GameTestHelper helper) {
        BlockState wet = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.WATERLOGGED, true)
                .setValue(StairBlock.FACING, Direction.NORTH);
        BlockPos pos = camo(helper, 2, 2, wet);
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, new ItemStack(ModRegistry.GIMBAL.get()));
        BlockState asked = wet.setValue(StairBlock.WATERLOGGED, false).setValue(StairBlock.FACING, Direction.EAST);
        helper.assertTrue(GimbalActions.editCamo(player, pos, asked), "edit refused");
        CamoPosition edited = Kit.camoAt(helper, pos);
        helper.assertTrue(edited.state().getValue(StairBlock.FACING) == Direction.EAST, "control: facing not edited " + edited);
        helper.assertTrue(edited.state().getValue(StairBlock.WATERLOGGED), "waterlogged changed " + edited);
        helper.succeed();
    }
}
