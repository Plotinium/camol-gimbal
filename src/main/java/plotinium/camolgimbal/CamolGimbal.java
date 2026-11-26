package plotinium.camolgimbal;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import plotinium.camolgimbal.registry.ModRegistry;

@Mod(CamolGimbal.MOD_ID)
public final class CamolGimbal {
    public static final String MOD_ID = "camolgimbal";

    public CamolGimbal(IEventBus modBus) {
        ModRegistry.register(modBus);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
