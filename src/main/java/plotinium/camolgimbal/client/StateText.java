package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * The names of states and values as the player reads them. The addon translates the common ones and shows the plain
 * name of the others.
 */
final class StateText {
    private StateText() {
    }

    static Component name(Property<?> property) {
        String name = property.getName();
        return Component.translatableWithFallback("state.camolgimbal." + name, name);
    }

    static <T extends Comparable<T>> Component value(BlockState state, Property<T> property) {
        String value = property.getName(state.getValue(property));
        return Component.translatableWithFallback("state.camolgimbal.value." + value, value);
    }

    /**
     * One entry of a compact line, which must read without the state's name beside it: the words the addon has for
     * this value of this state ("Closed"); else "name: value" for a yes or no, a number or a single letter; else the
     * value alone ("North").
     */
    static <T extends Comparable<T>> Component phrase(BlockState state, Property<T> property) {
        T value = state.getValue(property);
        String written = property.getName(value);
        String words = "state.camolgimbal." + property.getName() + "." + written;
        if (Language.getInstance().has(words)) {
            return Component.translatable(words);
        }
        if (value instanceof Boolean || value instanceof Number || written.length() == 1) {
            return Component.translatable("label.camolgimbal.state", name(property), value(state, property));
        }
        return value(state, property);
    }

    /** The entries of a block state, for the states the Gimbal offers. */
    static List<Component> phrases(BlockState state) {
        List<Component> phrases = new ArrayList<>();
        for (Property<?> property : BlockLooks.visibleStates(state.getBlock())) {
            phrases.add(phrase(state, property));
        }
        return phrases;
    }
}
