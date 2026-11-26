package plotinium.camolgimbal.item;

import cy.jdkdigital.camol.Camol;
import net.minecraft.world.item.ItemStack;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * Whether camouflaged blocks are drawn see-through for a player holding these two stacks (D-8, D-19). Camol's own rule
 * looks at its camo items in the main hand only; a Gimbal counts in either hand while its switch is on.
 */
public final class SeeThrough {
    private SeeThrough() {
    }

    public static boolean wanted(ItemStack mainHand, ItemStack offHand) {
        return isCamoItem(mainHand) || isSeeThroughGimbal(mainHand) || isSeeThroughGimbal(offHand);
    }

    private static boolean isCamoItem(ItemStack stack) {
        return stack.is(Camol.CAMO_ITEM) || stack.is(Camol.SOLID_CAMO_ITEM);
    }

    private static boolean isSeeThroughGimbal(ItemStack stack) {
        return stack.is(ModRegistry.GIMBAL) && GimbalItem.settings(stack).seeThrough();
    }
}
