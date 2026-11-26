package plotinium.camolgimbal;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;

@Mod(CamolGimbal.MOD_ID)
public final class CamolGimbal {
    public static final String MOD_ID = "camolgimbal";

    public CamolGimbal() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
