package plotinium.camolgimbal.data;

import net.minecraft.util.StringRepresentable;

/** Where the direction states of a pasted camouflage come from (D-19). */
public enum DirectionMode implements StringRepresentable {
    AS_COPIED("as_copied"),
    FOLLOW_LOOK("follow_look"),
    FOLLOW_FACE("follow_face");

    public static final StringRepresentable.EnumCodec<DirectionMode> CODEC =
            StringRepresentable.fromEnum(DirectionMode::values);

    private final String name;

    DirectionMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
