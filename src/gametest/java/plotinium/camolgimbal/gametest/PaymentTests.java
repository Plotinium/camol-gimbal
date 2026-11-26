package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
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

/** D-17: what a new camouflage costs, and the two blacklists. Every refusal sits next to a success elsewhere. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PaymentTests {
    private PaymentTests() {
    }

    /** Applies the copy on a stone at (x, 1, z) and tells whether a camo of oak planks is there now. */
    private static boolean applyOakOn(GameTestHelper helper, FakePlayer player, int x, int z) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        CamoPosition camo = Kit.camoAt(helper, pos);
        return camo != null && camo.state().is(Blocks.OAK_PLANKS);
    }

    private static FakePlayer payer(GameTestHelper helper, ItemStack... stacks) {
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState())));
        Kit.give(player, stacks);
        return player;
    }

    @GameTest(template = Kit.EMPTY)
    public static void filledCamoOfOwnTypeIsSpentFirst(GameTestHelper helper) {
        FakePlayer player = payer(helper, Kit.filledSolid(Blocks.OAK_PLANKS, 1), Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, player, 2, 2), "no camo placed");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "filled normal left");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.SOLID_CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 1, "filled solid left");
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void filledCamoOfOtherTypeCounts(GameTestHelper helper) {
        FakePlayer player = payer(helper, Kit.filledSolid(Blocks.OAK_PLANKS, 1), new ItemStack(Items.OAK_PLANKS, 1),
                Kit.emptyNormal(1));
        helper.assertTrue(applyOakOn(helper, player, 2, 2), "no camo placed");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.SOLID_CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "filled solid left");
        helper.assertValueEqual(Kit.carried(player, stack -> stack.is(Items.OAK_PLANKS)), 1, "planks left");
        helper.assertValueEqual(Kit.carried(player, Kit.isEmptyCamo(Camol.CAMO_ITEM.get())), 1, "empty camo left");
        helper.assertValueEqual(Kit.camoAt(helper, Kit.abs(helper, 2, 1, 2)).camoType(), SavedCopy.NORMAL, "camo type");
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void emptyCamoOfOwnTypePlusBlockIsSpent(GameTestHelper helper) {
        FakePlayer player = payer(helper, Kit.emptySolid(1), Kit.emptyNormal(1), new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertTrue(applyOakOn(helper, player, 2, 2), "no camo placed");
        helper.assertValueEqual(Kit.carried(player, Kit.isEmptyCamo(Camol.CAMO_ITEM.get())), 0, "empty normal left");
        helper.assertValueEqual(Kit.carried(player, Kit.isEmptyCamo(Camol.SOLID_CAMO_ITEM.get())), 1, "empty solid left");
        helper.assertValueEqual(Kit.carried(player, stack -> stack.is(Items.OAK_PLANKS)), 1, "planks left");
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void emptyCamoOfOtherTypePlusBlockCounts(GameTestHelper helper) {
        FakePlayer player = payer(helper, Kit.emptySolid(1), new ItemStack(Items.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, player, 2, 2), "no camo placed");
        helper.assertValueEqual(Kit.carried(player, Kit.isEmptyCamo(Camol.SOLID_CAMO_ITEM.get())), 0, "empty solid left");
        helper.assertValueEqual(Kit.carried(player, stack -> stack.is(Items.OAK_PLANKS)), 0, "planks left");
        helper.succeed();
    }

    /** An empty camo item without the block, or the block without a camo item, pays for nothing. */
    @GameTest(template = Kit.EMPTY)
    public static void missingItemsChangeNothing(GameTestHelper helper) {
        FakePlayer onlyCamo = payer(helper, Kit.emptyNormal(1));
        helper.assertFalse(applyOakOn(helper, onlyCamo, 1, 1), "placed with an empty camo item alone");
        helper.assertValueEqual(Kit.carried(onlyCamo, Kit.isEmptyCamo(Camol.CAMO_ITEM.get())), 1, "empty camo spent");

        FakePlayer onlyBlock = payer(helper, new ItemStack(Items.OAK_PLANKS, 1));
        helper.assertFalse(applyOakOn(helper, onlyBlock, 3, 1), "placed with planks alone");
        helper.assertValueEqual(Kit.carried(onlyBlock, stack -> stack.is(Items.OAK_PLANKS)), 1, "planks spent");

        FakePlayer both = payer(helper, Kit.emptyNormal(1), new ItemStack(Items.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, both, 5, 1), "control: not placed with both items");
        helper.succeed();
    }

    /** In creative nothing is spent, even when the items are there. */
    @GameTest(template = Kit.EMPTY)
    public static void creativeSpendsNothing(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState())));
        Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, player, 2, 2), "no camo placed in creative");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 1, "spent in creative");

        FakePlayer survival = payer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, survival, 5, 2), "control: no camo placed in survival");
        helper.assertValueEqual(Kit.carried(survival, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "control: not spent in survival");
        helper.succeed();
    }

    /** {@code camol:disallowed_camo_blocks} (doors) stops the ingredient path; a filled camo item still pays. */
    @GameTest(template = Kit.EMPTY)
    public static void disallowedIngredientIsNotSpent(GameTestHelper helper) {
        var door = Blocks.OAK_DOOR.defaultBlockState();
        FakePlayer ingredient = Kit.survival(helper);
        Kit.hold(ingredient, Kit.gimbal(Kit.copy(door)));
        Kit.give(ingredient, Kit.emptyNormal(1), new ItemStack(Items.OAK_DOOR, 1));
        BlockPos refused = Kit.abs(helper, 1, 1, 1);
        helper.getLevel().setBlockAndUpdate(refused, Blocks.STONE.defaultBlockState());
        GimbalActions.apply(ingredient, InteractionHand.MAIN_HAND, Kit.hit(refused, Direction.UP));
        helper.assertTrue(Kit.camoAt(helper, refused) == null, "a door paid as an ingredient");
        helper.assertValueEqual(Kit.carried(ingredient, stack -> stack.is(Items.OAK_DOOR)), 1, "door spent");

        FakePlayer filled = Kit.survival(helper);
        Kit.hold(filled, Kit.gimbal(Kit.copy(door)));
        Kit.give(filled, Kit.filledNormal(Blocks.OAK_DOOR, 1));
        BlockPos placed = Kit.abs(helper, 5, 1, 1);
        helper.getLevel().setBlockAndUpdate(placed, Blocks.STONE.defaultBlockState());
        GimbalActions.apply(filled, InteractionHand.MAIN_HAND, Kit.hit(placed, Direction.UP));
        helper.assertTrue(Kit.camoAt(helper, placed) != null, "control: a filled door camo did not pay");
        helper.succeed();
    }

    /** A block item with components of its own (here a name) does not pay. */
    @GameTest(template = Kit.EMPTY)
    public static void blockItemWithComponentsIsNotSpent(GameTestHelper helper) {
        ItemStack named = new ItemStack(Items.OAK_PLANKS, 1);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("kept"));
        FakePlayer player = payer(helper, Kit.emptyNormal(1), named);
        helper.assertFalse(applyOakOn(helper, player, 1, 1), "a named block item paid");
        helper.assertValueEqual(Kit.carried(player, stack -> stack.is(Items.OAK_PLANKS)), 1, "named planks spent");

        FakePlayer plain = payer(helper, Kit.emptyNormal(1), new ItemStack(Items.OAK_PLANKS, 1));
        helper.assertTrue(applyOakOn(helper, plain, 5, 1), "control: plain planks did not pay");
        helper.succeed();
    }

    /** {@code camol:disallowed_camoable_blocks} (sponge, in the test data) takes no camouflage. */
    @GameTest(template = Kit.EMPTY)
    public static void disallowedTargetTakesNoCamo(GameTestHelper helper) {
        FakePlayer player = payer(helper, Kit.filledNormal(Blocks.OAK_PLANKS, 2));
        BlockPos sponge = Kit.abs(helper, 1, 1, 1);
        helper.getLevel().setBlockAndUpdate(sponge, Blocks.SPONGE.defaultBlockState());
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(sponge, Direction.UP));
        helper.assertTrue(Kit.camoAt(helper, sponge) == null, "a sponge took a camo");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 2, "spent on a sponge");

        helper.assertTrue(applyOakOn(helper, player, 5, 1), "control: stone took no camo");
        helper.succeed();
    }
}
