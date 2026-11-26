package plotinium.camolgimbal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cy.jdkdigital.camol.common.item.CamoItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import plotinium.camolgimbal.Config;
import plotinium.camolgimbal.rules.Placement;

/**
 * A camo item follows the clicked face (D-13). Camol's {@code CamoItem.useOn} asks the block for its placement state
 * with the player's facing as the clicked face; with the server setting on, the addon asks with the face, position
 * and point really clicked, through {@link Placement}. A Camol version that passes the real click gets the same
 * answer, so the change stays harmless. {@code require = 0}: the config plugin logs one warning if it finds nothing.
 */
@Mixin(CamoItem.class)
public abstract class CamoItemMixin {
    @WrapOperation(method = CamolGimbalMixinPlugin.CAMO_ITEM_METHOD, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState camolgimbal$followClickedFace(Block block, BlockPlaceContext camolContext,
            Operation<BlockState> original, UseOnContext context) {
        if (!Config.followClickedFace()) {
            return original.call(block, camolContext);
        }
        return Placement.stateFor(context.getLevel(), context.getPlayer(), block, context.getClickedPos(),
                context.getClickedFace(), context.getClickLocation());
    }
}
