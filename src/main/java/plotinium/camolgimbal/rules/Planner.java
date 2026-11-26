package plotinium.camolgimbal.rules;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;

/**
 * From a saved copy, the Gimbal's switches and a click to the list of camouflage changes (D-10, D-11, D-19). It
 * reads the world and writes nothing, spends nothing and checks no permission: the server and the preview both call
 * it.
 */
public final class Planner {
    private static final String WATERLOGGED = BlockStateProperties.WATERLOGGED.getName();

    public enum Kind {
        /** A new camouflage where there was none: it is paid for. */
        PLACE,
        /** An existing camouflage keeps its block; its states or type change: free. */
        UPDATE,
        /** An existing camouflage takes the copied block: the new one is paid for, the old one returns. */
        REPLACE
    }

    /** The click: the camouflaged (or real) block, the face clicked and the exact point. */
    public record Hit(BlockPos pos, Direction face, Vec3 location) {
    }

    /** {@code before} is null for {@link Kind#PLACE}. */
    public record Change(BlockPos pos, @Nullable CamoPosition before, CamoPosition after, Kind kind) {
    }

    private Planner() {
    }

    /**
     * @param look the player's horizontal facing now
     * @param series whether the series key is held
     */
    public static List<Change> plan(Level level, CamoView view, @Nullable Player player, SavedCopy copy,
            GimbalSettings settings, Hit hit, Direction look, boolean series) {
        List<BlockPos> targets = series
                ? Selection.select(level, view, hit.pos(), hit.face(), look, settings.shape(), settings.limit())
                : List.of(hit.pos().immutable());
        List<Change> changes = new ArrayList<>();
        for (BlockPos pos : targets) {
            Change change = changeAt(level, view, player, copy, settings, hit, look, pos);
            if (change != null) {
                changes.add(change);
            }
        }
        return changes;
    }

    @Nullable
    private static Change changeAt(Level level, CamoView view, @Nullable Player player, SavedCopy copy,
            GimbalSettings settings, Hit hit, Direction look, BlockPos pos) {
        CamoPosition before = view.camoAt(pos);
        Block copied = copy.state().getBlock();
        Kind kind;
        if (before == null) {
            BlockState real = level.getBlockState(pos);
            if (real.isAir() || real.is(Camol.CAMO_BLACKLIST)) {
                return null;
            }
            kind = Kind.PLACE;
        } else if (settings.replaceBlock() && !before.state().is(copied)) {
            kind = Kind.REPLACE;
        } else {
            kind = Kind.UPDATE;
        }

        Block target = kind == Kind.UPDATE ? before.state().getBlock() : copied;
        Vec3 click = hit.location().add(Vec3.atLowerCornerOf(pos.subtract(hit.pos())));
        BlockState placed = Placement.stateFor(level, player, target, pos, hit.face(), click);

        BlockState source = copy.state();
        if (settings.direction() == DirectionMode.FOLLOW_LOOK) {
            source = source.rotate(level, pos, turn(copy.look(), look));
        }

        BlockState state;
        if (kind == Kind.UPDATE) {
            state = before.state();
        } else {
            state = placed.hasProperty(BlockStateProperties.WATERLOGGED)
                    ? placed.setValue(BlockStateProperties.WATERLOGGED, false)
                    : placed;
        }
        for (Property<?> property : source.getProperties()) {
            String name = property.getName();
            if (name.equals(WATERLOGGED) || settings.skippedStates().contains(name)) {
                continue;
            }
            BlockState from = settings.direction() == DirectionMode.FOLLOW_FACE && isOrientation(property)
                    ? placed
                    : source;
            state = withValueOf(state, from, name);
        }

        String type = kind == Kind.PLACE || settings.pasteType() ? copy.type() : before.camoType();
        CamoPosition after = new CamoPosition(type, state);
        if (after.equals(before)) {
            return null;
        }
        return new Change(pos.immutable(), before, after, kind);
    }

    /** Copies the value of the property called {@code name} from one state to another, matched by name and value. */
    static BlockState withValueOf(BlockState into, BlockState from, String name) {
        Property<?> fromProperty = from.getBlock().getStateDefinition().getProperty(name);
        Property<?> intoProperty = into.getBlock().getStateDefinition().getProperty(name);
        if (fromProperty == null || intoProperty == null) {
            return into;
        }
        return setByName(into, intoProperty, valueName(from, fromProperty));
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState setByName(BlockState state, Property<T> property,
            String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.map(v -> state.setValue(property, v)).orElse(state);
    }

    /** The turn from the facing at copy time to the facing now, clockwise seen from above. */
    static Rotation turn(Direction from, Direction to) {
        int steps = Math.floorMod(to.get2DDataValue() - from.get2DDataValue(), 4);
        return Rotation.values()[steps];
    }

    /**
     * Orientation states: direction and axis, half, attach face, slab type, front-and-top and the 16-step rotation.
     * Not the half of a door or a tall plant: those are two blocks.
     */
    static boolean isOrientation(Property<?> property) {
        if (property instanceof DirectionProperty) {
            return true;
        }
        if (property instanceof EnumProperty<?> enumProperty) {
            Class<?> values = enumProperty.getValueClass();
            return values == Direction.class || values == Direction.Axis.class || values == Half.class
                    || values == AttachFace.class || values == SlabType.class || values == FrontAndTop.class;
        }
        return property == BlockStateProperties.ROTATION_16;
    }
}
