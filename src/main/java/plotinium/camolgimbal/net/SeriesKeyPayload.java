package plotinium.camolgimbal.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import plotinium.camolgimbal.CamolGimbal;

/** The series key was pressed ({@code held} true) or released. */
public record SeriesKeyPayload(boolean held) implements CustomPacketPayload {
    public static final Type<SeriesKeyPayload> TYPE = new Type<>(CamolGimbal.id("series_key"));
    public static final StreamCodec<ByteBuf, SeriesKeyPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(SeriesKeyPayload::new, SeriesKeyPayload::held);

    @Override
    public Type<SeriesKeyPayload> type() {
        return TYPE;
    }
}
