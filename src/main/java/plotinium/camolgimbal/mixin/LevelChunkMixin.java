package plotinium.camolgimbal.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plotinium.camolgimbal.server.LostCamos;

/**
 * Notes positions whose block disappeared, for {@link LostCamos}. Only a real change returns a non-null state. A
 * return-value hook allocates nothing on this hot path, unlike a callback injection.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @ModifyReturnValue(method = "setBlockState", at = @At("RETURN"))
    private BlockState camolgimbal$noteLostCamo(BlockState previous, BlockPos pos, BlockState state, boolean isMoving) {
        if (previous != null) {
            LostCamos.onBlockChanged((LevelChunk) (Object) this, pos, state);
        }
        return previous;
    }
}
