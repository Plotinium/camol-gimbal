package plotinium.camolgimbal.client;

import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.net.ClearCopyPayload;
import plotinium.camolgimbal.net.EditCamoPayload;
import plotinium.camolgimbal.net.EditCopyPayload;
import plotinium.camolgimbal.net.SettingsPayload;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * What the menu shows and changes: the camouflage in front or the saved copy, and the switches of the Gimbal.
 *
 * <p>Every change is sent at once, and the menu does not wait for the server to show it. From its first change on, it
 * shows its own switches, so an older value arriving from the server changes nothing on screen; after the clear button
 * the copy stays gone in the same way. An edited block state is shown for two seconds after the last edit, long enough
 * for the server's answer, and the next edit starts from it: an edit made before the answer keeps the one before. A
 * refused edit shows for those two seconds too, then the real state comes back.
 */
final class MenuState {
    private static final int PENDING_TICKS = 40;

    /** The camouflage being edited, or null when the menu edits the saved copy. */
    @Nullable
    private final BlockPos target;
    @Nullable
    private final Block targetBlock;
    /** The switches since the first change made here; null before it. */
    @Nullable
    private GimbalSettings changed;
    private boolean cleared;
    /** The last state sent, while the server's answer may be on its way. */
    @Nullable
    private BlockState pending;
    private int pendingAge;

    MenuState(@Nullable BlockPos target, @Nullable Block targetBlock) {
        this.target = target;
        this.targetBlock = targetBlock;
    }

    /** The Gimbal the server changes for the menu: the main hand if it holds one, else the off hand. */
    static ItemStack gimbal() {
        LocalPlayer player = Minecraft.getInstance().player;
        InteractionHand hand = player == null ? null : GimbalInput.gimbalHand(player);
        return hand == null ? ItemStack.EMPTY : player.getItemInHand(hand);
    }

    boolean editsCamo() {
        return target != null;
    }

    /** Whether the camouflage being edited disappeared or changed block. */
    boolean targetLost() {
        if (target == null) {
            return false;
        }
        CamoPosition camo = ClientCamos.VIEW.camoAt(target);
        return camo == null || !camo.state().is(targetBlock);
    }

    GimbalSettings settings() {
        return changed != null ? changed : gimbal().getOrDefault(ModRegistry.SETTINGS, GimbalSettings.DEFAULT);
    }

    @Nullable
    SavedCopy copy() {
        return cleared ? null : gimbal().get(ModRegistry.COPY);
    }

    /** The state of the saved copy as the menu shows it, or null without a copy. */
    @Nullable
    BlockState copyState() {
        SavedCopy copy = copy();
        if (copy == null) {
            return null;
        }
        return target == null ? shown(copy.state()) : copy.state();
    }

    /** The block state the wheel and the list edit, or null when there is none. */
    @Nullable
    BlockState editedState() {
        if (target == null) {
            return copyState();
        }
        CamoPosition camo = ClientCamos.VIEW.camoAt(target);
        return camo == null ? null : shown(camo.state());
    }

    /** Sends a new state of the edited block. */
    void edit(BlockState chosen) {
        if (target != null) {
            if (targetLost()) {
                return;
            }
            PacketDistributor.sendToServer(new EditCamoPayload(target, chosen));
        } else {
            SavedCopy copy = copy();
            if (copy == null) {
                return;
            }
            PacketDistributor.sendToServer(new EditCopyPayload(chosen));
            // The preview follows even if the menu closes before the server answers.
            gimbal().set(ModRegistry.COPY, copy.withState(chosen));
        }
        pending = chosen;
        pendingAge = 0;
    }

    void tick() {
        if (pending != null && ++pendingAge > PENDING_TICKS) {
            pending = null;
        }
    }

    /** Changes switches: sent at once and written on the client's own stack, which the preview reads. */
    void change(Consumer<Draft> edit) {
        Draft draft = new Draft(settings());
        edit.accept(draft);
        changed = draft.build();
        store(changed);
        PacketDistributor.sendToServer(new SettingsPayload(changed));
    }

    /** The clear button: the copy goes, and the unticked states with it (D-19), as the server does. */
    void clear() {
        Draft draft = new Draft(settings());
        draft.skipped.clear();
        changed = draft.build();
        cleared = true;
        if (target == null) {
            pending = null;
        }
        store(changed);
        ItemStack gimbal = gimbal();
        if (!gimbal.isEmpty()) {
            gimbal.remove(ModRegistry.COPY);
        }
        PacketDistributor.sendToServer(ClearCopyPayload.INSTANCE);
    }

    private BlockState shown(BlockState real) {
        return pending != null && pending.is(real.getBlock()) ? pending : real;
    }

    private static void store(GimbalSettings settings) {
        ItemStack gimbal = gimbal();
        if (gimbal.isEmpty()) {
            return;
        }
        if (settings.equals(GimbalSettings.DEFAULT)) {
            gimbal.remove(ModRegistry.SETTINGS);
        } else {
            gimbal.set(ModRegistry.SETTINGS, settings);
        }
    }

    /** The switches while one of them changes. */
    static final class Draft {
        boolean pasteType;
        final Set<String> skipped;
        boolean seeThrough;
        DirectionMode direction;
        SeriesShape shape;
        int limit;
        boolean replaceBlock;
        boolean preview;

        private Draft(GimbalSettings from) {
            pasteType = from.pasteType();
            skipped = new HashSet<>(from.skippedStates());
            seeThrough = from.seeThrough();
            direction = from.direction();
            shape = from.shape();
            limit = from.limit();
            replaceBlock = from.replaceBlock();
            preview = from.preview();
        }

        private GimbalSettings build() {
            return new GimbalSettings(pasteType, Set.copyOf(skipped), seeThrough, direction, shape, limit,
                    replaceBlock, preview);
        }
    }
}
