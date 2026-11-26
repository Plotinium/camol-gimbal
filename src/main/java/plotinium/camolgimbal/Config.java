package plotinium.camolgimbal;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server settings, in {@code config/camolgimbal-server.toml} of the instance or server; a copy in a world's
 * {@code serverconfig} folder overrides it for that world.
 */
public final class Config {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FOLLOW_CLICKED_FACE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        FOLLOW_CLICKED_FACE = builder
                .comment("Camol's camo items place the camouflage as placing the block on the clicked face would.",
                        "Off: Camol's own placement, which faces the player.")
                .define("followClickedFace", true);
        SPEC = builder.build();
    }

    private Config() {
    }

    /** The setting, or its default while no world has loaded the server config. */
    public static boolean followClickedFace() {
        return SPEC.isLoaded() ? FOLLOW_CLICKED_FACE.get() : true;
    }
}
