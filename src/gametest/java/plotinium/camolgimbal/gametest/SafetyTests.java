package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.rules.CamoView;
import plotinium.camolgimbal.rules.Planner;
import plotinium.camolgimbal.rules.Selection;
import plotinium.camolgimbal.server.GimbalActions;
import plotinium.camolgimbal.server.Payment;
import plotinium.camolgimbal.server.SeriesKeys;

/** The preview's count of what can be paid, block drops turned off, the single write of a series, loaded areas. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SafetyTests {
    private static final GimbalSettings ROW = Kit.series(SeriesShape.ROW, 64);
    private static final GimbalSettings REPLACE_ROW =
            Kit.settings(true, Set.of(), DirectionMode.AS_COPIED, SeriesShape.ROW, 64, true);

    private SafetyTests() {
    }

    /** A row of five at (1..5, 1, z): stone, or stone with a birch camo. */
    private static void row(GameTestHelper helper, int z, boolean birchCamo) {
        for (int x = 1; x <= 5; x++) {
            BlockPos pos = Kit.abs(helper, x, 1, z);
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            if (birchCamo) {
                Kit.camo(helper, pos, Blocks.BIRCH_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
            }
        }
    }

    private static int camosOf(GameTestHelper helper, int z, Block block) {
        int count = 0;
        for (int x = 1; x <= 5; x++) {
            CamoPosition camo = Kit.camoAt(helper, Kit.abs(helper, x, 1, z));
            count += camo != null && camo.state().is(block) ? 1 : 0;
        }
        return count;
    }

    private static FakePlayer seriesPlayer(GameTestHelper helper, boolean creative, GimbalSettings settings) {
        FakePlayer player = creative ? Kit.creative(helper) : Kit.survival(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(Blocks.OAK_PLANKS.defaultBlockState()), settings));
        SeriesKeys.set(player, true);
        return player;
    }

    private static List<Planner.Change> plan(GameTestHelper helper, FakePlayer player, Planner.Hit hit) {
        ItemStack gimbal = player.getMainHandItem();
        return Planner.plan(helper.getLevel(), CamoView.of(helper.getLevel()), player, gimbal.get(ModRegistry.COPY),
                gimbal.get(ModRegistry.SETTINGS), hit, player.getDirection(), true);
    }

    /** Planned 5, items for 2: the preview's count is 2 and the click writes 2. Creative pays for all. */
    @GameTest(template = Kit.EMPTY)
    public static void previewCountsWhatCanBePaid(GameTestHelper helper) {
        row(helper, 1, false);
        FakePlayer player = seriesPlayer(helper, false, ROW);
        Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 2));
        Planner.Hit hit = Kit.hit(Kit.abs(helper, 3, 1, 1), Direction.UP);
        List<Planner.Change> planned = plan(helper, player, hit);
        helper.assertValueEqual(planned.size(), 5, "planned");
        helper.assertValueEqual(Payment.affordable(player, planned), 2, "preview count");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 2, "items spent by counting");
        helper.assertValueEqual(GimbalActions.apply(player, InteractionHand.MAIN_HAND, hit).written(), 2, "written");

        row(helper, 4, true);
        FakePlayer replacer = seriesPlayer(helper, false, REPLACE_ROW);
        Kit.give(replacer, Kit.filledNormal(Blocks.OAK_PLANKS, 1));
        Planner.Hit replaceHit = Kit.hit(Kit.abs(helper, 3, 1, 4), Direction.UP);
        List<Planner.Change> replaces = plan(helper, replacer, replaceHit);
        helper.assertValueEqual(replaces.size(), 5, "planned replaces");
        helper.assertValueEqual(Payment.affordable(replacer, replaces), 1, "preview count of replaces");
        helper.assertValueEqual(GimbalActions.apply(replacer, InteractionHand.MAIN_HAND, replaceHit).written(), 1, "replaces written");

        FakePlayer creative = seriesPlayer(helper, true, ROW);
        helper.assertValueEqual(Payment.affordable(creative, planned), 5, "control: creative preview count");
        helper.succeed();
    }

    /** Block drops off: a removed camo goes to the inventory. On: it drops, as before. */
    @GameTest(template = Kit.EMPTY)
    public static void removeWithoutBlockDropsKeepsTheItem(GameTestHelper helper) {
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        BlockPos off = Kit.abs(helper, 1, 1, 1);
        BlockPos on = Kit.abs(helper, 5, 1, 1);
        for (BlockPos pos : List.of(off, on)) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }
        Kit.camo(helper, off, Blocks.OAK_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        Kit.camo(helper, on, Blocks.BIRCH_PLANKS.defaultBlockState(), SavedCopy.NORMAL);
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, new ItemStack(ModRegistry.GIMBAL.get()));

        rule.set(false, helper.getLevel().getServer());
        try {
            helper.assertTrue(GimbalActions.remove(player, off, Direction.UP), "remove refused without block drops");
        } finally {
            rule.set(true, helper.getLevel().getServer());
        }
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 1, "oak camo in the inventory");
        helper.assertValueEqual(Kit.dropped(helper, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 0, "oak camo dropped");

        helper.assertTrue(GimbalActions.remove(player, on, Direction.UP), "control: remove refused");
        helper.assertValueEqual(Kit.dropped(helper, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 1, "control: birch camo dropped");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 0, "control: birch camo in the inventory");
        helper.succeed();
    }

    /**
     * A listener that throws in the middle of a series: every change paid for so far is written, in one call, and
     * each replaced camo comes back once. The order is 3, 4, 2, 5, 1, and the listener fails at 5.
     */
    @GameTest(template = Kit.EMPTY)
    public static void failingSeriesWritesWhatWasPaid(GameTestHelper helper) {
        row(helper, 1, true);
        BlockPos failing = Kit.abs(helper, 5, 1, 1);
        FakePlayer player = seriesPlayer(helper, false, REPLACE_ROW);
        Kit.give(player, Kit.filledNormal(Blocks.OAK_PLANKS, 5));
        Deny.fail(failing);
        boolean thrown = false;
        try {
            GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(Kit.abs(helper, 3, 1, 1), Direction.UP));
        } catch (RuntimeException expected) {
            thrown = true;
        } finally {
            Deny.clear(failing);
        }
        helper.assertTrue(thrown, "the test listener did not throw");
        helper.assertValueEqual(camosOf(helper, 1, Blocks.OAK_PLANKS), 3, "oak camos written");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.OAK_PLANKS)), 2, "oak camos left");
        helper.assertValueEqual(Kit.carried(player, Kit.isFilled(Camol.CAMO_ITEM.get(), Blocks.BIRCH_PLANKS)), 3, "birch camos returned");
        helper.assertValueEqual(camosOf(helper, 1, Blocks.BIRCH_PLANKS), 2, "birch camos left in place");
        helper.assertValueEqual(Kit.dropped(helper), 0, "items dropped");
        helper.succeed();
    }

    /**
     * A position joins a series only in loaded surroundings, and asking never loads a chunk. The edge case: the last
     * loaded block before an unloaded chunk is loaded itself, yet refused.
     */
    @GameTest(template = Kit.EMPTY)
    public static void seriesStaysInLoadedChunks(GameTestHelper helper) {
        BlockPos start = Kit.abs(helper, 3, 1, 3);
        BlockPos firstUnloaded = null;
        for (int step = 1; step < 200 && firstUnloaded == null; step++) {
            BlockPos probe = start.offset(step * 16, 0, 0);
            if (!helper.getLevel().hasChunkAt(probe)) {
                firstUnloaded = new BlockPos(SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(probe.getX())),
                        probe.getY(), probe.getZ());
            }
        }
        helper.assertTrue(firstUnloaded != null, "no unloaded chunk found");
        BlockPos edge = firstUnloaded.west();
        helper.assertTrue(helper.getLevel().isLoaded(edge), "the edge block is not loaded");
        helper.assertFalse(Selection.neighbourhoodLoaded(helper.getLevel(), edge), "a block next to an unloaded chunk was accepted");
        helper.assertFalse(helper.getLevel().hasChunkAt(firstUnloaded), "asking loaded the chunk");
        helper.assertTrue(Selection.neighbourhoodLoaded(helper.getLevel(), start), "control: the test area was refused");
        helper.succeed();
    }
}
