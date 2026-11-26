package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.item.SeeThrough;
import plotinium.camolgimbal.registry.ModRegistry;

/** Which held items draw camouflaged blocks see-through (D-8, D-19). Every "no" sits next to a "yes". */
@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeeThroughTests {
    private static final String EMPTY = "empty8";

    private SeeThroughTests() {
    }

    /** Camol's own rule: a camo item counts in the main hand, not in the off hand. */
    @GameTest(template = EMPTY)
    public static void camoItemCountsInTheMainHandOnly(GameTestHelper helper) {
        ItemStack camo = new ItemStack(Camol.CAMO_ITEM.get());
        ItemStack solid = new ItemStack(Camol.SOLID_CAMO_ITEM.get());
        ItemStack empty = ItemStack.EMPTY;

        helper.assertTrue(SeeThrough.wanted(camo, empty), "Camo item in the main hand is not see-through");
        helper.assertTrue(SeeThrough.wanted(solid, empty), "Solid camo item in the main hand is not see-through");
        helper.assertFalse(SeeThrough.wanted(empty, camo), "Camo item in the off hand is see-through");
        helper.assertFalse(SeeThrough.wanted(empty, solid), "Solid camo item in the off hand is see-through");
        helper.succeed();
    }

    /** A Gimbal counts in either hand while its switch is on, and in neither while it is off. */
    @GameTest(template = EMPTY)
    public static void gimbalCountsInEitherHandWithTheSwitchOn(GameTestHelper helper) {
        ItemStack on = new ItemStack(ModRegistry.GIMBAL.get());
        ItemStack off = gimbal(false);
        ItemStack stone = new ItemStack(Items.STONE);

        helper.assertTrue(SeeThrough.wanted(on, stone), "Gimbal with the switch on, main hand, is not see-through");
        helper.assertTrue(SeeThrough.wanted(stone, on), "Gimbal with the switch on, off hand, is not see-through");
        helper.assertTrue(SeeThrough.wanted(off, on), "A Gimbal with the switch off hides the other one");
        helper.assertFalse(SeeThrough.wanted(off, stone), "Gimbal with the switch off, main hand, is see-through");
        helper.assertFalse(SeeThrough.wanted(stone, off), "Gimbal with the switch off, off hand, is see-through");
        helper.assertFalse(SeeThrough.wanted(stone, stone), "A plain block is see-through");
        helper.succeed();
    }

    /** With the switch off and a camo item in the main hand, Camol's own rule still applies. */
    @GameTest(template = EMPTY)
    public static void camoItemStillCountsNextToASwitchedOffGimbal(GameTestHelper helper) {
        ItemStack camo = new ItemStack(Camol.CAMO_ITEM.get());
        ItemStack off = gimbal(false);

        helper.assertTrue(SeeThrough.wanted(camo, off), "Camo item next to a switched-off Gimbal is not see-through");
        helper.assertFalse(SeeThrough.wanted(off, camo), "Switched-off Gimbal next to an off-hand camo item is see-through");
        helper.succeed();
    }

    private static ItemStack gimbal(boolean seeThrough) {
        ItemStack stack = new ItemStack(ModRegistry.GIMBAL.get());
        GimbalSettings d = GimbalSettings.DEFAULT;
        stack.set(ModRegistry.SETTINGS, new GimbalSettings(d.pasteType(), d.skippedStates(), seeThrough, d.direction(),
                d.shape(), d.limit(), d.replaceBlock(), d.preview()));
        return stack;
    }
}
