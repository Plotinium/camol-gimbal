package plotinium.camolgimbal.rules;

import cy.jdkdigital.camol.Camol;
import cy.jdkdigital.camol.utils.CamoPosition;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/** Where the rules read camouflages from: the chunk data on the server, Camol's client copy in the preview. */
@FunctionalInterface
public interface CamoView {
    /** The camouflage at a position, or null when there is none (Camol stores a removed one as air). */
    @Nullable
    CamoPosition camoAt(BlockPos pos);

    /**
     * Reads the chunk data of a level. It never loads a chunk and never creates the attachment: an unloaded position
     * and a chunk without data both read as no camouflage.
     */
    static CamoView of(Level level) {
        return pos -> {
            if (!level.isLoaded(pos)) {
                return null;
            }
            LevelChunk chunk = level.getChunkAt(pos);
            if (!chunk.hasData(Camol.CAMO_BLOCK_MAP)) {
                return null;
            }
            Map<String, CamoPosition> map = chunk.getData(Camol.CAMO_BLOCK_MAP);
            CamoPosition camo = map.get(String.valueOf(pos.asLong()));
            return camo == null || camo.state().isAir() ? null : camo;
        };
    }
}
