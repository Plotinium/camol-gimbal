package plotinium.camolgimbal.client;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.item.MenuHook;

/** Client registrations on the mod bus: key mappings, the label layer, the menu opener and the model cache. */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GimbalKeys.MENU);
        event.register(GimbalKeys.SERIES);
    }

    /** Shift and right click: the menu stays open. The menu key opens it from {@link GimbalInput}. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuHook.set((hand, hit) -> GimbalMenuScreen.open(hit, false));
    }

    /** Models can change with the resource packs. */
    @SubscribeEvent
    public static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> BlockLooks.clear());
    }

    @SubscribeEvent
    public static void onGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, CamolGimbal.id("camo_label"), CamoLabel::render);
    }
}
