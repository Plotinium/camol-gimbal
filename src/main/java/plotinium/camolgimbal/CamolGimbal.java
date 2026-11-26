package plotinium.camolgimbal;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import plotinium.camolgimbal.net.ModNetwork;
import plotinium.camolgimbal.registry.ModRegistry;

@Mod(CamolGimbal.MOD_ID)
public final class CamolGimbal {
    public static final String MOD_ID = "camolgimbal";

    public CamolGimbal(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModRegistry.register(modBus);
        modBus.addListener(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
