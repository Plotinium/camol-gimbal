package plotinium.camolgimbal.gametest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.server.GimbalActions;
import plotinium.camolgimbal.server.Messages;
import plotinium.camolgimbal.server.SeriesKeys;

/** Every click says what it did. Fake players drop the packet, so the tests read the message chosen. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MessageTests {
    private static final Map<UUID, Component> LAST = new ConcurrentHashMap<>();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState OAK = Blocks.OAK_PLANKS.defaultBlockState();

    static {
        Messages.setObserver((player, message) -> LAST.put(player.getUUID(), message));
    }

    private MessageTests() {
    }

    @Nullable
    private static String key(FakePlayer player) {
        Component message = LAST.get(player.getUUID());
        return message != null && message.getContents() instanceof TranslatableContents contents ? contents.getKey() : null;
    }

    private static List<Object> args(FakePlayer player) {
        return Arrays.asList(((TranslatableContents) LAST.get(player.getUUID()).getContents()).getArgs());
    }

    private static void assertMessage(GameTestHelper helper, FakePlayer player, String key, String what) {
        helper.assertValueEqual(key(player), key, what);
    }

    /** The name argument is the block's own translatable name, so it shows in the player's language. */
    private static void assertBlockArg(GameTestHelper helper, FakePlayer player, Block block, String what) {
        Object arg = args(player).get(0);
        helper.assertTrue(arg instanceof Component name && name.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(block.getDescriptionId()), what + ": " + arg);
    }

    private static BlockPos stone(GameTestHelper helper, int x, int z) {
        BlockPos pos = Kit.abs(helper, x, 1, z);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        return pos;
    }

    private static BlockPos camo(GameTestHelper helper, int x, int z, BlockState camo) {
        BlockPos pos = stone(helper, x, z);
        Kit.camo(helper, pos, camo, SavedCopy.NORMAL);
        return pos;
    }

    @GameTest(template = Kit.EMPTY)
    public static void copyAndRemove(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, new ItemStack(ModRegistry.GIMBAL.get()));
        BlockPos log = camo(helper, 1, 1, LOG);
        GimbalActions.copy(player, log, Direction.UP);
        assertMessage(helper, player, Messages.COPIED, "copy");
        assertBlockArg(helper, player, Blocks.OAK_LOG, "copied block");
        GimbalActions.copy(player, stone(helper, 3, 1), Direction.UP);
        assertMessage(helper, player, Messages.NOTHING_TO_COPY, "control: copy on a plain block");

        GimbalActions.remove(player, log, Direction.UP);
        assertMessage(helper, player, Messages.REMOVED, "remove");
        assertBlockArg(helper, player, Blocks.OAK_LOG, "removed block");
        GimbalActions.remove(player, stone(helper, 5, 1), Direction.UP);
        assertMessage(helper, player, Messages.NO_CAMO, "control: remove on a plain block");
        helper.succeed();
    }

    /**
     * One block pasted. A repeat of the held button right after it has nothing to do and leaves the message alone;
     * the same click later, on its own, says the camo is already like the copy.
     */
    @GameTest(template = Kit.EMPTY, timeoutTicks = 40)
    public static void pasteAndRepeat(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(OAK)));
        BlockPos pos = stone(helper, 2, 2);
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        assertMessage(helper, player, Messages.PASTED, "paste");
        assertBlockArg(helper, player, Blocks.OAK_PLANKS, "pasted block");
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
        assertMessage(helper, player, Messages.PASTED, "a repeat replaced the paste message");
        helper.runAfterDelay(15, () -> {
            GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(pos, Direction.UP));
            assertMessage(helper, player, Messages.ALREADY_LIKE_COPY, "a later click on the same camo");
            helper.succeed();
        });
    }

    @GameTest(template = Kit.EMPTY)
    public static void blockThatCannotTakeACamo(GameTestHelper helper) {
        FakePlayer player = Kit.creative(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(OAK)));
        BlockPos sponge = Kit.abs(helper, 1, 1, 1);
        helper.getLevel().setBlockAndUpdate(sponge, Blocks.SPONGE.defaultBlockState());
        GimbalActions.apply(player, InteractionHand.MAIN_HAND, Kit.hit(sponge, Direction.UP));
        assertMessage(helper, player, Messages.CANNOT_TAKE_CAMO, "sponge");

        FakePlayer other = Kit.creative(helper);
        Kit.hold(other, Kit.gimbal(Kit.copy(OAK)));
        GimbalActions.apply(other, InteractionHand.MAIN_HAND, Kit.hit(stone(helper, 5, 1), Direction.UP));
        assertMessage(helper, other, Messages.PASTED, "control: stone");
        helper.succeed();
    }

    /** A series says how many blocks changed: when it finishes, when items run out, when some are refused. */
    @GameTest(template = Kit.EMPTY)
    public static void seriesCounts(GameTestHelper helper) {
        GimbalSettings row = Kit.series(SeriesShape.ROW, 64);
        for (int z : new int[] {1, 3, 5}) {
            for (int x = 1; x <= 5; x++) {
                stone(helper, x, z);
            }
        }
        FakePlayer finishes = Kit.creative(helper);
        Kit.hold(finishes, Kit.gimbal(Kit.copy(OAK), row));
        SeriesKeys.set(finishes, true);
        GimbalActions.apply(finishes, InteractionHand.MAIN_HAND, Kit.hit(Kit.abs(helper, 3, 1, 1), Direction.UP));
        assertMessage(helper, finishes, Messages.BLOCKS_CHANGED, "series");
        helper.assertValueEqual(args(finishes).get(0), 5, "blocks changed");

        FakePlayer runsOut = Kit.survival(helper);
        Kit.hold(runsOut, Kit.gimbal(Kit.copy(OAK), row));
        Kit.give(runsOut, Kit.filledNormal(Blocks.OAK_PLANKS, 2));
        SeriesKeys.set(runsOut, true);
        GimbalActions.apply(runsOut, InteractionHand.MAIN_HAND, Kit.hit(Kit.abs(helper, 3, 1, 3), Direction.UP));
        assertMessage(helper, runsOut, Messages.SERIES_STOPPED, "items ran out");
        helper.assertValueEqual(args(runsOut).get(0), 2, "blocks changed before running out");

        BlockPos denied = Kit.abs(helper, 1, 1, 5);
        Deny.add(denied);
        try {
            FakePlayer refused = Kit.creative(helper);
            Kit.hold(refused, Kit.gimbal(Kit.copy(OAK), row));
            SeriesKeys.set(refused, true);
            GimbalActions.apply(refused, InteractionHand.MAIN_HAND, Kit.hit(Kit.abs(helper, 3, 1, 5), Direction.UP));
            assertMessage(helper, refused, Messages.REFUSED_SOME, "some refused");
            helper.assertValueEqual(args(refused), List.<Object>of(4, 1), "changed and skipped");
        } finally {
            Deny.clear(denied);
        }
        helper.succeed();
    }

    /** The menu: editing the camo, the copy and clearing say so; a switch says nothing. */
    @GameTest(template = Kit.EMPTY)
    public static void menuEdits(GameTestHelper helper) {
        FakePlayer switches = Kit.survival(helper);
        Kit.hold(switches, Kit.gimbal(Kit.copy(LOG)));
        GimbalActions.changeSettings(switches, Kit.settings(false, Set.of(), DirectionMode.FOLLOW_LOOK, SeriesShape.ROW, 8, false));
        helper.assertTrue(key(switches) == null, "a switch showed a message: " + key(switches));
        GimbalActions.clearCopy(switches);
        assertMessage(helper, switches, Messages.COPY_CLEARED, "control: clear on the same player");

        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, Kit.gimbal(Kit.copy(LOG)));
        BlockPos pos = camo(helper, 2, 2, LOG);
        BlockState lying = LOG.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        GimbalActions.editCamo(player, pos, lying);
        assertMessage(helper, player, Messages.CAMO_EDITED, "camo edit");
        assertBlockArg(helper, player, Blocks.OAK_LOG, "edited camo");
        GimbalActions.editCopy(player, lying);
        assertMessage(helper, player, Messages.COPY_EDITED, "copy edit");
        assertBlockArg(helper, player, Blocks.OAK_LOG, "edited copy");
        helper.succeed();
    }
}
