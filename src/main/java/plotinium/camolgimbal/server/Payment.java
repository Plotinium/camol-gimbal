package plotinium.camolgimbal.server;

import cy.jdkdigital.camol.Camol;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plotinium.camolgimbal.data.SavedCopy;

/**
 * What a new camouflage costs (D-17), taken from the main inventory and the off hand. In order: a filled camo item of
 * the block, own type first, then the other; else an empty camo item, own type first, plus one block item. The block
 * item must be that block's, outside {@code camol:disallowed_camo_blocks}, and carry no components beyond its
 * default ones. Every stack is found before any of them shrinks.
 */
public final class Payment {
    private final List<ItemStack> stacks;

    private Payment(List<ItemStack> stacks) {
        this.stacks = stacks;
    }

    /** The stacks that pay for one camouflage of {@code block} with camo {@code type}, or null if they are missing. */
    @Nullable
    public static Payment find(Player player, Block block, String type) {
        List<ItemStack> inventory = carried(player.getInventory());
        Item own = camoItem(type);
        Item other = camoItem(SavedCopy.SOLID.equals(type) ? SavedCopy.NORMAL : SavedCopy.SOLID);

        for (Item camo : List.of(own, other)) {
            ItemStack filled = first(inventory, stack -> isFilled(stack, camo, block));
            if (filled != null) {
                return new Payment(List.of(filled));
            }
        }
        ItemStack blockItem = first(inventory, stack -> isPayingBlockItem(stack, block));
        if (blockItem == null) {
            return null;
        }
        for (Item camo : List.of(own, other)) {
            ItemStack empty = first(inventory, stack -> stack.is(camo) && !stack.has(Camol.BLOCK_COMPONENT));
            if (empty != null) {
                return new Payment(List.of(empty, blockItem));
            }
        }
        return null;
    }

    public void spend() {
        for (ItemStack stack : stacks) {
            stack.shrink(1);
        }
    }

    public static Item camoItem(String type) {
        return SavedCopy.SOLID.equals(type) ? Camol.SOLID_CAMO_ITEM.get() : Camol.CAMO_ITEM.get();
    }

    private static boolean isFilled(ItemStack stack, Item camo, Block block) {
        if (!stack.is(camo)) {
            return false;
        }
        BlockState held = stack.get(Camol.BLOCK_COMPONENT);
        return held != null && held.is(block);
    }

    private static boolean isPayingBlockItem(ItemStack stack, Block block) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block
                && !stack.is(Camol.CRAFTING_BLACKLIST) && stack.getComponentsPatch().isEmpty();
    }

    private static List<ItemStack> carried(Inventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.items);
        stacks.addAll(inventory.offhand);
        return stacks;
    }

    @Nullable
    private static ItemStack first(List<ItemStack> stacks, Predicate<ItemStack> test) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && test.test(stack)) {
                return stack;
            }
        }
        return null;
    }
}
