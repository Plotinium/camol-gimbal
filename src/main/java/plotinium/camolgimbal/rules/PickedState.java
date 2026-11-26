package plotinium.camolgimbal.rules;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Which state the menu's wheel shows (D-18): the one the player picked last, found by its name so that it carries
 * over to other blocks that have it, and the first one on a block that does not.
 */
public final class PickedState {
    private PickedState() {
    }

    /** The index of the state named {@code picked} among the offered ones, or 0. */
    public static int indexIn(List<Property<?>> offered, @Nullable String picked) {
        for (int i = 0; i < offered.size(); i++) {
            if (offered.get(i).getName().equals(picked)) {
                return i;
            }
        }
        return 0;
    }
}
