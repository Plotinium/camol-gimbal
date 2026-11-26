package plotinium.camolgimbal.server;

import cy.jdkdigital.camol.common.item.CamoItem;
import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.item.GimbalItem;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.rules.CamoView;
import plotinium.camolgimbal.rules.Planner;

/**
 * The server side of every Gimbal action. Each one checks that the player holds a Gimbal, that the block is loaded
 * and within reach, and the edit permission, before it reads or writes a camouflage.
 *
 * <p>Which Gimbal: the clicked hand for apply; the main hand for copy and remove (left click); for the menu packets
 * the main hand if it holds a Gimbal, else the off hand.
 */
public final class GimbalActions {
    private static final double REACH_MARGIN = 1.0;

    /** What an apply did, for the tests. */
    public record Outcome(int written, int refused, boolean ranOutOfItems) {
        static final Outcome NOTHING = new Outcome(0, 0, false);
    }

    private GimbalActions() {
    }

    /** Right click on a block (D-10, D-11, D-17, D-19). */
    public static Outcome apply(ServerPlayer player, InteractionHand hand, Planner.Hit hit) {
        ItemStack gimbal = player.getItemInHand(hand);
        if (!gimbal.is(ModRegistry.GIMBAL) || !reachable(player, hit.pos())) {
            return Outcome.NOTHING;
        }
        SavedCopy copy = gimbal.get(ModRegistry.COPY);
        if (copy == null) {
            Messages.show(player, Messages.NOTHING_COPIED);
            return Outcome.NOTHING;
        }
        ServerLevel level = player.serverLevel();
        CamoView view = CamoView.of(level);
        GimbalSettings settings = GimbalItem.settings(gimbal);
        List<Planner.Change> changes = Planner.plan(level, view, player, copy, settings, hit, player.getDirection(),
                SeriesKeys.isHeld(player));

        boolean free = player.hasInfiniteMaterials();
        int written = 0;
        int refused = 0;
        Planner.Change unpaid = null;
        for (Planner.Change change : changes) {
            // The camo may have changed since planning (another change of this series, or another mod).
            if (!Objects.equals(view.camoAt(change.pos()), change.before())) {
                continue;
            }
            if (!Permissions.canEdit(player, change.pos(), hit.face(), gimbal)) {
                refused++;
                continue;
            }
            boolean paid = change.kind() != Planner.Kind.UPDATE;
            if (paid && !free) {
                Payment payment = Payment.find(player, change.after().state().getBlock(), change.after().camoType());
                if (payment == null) {
                    unpaid = change;
                    break;
                }
                payment.spend();
            }
            CamoHelper.setCamo(level, Map.of(change.pos(), change.after()));
            if (change.kind() == Planner.Kind.REPLACE && !free) {
                giveBack(player, Objects.requireNonNull(change.before()));
            }
            written++;
        }

        if (unpaid != null) {
            if (written == 0) {
                Messages.itemsMissing(player, unpaid.after().state().getBlock(), unpaid.after().camoType());
            } else {
                Messages.show(player, Messages.SERIES_STOPPED, written);
            }
        } else if (refused > 0) {
            if (written == 0) {
                Messages.show(player, Messages.REFUSED);
            } else {
                Messages.show(player, Messages.REFUSED_SOME, refused);
            }
        }
        return new Outcome(written, refused, unpaid != null);
    }

    /** Left click on a camouflaged block: the camo goes into the Gimbal held in the main hand. */
    public static boolean copy(ServerPlayer player, BlockPos pos, Direction face) {
        ItemStack gimbal = player.getMainHandItem();
        if (!gimbal.is(ModRegistry.GIMBAL) || !reachable(player, pos)) {
            return false;
        }
        CamoPosition camo = CamoView.of(player.serverLevel()).camoAt(pos);
        if (camo == null) {
            Messages.show(player, Messages.NOTHING_TO_COPY);
            return false;
        }
        if (!Permissions.canEdit(player, pos, face, gimbal)) {
            Messages.show(player, Messages.REFUSED);
            return false;
        }
        gimbal.set(ModRegistry.COPY, new SavedCopy(camo.camoType(), camo.state(), player.getDirection()));
        return true;
    }

