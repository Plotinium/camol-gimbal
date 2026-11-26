package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.server.GimbalActions;

/** The switches of D-19 that change what apply writes (plan task 6): type, states, direction. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SwitchTests {
    private static final BlockState LOG_X = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    private static final BlockState LOG_Y = Blocks.OAK_LOG.defaultBlockState();

    private SwitchTests() {
    }

    private static GimbalSettings with(boolean pasteType, Set<String> skipped, DirectionMode direction) {
        return Kit.settings(pasteType, skipped, direction, SeriesShape.CONNECTED, 64, false);
    }

    private static BlockPos stone(GameTestHelper helper, int x, int z) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        return pos;
    }

    private static CamoPosition applyWith(GameTestHelper helper, FakePlayer player, SavedCopy copy,
            GimbalSettings settings, BlockPos pos, Direction face) {
        Kit.hold(player, Kit.gimbal(copy, settings));
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, face));
        return Kit.camoAt(helper, pos);
    }

    /** On an existing camo the type follows the copy, for free; with the switch off the camo keeps its type. */
    @GameTest(template = Kit.EMPTY)
    public static void pasteTypeSwitch(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        SavedCopy solidLog = Kit.copy(SavedCopy.SOLID, LOG_X, Direction.NORTH);
        BlockPos on = stone(helper, 1, 1);
        BlockPos off = stone(helper, 5, 1);
        Kit.camo(helper, on, LOG_Y, SavedCopy.NORMAL);
        Kit.camo(helper, off, LOG_Y, SavedCopy.NORMAL);

        CamoPosition typed = applyWith(helper, player, solidLog, with(true, Set.of(), DirectionMode.AS_COPIED), on, Direction.UP);
        helper.assertTrue(typed.camoType().equals(SavedCopy.SOLID) && typed.state().equals(LOG_X), "switch on: " + typed);
        CamoPosition kept = applyWith(helper, player, solidLog, with(false, Set.of(), DirectionMode.AS_COPIED), off, Direction.UP);
        helper.assertTrue(kept.camoType().equals(SavedCopy.NORMAL) && kept.state().equals(LOG_X), "switch off: " + kept);
        helper.assertValueEqual(Kit.dropped(helper), 0, "items dropped");
        helper.succeed();
    }

    /**
     * An unticked state is not pasted: an existing camo keeps its value, a new one takes the value of placing on the
     * clicked face. Only the states the two blocks share are pasted.
     */
    @GameTest(template = Kit.EMPTY)
    public static void skippedStatesAndSharedStates(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        BlockState topStairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.FACING, Direction.EAST);
        SavedCopy copy = Kit.copy(topStairs);

        BlockPos ticked = stone(helper, 1, 1);
        CamoPosition all = applyWith(helper, player, copy, with(true, Set.of(), DirectionMode.AS_COPIED), ticked, Direction.UP);
        helper.assertTrue(all.state().getValue(StairBlock.HALF) == Half.TOP, "control: half not pasted " + all);

        BlockPos unticked = stone(helper, 3, 1);
        CamoPosition placed = applyWith(helper, player, copy, with(true, Set.of("half"), DirectionMode.AS_COPIED), unticked, Direction.UP);
        helper.assertTrue(placed.state().getValue(StairBlock.HALF) == Half.BOTTOM
                && placed.state().getValue(StairBlock.FACING) == Direction.EAST, "new camo, half unticked: " + placed);

        BlockPos existing = stone(helper, 5, 1);
        Kit.camo(helper, existing, LOG_Y, SavedCopy.NORMAL);
        CamoPosition log = applyWith(helper, player, Kit.copy(LOG_X), with(true, Set.of("axis"), DirectionMode.AS_COPIED), existing, Direction.UP);
        helper.assertTrue(log.state().equals(LOG_Y), "existing camo, axis unticked: " + log);

        BlockPos shared = stone(helper, 5, 5);
        Kit.camo(helper, shared, Blocks.QUARTZ_PILLAR.defaultBlockState(), SavedCopy.NORMAL);
        CamoPosition pillar = applyWith(helper, player, Kit.copy(LOG_X), with(true, Set.of(), DirectionMode.AS_COPIED), shared, Direction.UP);
        helper.assertTrue(pillar.state().is(Blocks.QUARTZ_PILLAR)
                && pillar.state().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "shared axis: " + pillar);
        BlockPos notShared = stone(helper, 1, 5);
        Kit.camo(helper, notShared, Blocks.FURNACE.defaultBlockState(), SavedCopy.NORMAL);
        CamoPosition furnace = applyWith(helper, player, Kit.copy(LOG_X), with(true, Set.of(), DirectionMode.AS_COPIED), notShared, Direction.UP);
        helper.assertTrue(furnace.state().equals(Blocks.FURNACE.defaultBlockState()), "a furnace took a log state: " + furnace);
        helper.succeed();
    }

    /** As copied, following the look (turned by the turn since copying), following the clicked face. */
    @GameTest(template = Kit.EMPTY)
    public static void directionModes(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        player.setYRot(-90);
        helper.assertTrue(player.getDirection() == Direction.EAST, "the player does not face east");
        SavedCopy copiedFacingNorth = Kit.copy(SavedCopy.NORMAL, LOG_X, Direction.NORTH);

        CamoPosition asCopied = applyWith(helper, player, copiedFacingNorth, with(true, Set.of(), DirectionMode.AS_COPIED),
                stone(helper, 1, 1), Direction.UP);
        helper.assertTrue(asCopied.state().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "as copied: " + asCopied);

        CamoPosition look = applyWith(helper, player, copiedFacingNorth, with(true, Set.of(), DirectionMode.FOLLOW_LOOK),
                stone(helper, 3, 1), Direction.UP);
        helper.assertTrue(look.state().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Z, "follows look: " + look);

        CamoPosition face = applyWith(helper, player, copiedFacingNorth, with(true, Set.of(), DirectionMode.FOLLOW_FACE),
                stone(helper, 5, 1), Direction.UP);
        helper.assertTrue(face.state().getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y, "follows face: " + face);

        BlockState northStairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        CamoPosition turned = applyWith(helper, player, Kit.copy(SavedCopy.NORMAL, northStairs, Direction.NORTH),
                with(true, Set.of(), DirectionMode.FOLLOW_LOOK), stone(helper, 1, 5), Direction.UP);
        helper.assertTrue(turned.state().getValue(StairBlock.FACING) == Direction.EAST, "stairs follow look: " + turned);
        helper.succeed();
    }
}
