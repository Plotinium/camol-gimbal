package plotinium.camolgimbal.mixin;

import cy.jdkdigital.camol.event.ClientEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import plotinium.camolgimbal.client.SeeThroughClient;

/**
 * Camol decides the see-through drawing of a section inside the renderer lambda of
 * {@code ClientEventHandler.geometryEvent}, from the main-hand item alone, and stores the answer in the lambda's only
 * boolean local. That decision is replaced by the addon's value (see {@link SeeThroughClient}).
 *
 * <p>{@code require = 0}: a Camol update can rename the synthetic method, and then Camol draws as it always did. The
 * config plugin logs one warning when the handler is not reached.
 */
@Mixin(ClientEventHandler.class)
public abstract class CamolDrawingMixin {
    @ModifyVariable(method = CamolGimbalMixinPlugin.DRAWING_METHOD, at = @At("STORE"), ordinal = 0, require = 0)
    private static boolean camolgimbal$drawSeeThrough(boolean camolDecision) {
        return SeeThroughClient.isActive();
    }
}