    /** Shift and left click: the camouflage is removed and its camo item drops, except in creative. */
    public static boolean remove(ServerPlayer player, BlockPos pos, Direction face) {
        ItemStack gimbal = player.getMainHandItem();
        if (!gimbal.is(ModRegistry.GIMBAL) || !reachable(player, pos)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        CamoPosition camo = CamoView.of(level).camoAt(pos);
        if (camo == null) {
            Messages.show(player, Messages.NO_CAMO);
            return false;
        }
        if (!Permissions.canEdit(player, pos, face, gimbal)) {
            Messages.show(player, Messages.REFUSED);
            return false;
        }
        CamoHelper.setCamo(level, Map.of(pos.immutable(), new CamoPosition(SavedCopy.NORMAL, Blocks.AIR.defaultBlockState())));
        if (!player.hasInfiniteMaterials()) {
            Block.popResourceFromFace(level, pos, face, camoItemOf(camo));
        }
        return true;
    }

    /** The menu on a camouflage: a new state of the same block. Anything else is refused. */
    public static boolean editCamo(ServerPlayer player, BlockPos pos, BlockState state) {
        ItemStack gimbal = heldGimbal(player);
        if (gimbal == null || !reachable(player, pos)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        CamoPosition camo = CamoView.of(level).camoAt(pos);
        if (camo == null || !state.is(camo.state().getBlock())) {
            return false;
        }
        if (!Permissions.canEdit(player, pos, Direction.UP, gimbal)) {
            Messages.show(player, Messages.REFUSED);
            return false;
        }
        BlockState edited = keepWaterlogged(state, camo.state());
        if (edited == camo.state()) {
            return false;
        }
        CamoHelper.setCamo(level, Map.of(pos.immutable(), new CamoPosition(camo.camoType(), edited)));
        return true;
    }

    /** The menu on the saved copy: a new state of the copied block. Anything else is refused. */
    public static boolean editCopy(ServerPlayer player, BlockState state) {
        ItemStack gimbal = heldGimbal(player);
        SavedCopy copy = gimbal == null ? null : gimbal.get(ModRegistry.COPY);
        if (copy == null || !state.is(copy.state().getBlock())) {
            return false;
        }
        gimbal.set(ModRegistry.COPY, copy.withState(state));
        return true;
    }

    /** The clear button: the copy goes, and the unticked states with it (D-19). */
    public static boolean clearCopy(ServerPlayer player) {
        ItemStack gimbal = heldGimbal(player);
        if (gimbal == null) {
            return false;
        }
        gimbal.remove(ModRegistry.COPY);
        GimbalSettings s = GimbalItem.settings(gimbal);
        storeSettings(gimbal, new GimbalSettings(s.pasteType(), java.util.Set.of(), s.seeThrough(), s.direction(),
                s.shape(), s.limit(), s.replaceBlock(), s.preview()));
        return true;
    }

    /** The switches. The settings record clamps every value when it is built, decoding included. */
    public static boolean changeSettings(ServerPlayer player, GimbalSettings settings) {
        ItemStack gimbal = heldGimbal(player);
        if (gimbal == null) {
            return false;
        }
        storeSettings(gimbal, settings);
        return true;
    }

    /** The Gimbal for the menu packets: the main hand if it holds one, else the off hand. */
    @Nullable
    public static ItemStack heldGimbal(ServerPlayer player) {
        if (player.getMainHandItem().is(ModRegistry.GIMBAL)) {
            return player.getMainHandItem();
        }
        return player.getOffhandItem().is(ModRegistry.GIMBAL) ? player.getOffhandItem() : null;
    }

    private static void storeSettings(ItemStack gimbal, GimbalSettings settings) {
        if (settings.equals(GimbalSettings.DEFAULT)) {
            gimbal.remove(ModRegistry.SETTINGS);
        } else {
            gimbal.set(ModRegistry.SETTINGS, settings);
        }
    }

    private static boolean reachable(ServerPlayer player, BlockPos pos) {
        return player.serverLevel().isLoaded(pos) && player.canInteractWithBlock(pos, REACH_MARGIN);
    }

    private static ItemStack camoItemOf(CamoPosition camo) {
        return CamoItem.getCamoItem(camo.state(), SavedCopy.SOLID.equals(camo.camoType()));
    }

    /** The old camo of a replaced block returns to the inventory, or drops at the player if it is full. */
    private static void giveBack(ServerPlayer player, CamoPosition old) {
        ItemStack stack = camoItemOf(old);
        player.getInventory().add(stack);
        if (!stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    /** The camo draws no water, so the menu never changes {@code waterlogged}. */
    private static BlockState keepWaterlogged(BlockState edited, BlockState current) {
        if (edited.hasProperty(BlockStateProperties.WATERLOGGED) && current.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return edited.setValue(BlockStateProperties.WATERLOGGED, current.getValue(BlockStateProperties.WATERLOGGED));
        }
        return edited;
    }
}
