package plotinium.camolgimbal.gametest;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoHelper;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import plotinium.camolgimbal.CamolGimbal;

@GameTestHolder(CamolGimbal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SetupTests {
    private static final String EMPTY = "empty8";

    private SetupTests() {
    }

    /** Camol is loaded next to the addon and stores a camouflage where we ask it to. */
    @GameTest(template = EMPTY)
    public static void camolStoresACamo(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.STONE);
        BlockPos pos = helper.absolutePos(relative);

        CamoHelper.setCamo(helper.getLevel(), Map.of(pos, new CamoPosition("normal", Blocks.OAK_PLANKS.defaultBlockState())));

        CamoPosition stored = helper.getLevel().getChunkAt(pos).getData(Camol.CAMO_BLOCK_MAP).get(String.valueOf(pos.asLong()));
        helper.assertTrue(stored != null && stored.state().is(Blocks.OAK_PLANKS), "Camol did not store the camo");
        helper.succeed();
    }
}
