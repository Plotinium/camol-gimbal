package plotinium.camolgimbal.gametest;

import com.mojang.authlib.GameProfile;
import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.common.item.CamoItem;
import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import plotinium.camolgimbal.data.DirectionMode;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.data.SavedCopy;
import plotinium.camolgimbal.data.SeriesShape;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.rules.CamoView;
import plotinium.camolgimbal.rules.Planner;
import plotinium.camolgimbal.server.SeriesKeys;

/**
 * Shared steps of the action tests. Players are NeoForge fake players: a mock player joined to the server receives
 * Camol's sync packets without having negotiated them, and the server stops. A fake player is not ticked, so it
 * picks up nothing that drops.
 */
final class Kit {
    static final String EMPTY = "empty8";

    private Kit() {
    }

    /** A player standing in the middle of the test area, facing north. */
    static FakePlayer player(GameTestHelper helper, GameType mode) {
        FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "gimbal-test"));
        player.setGameMode(mode);
        Vec3 at = helper.absoluteVec(new Vec3(4.5, 1, 4.5));
        player.moveTo(at.x, at.y, at.z, 180, 0);
        player.getInventory().clearContent();
        SeriesKeys.set(player, false);
        boolean infinite = mode == GameType.CREATIVE;
        helper.assertTrue(player.hasInfiniteMaterials() == infinite, "the test player is not in " + mode);
        return player;
    }

    static FakePlayer survival(GameTestHelper helper) {
        return player(helper, GameType.SURVIVAL);
    }

    static FakePlayer creative(GameTestHelper helper) {
        return player(helper, GameType.CREATIVE);
    }

    static void hold(FakePlayer player, ItemStack gimbal) {
        player.setItemInHand(InteractionHand.MAIN_HAND, gimbal);
    }

    static void give(FakePlayer player, ItemStack... stacks) {
        for (ItemStack stack : stacks) {
            player.getInventory().add(stack);
        }
    }

    static ItemStack gimbal(@Nullable SavedCopy copy, GimbalSettings settings) {
        ItemStack stack = new ItemStack(ModRegistry.GIMBAL.get());
        if (copy != null) {
            stack.set(ModRegistry.COPY, copy);
        }
        stack.set(ModRegistry.SETTINGS, settings);
        return stack;
    }

    static ItemStack gimbal(SavedCopy copy) {
        return gimbal(copy, GimbalSettings.DEFAULT);
    }

    static SavedCopy copy(BlockState state) {
        return new SavedCopy(SavedCopy.NORMAL, state, Direction.NORTH);
    }

    static SavedCopy copy(String type, BlockState state, Direction look) {
        return new SavedCopy(type, state, look);
    }

    static GimbalSettings settings(boolean pasteType, Set<String> skipped, DirectionMode direction, SeriesShape shape,
            int limit, boolean replaceBlock) {
        return new GimbalSettings(pasteType, skipped, true, direction, shape, limit, replaceBlock, true);
    }

    static GimbalSettings replacing() {
        return settings(true, Set.of(), DirectionMode.AS_COPIED, SeriesShape.CONNECTED, 64, true);
    }

    static GimbalSettings series(SeriesShape shape, int limit) {
        return settings(true, Set.of(), DirectionMode.AS_COPIED, shape, limit, false);
    }

    static BlockPos abs(GameTestHelper helper, int x, int y, int z) {
        return helper.absolutePos(new BlockPos(x, y, z));
    }

    static void camo(GameTestHelper helper, BlockPos absolute, BlockState state, String type) {
        CamoHelper.setCamo(helper.getLevel(), Map.of(absolute, new CamoPosition(type, state)));
    }

    @Nullable
    static CamoPosition camoAt(GameTestHelper helper, BlockPos absolute) {
        return CamoView.of(helper.getLevel()).camoAt(absolute);
    }

    static Planner.Hit hit(BlockPos absolute, Direction face) {
        return new Planner.Hit(absolute, face, Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)));
    }

    static ItemStack filled(Item camo, Block block, int count) {
        ItemStack stack = CamoItem.getCamoItem(block.defaultBlockState(), new ItemStack(camo, count));
        return stack;
    }

    static ItemStack filledNormal(Block block, int count) {
        return filled(Camol.CAMO_ITEM.get(), block, count);
    }

    static ItemStack filledSolid(Block block, int count) {
        return filled(Camol.SOLID_CAMO_ITEM.get(), block, count);
    }

    static ItemStack emptyNormal(int count) {
        return new ItemStack(Camol.CAMO_ITEM.get(), count);
    }

    static ItemStack emptySolid(int count) {
        return new ItemStack(Camol.SOLID_CAMO_ITEM.get(), count);
    }

    /** How many items matching {@code test} the player carries (main inventory and off hand). */
    static int carried(FakePlayer player, Predicate<ItemStack> test) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            count += test.test(stack) ? stack.getCount() : 0;
        }
        for (ItemStack stack : player.getInventory().offhand) {
            count += test.test(stack) ? stack.getCount() : 0;
        }
        return count;
    }

    static Predicate<ItemStack> isFilled(Item camo, Block block) {
        return stack -> stack.is(camo) && stack.has(Camol.BLOCK_COMPONENT)
                && stack.get(Camol.BLOCK_COMPONENT).is(block);
    }

    static Predicate<ItemStack> isEmptyCamo(Item camo) {
        return stack -> stack.is(camo) && !stack.has(Camol.BLOCK_COMPONENT);
    }

    /** Items lying in the test area (inflated so the player's own drops count too). */
    static int dropped(GameTestHelper helper, Predicate<ItemStack> test) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(1))
                .stream()
                .filter(entity -> test.test(entity.getItem()))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    static int dropped(GameTestHelper helper) {
        return dropped(helper, stack -> true);
    }
}
