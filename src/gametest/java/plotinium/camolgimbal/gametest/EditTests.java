package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.utils.CamoPosition;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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

/** The menu packets: a state of another block is refused, in the world and in the copy. */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EditTests {
    private EditTests() {
    }

    @GameTest(template = Kit.EMPTY)
    public static void editCamoAcceptsOnlyTheSameBlock(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        Kit.hold(player, new ItemStack(ModRegistry.GIMBAL.get()));
        BlockPos pos = Kit.abs(helper, 2, 1, 2);
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        Kit.camo(helper, pos, log, SavedCopy.SOLID);

        helper.assertFalse(GimbalActions.editCamo(player, pos, Blocks.DIAMOND_BLOCK.defaultBlockState()), "a forged block was accepted");
        helper.assertTrue(Kit.camoAt(helper, pos).state().equals(log), "a forged block changed the camo");

        BlockState lying = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        helper.assertTrue(GimbalActions.editCamo(player, pos, lying), "control: a state of the same block was refused");
        CamoPosition edited = Kit.camoAt(helper, pos);
        helper.assertTrue(edited.state().equals(lying) && edited.camoType().equals(SavedCopy.SOLID), "control: wrong edit " + edited);
        helper.succeed();
    }

    @GameTest(template = Kit.EMPTY)
    public static void editCopyAcceptsOnlyTheCopiedBlock(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        ItemStack gimbal = Kit.gimbal(Kit.copy(log));
        player.setItemInHand(InteractionHand.OFF_HAND, gimbal);

        helper.assertFalse(GimbalActions.editCopy(player, Blocks.DIAMOND_BLOCK.defaultBlockState()), "a forged block was accepted");
        helper.assertTrue(gimbal.get(ModRegistry.COPY).state().equals(log), "a forged block changed the copy");

        BlockState lying = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        helper.assertTrue(GimbalActions.editCopy(player, lying), "control: a state of the copied block was refused");
        helper.assertTrue(gimbal.get(ModRegistry.COPY).state().equals(lying), "control: the copy did not change");
        helper.succeed();
    }

    /** Clearing removes the copy and the unticked states; the settings are clamped, and defaults leave no data. */
    @GameTest(template = Kit.EMPTY)
    public static void clearCopyAndSettings(GameTestHelper helper) {
        FakePlayer player = Kit.survival(helper);
        GimbalSettings skipping = Kit.settings(false, Set.of("axis"), DirectionMode.FOLLOW_LOOK, SeriesShape.ROW, 8, true);
        ItemStack gimbal = Kit.gimbal(Kit.copy(Blocks.OAK_LOG.defaultBlockState()), skipping);
        Kit.hold(player, gimbal);

        helper.assertTrue(GimbalActions.clearCopy(player), "clear refused");
        helper.assertTrue(gimbal.get(ModRegistry.COPY) == null, "the copy is still there");
        GimbalSettings cleared = gimbal.get(ModRegistry.SETTINGS);
        helper.assertTrue(cleared != null && cleared.skippedStates().isEmpty() && cleared.limit() == 8
                && !cleared.pasteType(), "clearing changed the wrong settings: " + cleared);

        helper.assertTrue(GimbalActions.changeSettings(player, GimbalSettings.DEFAULT), "default settings refused");
        helper.assertFalse(gimbal.has(ModRegistry.SETTINGS), "default settings left data on the item");
        helper.succeed();
    }

    /** A forged settings packet: limit 13, 100 unticked names and one name too long, as raw data. */
    private static CompoundTag forgedSettings() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("limit", 13);
        ListTag names = new ListTag();
        for (int i = 0; i < 100; i++) {
            names.add(StringTag.valueOf("state" + i));
        }
        names.add(StringTag.valueOf("x".repeat(65)));
        tag.put("skipped_states", names);
        return tag;
    }

    private static void assertClamped(GameTestHelper helper, GimbalSettings settings, String path) {
        helper.assertValueEqual(settings.limit(), 64, path + ": limit");
        helper.assertTrue(settings.skippedStates().size() <= GimbalSettings.MAX_SKIPPED, path + ": too many names kept");
        helper.assertTrue(settings.skippedStates().stream().allMatch(name -> name.length() <= 64), path + ": a long name kept");
        helper.assertFalse(settings.skippedStates().isEmpty(), path + ": control: no name kept at all");
    }

    /** Values from outside are clamped by the real codecs: the item data codec and the packet codec. */
    @GameTest(template = Kit.EMPTY)
    public static void forgedSettingsAreClamped(GameTestHelper helper) {
        GimbalSettings decoded = GimbalSettings.CODEC.parse(NbtOps.INSTANCE, forgedSettings()).getOrThrow();
        assertClamped(helper, decoded, "item data");

        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        buf.writeNbt(forgedSettings());
        assertClamped(helper, GimbalSettings.STREAM_CODEC.decode(buf), "packet");

        FakePlayer player = Kit.survival(helper);
        ItemStack gimbal = new ItemStack(ModRegistry.GIMBAL.get());
        Kit.hold(player, gimbal);
        helper.assertTrue(GimbalActions.changeSettings(player, decoded), "settings refused");
        helper.assertValueEqual(gimbal.get(ModRegistry.SETTINGS).limit(), 64, "stored limit");
        helper.succeed();
    }
}
