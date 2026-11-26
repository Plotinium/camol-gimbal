package plotinium.camolgimbal.gametest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.rules.VisibleStates;

/**
 * Which states are offered (D-1), with made-up looks: how the client compares models is only seen in game.
 */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VisibleStatesTests {
    private VisibleStatesTests() {
    }

    @GameTest(template = Kit.EMPTY)
    public static void onlyStatesThatChangeTheLookAreOffered(GameTestHelper helper) {
        // Every state looks different: all are offered but waterlogged.
        expect(helper, Blocks.OAK_STAIRS, state -> state,
                Set.of(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF,
                        BlockStateProperties.STAIRS_SHAPE));
        expect(helper, Blocks.OAK_STAIRS, state -> state.getValue(BlockStateProperties.HORIZONTAL_FACING),
                Set.of(BlockStateProperties.HORIZONTAL_FACING));
        expect(helper, Blocks.OAK_STAIRS, state -> "same", Set.of());
        // A difference that shows only while another state has one of its values.
        expect(helper, Blocks.OAK_STAIRS,
                state -> state.getValue(BlockStateProperties.HALF) == Half.TOP
                        ? state.getValue(BlockStateProperties.STAIRS_SHAPE).getSerializedName() : "bottom",
                Set.of(BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE));
        helper.succeed();
    }

    /** A block with many states is sampled: fewer looks than it has states, and a plain difference is still found. */
    @GameTest(template = Kit.EMPTY)
    public static void manyStatesAreSampled(GameTestHelper helper) {
        int states = Blocks.REDSTONE_WIRE.getStateDefinition().getPossibleStates().size();
        Set<BlockState> looked = new HashSet<>();
        List<Property<?>> none = VisibleStates.of(Blocks.REDSTONE_WIRE, state -> {
            looked.add(state);
            return "same";
        }, 64);
        helper.assertTrue(none.isEmpty(), "states offered for a block that always looks the same: " + none);
        // At most 33 groups for each of four 3-value sides and the 16-value power.
        helper.assertTrue(looked.size() <= 4 * 33 * 3 + 33 * 16 && looked.size() < states,
                "looked at " + looked.size() + " of " + states + " states");

        List<Property<?>> power = VisibleStates.of(Blocks.REDSTONE_WIRE,
                state -> state.getValue(BlockStateProperties.POWER), 64);
        helper.assertTrue(power.equals(List.of(BlockStateProperties.POWER)), "power not found: " + power);
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, Block block, Function<BlockState, Object> look,
            Set<Property<?>> expected) {
        List<Property<?>> offered = VisibleStates.of(block, look);
        helper.assertTrue(Set.copyOf(offered).equals(expected), "offered " + offered + ", expected " + expected);
    }
}
