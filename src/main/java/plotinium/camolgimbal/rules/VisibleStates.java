package plotinium.camolgimbal.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Which states of a block the Gimbal offers (D-1): those where two states differing only in that state can look
 * different. {@code waterlogged} is never offered, since the camo draws no water. What "looks different" means is
 * given by the caller (the client compares baked models, tints and shapes).
 *
 * <p>The work is bounded. For each state, the groups of block states that differ only in it are all compared when
 * there are few; a block with very many states is sampled: the default state's group and a fixed pseudo-random set
 * of groups. A difference that shows only outside the sample is missed.
 */
public final class VisibleStates {
    /** How many looks may be compared for one state before its groups are sampled. */
    public static final int LOOKS_PER_STATE = 2048;
    private static final int MIN_GROUPS = 32;
    private static final long SEED = 0x47696d62616cL;

    private VisibleStates() {
    }

    public static List<Property<?>> of(Block block, Function<BlockState, Object> look) {
        return of(block, look, LOOKS_PER_STATE);
    }

    /** @param looksPerState the bound of {@link #LOOKS_PER_STATE}; at least {@value #MIN_GROUPS} groups are compared */
    public static List<Property<?>> of(Block block, Function<BlockState, Object> look, int looksPerState) {
        Map<BlockState, Object> looks = new HashMap<>();
        Function<BlockState, Object> remembered = state -> looks.computeIfAbsent(state, look);
        List<BlockState> states = block.getStateDefinition().getPossibleStates();
        List<Property<?>> visible = new ArrayList<>();
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            if (property != BlockStateProperties.WATERLOGGED && property.getPossibleValues().size() > 1
                    && differs(block, states, property, remembered, looksPerState)) {
                visible.add(property);
            }
        }
        return List.copyOf(visible);
    }

    private static <T extends Comparable<T>> boolean differs(Block block, List<BlockState> states,
            Property<T> property, Function<BlockState, Object> look, int looksPerState) {
        List<T> values = List.copyOf(property.getPossibleValues());
        if (groupDiffers(block.defaultBlockState(), property, values, look)) {
            return true;
        }
        int groups = states.size() / values.size();
        int budget = Math.max(MIN_GROUPS, looksPerState / values.size());
        if (groups <= budget) {
            for (BlockState state : states) {
                if (state.getValue(property).equals(values.get(0)) && groupDiffers(state, property, values, look)) {
                    return true;
                }
            }
            return false;
        }
        // Random rather than evenly spaced: states are ordered as a product of their values, and a fixed step can
        // keep hitting the same value of another state.
        Random random = new Random(SEED);
        for (int i = 0; i < budget; i++) {
            if (groupDiffers(states.get(random.nextInt(states.size())), property, values, look)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Comparable<T>> boolean groupDiffers(BlockState base, Property<T> property,
            List<T> values, Function<BlockState, Object> look) {
        Object first = look.apply(base.setValue(property, values.get(0)));
        for (int i = 1; i < values.size(); i++) {
            if (!Objects.equals(first, look.apply(base.setValue(property, values.get(i))))) {
                return true;
            }
        }
        return false;
    }
}
