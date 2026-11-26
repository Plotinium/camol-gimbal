package plotinium.camolgimbal.data;

import net.minecraft.util.StringRepresentable;

/** Which blocks a series covers (D-11). */
public enum SeriesShape implements StringRepresentable {
    CONNECTED("connected"),
    FACE("face"),
    ROW("row"),
    COLUMN("column");

    public static final StringRepresentable.EnumCodec<SeriesShape> CODEC =
            StringRepresentable.fromEnum(SeriesShape::values);

    private final String name;

    SeriesShape(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
