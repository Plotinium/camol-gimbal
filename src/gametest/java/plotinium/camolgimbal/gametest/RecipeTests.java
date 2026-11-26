package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.registry.ModRegistry;

@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeTests {
    private RecipeTests() {
    }

    /** D-15: two gold, two iron and a Camo Block in the shape of a diagonal tool. */
    @GameTest(template = "empty8")
    public static void gimbalIsCrafted(GameTestHelper helper) {
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack camo = new ItemStack(Camol.CAMO_ITEM.get());
        ItemStack none = ItemStack.EMPTY;

        ItemStack crafted = craft(helper, List.of(none, gold, camo, none, iron, gold, iron, none, none));
        helper.assertTrue(crafted.is(ModRegistry.GIMBAL.get()), "the recipe does not give a Gimbal: " + crafted);

        ItemStack solid = new ItemStack(Camol.SOLID_CAMO_ITEM.get());
        ItemStack withSolid = craft(helper, List.of(none, gold, solid, none, iron, gold, iron, none, none));
        helper.assertTrue(!withSolid.is(ModRegistry.GIMBAL.get()), "a Shape Retaining Camo Block crafts a Gimbal");
        helper.succeed();
    }

    private static ItemStack craft(GameTestHelper helper, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid.stream().map(ItemStack::copy).toList());
        return helper.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(recipe -> recipe.value().assemble(input, helper.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
