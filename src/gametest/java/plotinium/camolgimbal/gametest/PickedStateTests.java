package plotinium.camolgimbal.gametest;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.rules.PickedState;

@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickedStateTests {
    private PickedStateTests() {
    }

    /** D-18: the menu reopens on the picked state, also on another block that has it, else on the first. */
    @GameTest(template = "empty8")
    public static void menuKeepsThePickedState(GameTestHelper helper) {
        List<Property<?>> trapdoor = List.of(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF,
                BlockStateProperties.OPEN);
        List<Property<?>> door = List.of(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.OPEN);
        List<Property<?>> log = List.of(BlockStateProperties.AXIS);

        helper.assertValueEqual(PickedState.indexIn(trapdoor, "open"), 2, "trapdoor");
        helper.assertValueEqual(PickedState.indexIn(door, "open"), 1, "a block with the state at another place");
        helper.assertValueEqual(PickedState.indexIn(log, "open"), 0, "a block without the state");
        helper.assertValueEqual(PickedState.indexIn(trapdoor, null), 0, "nothing picked yet");
        helper.assertValueEqual(PickedState.indexIn(List.of(), "open"), 0, "a block with no state");
        helper.succeed();
    }
}
