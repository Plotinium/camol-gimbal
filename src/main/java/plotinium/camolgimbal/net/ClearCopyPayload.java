package plotinium.camolgimbal.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import plotinium.camolgimbal.CamolGimbal;

/** The clear button of the menu. */
public record ClearCopyPayload() implements CustomPacketPayload {
    public static final ClearCopyPayload INSTANCE = new ClearCopyPayload();
    public static final Type<ClearCopyPayload> TYPE = new Type<>(CamolGimbal.id("clear_copy"));
    public static final StreamCodec<ByteBuf, ClearCopyPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<ClearCopyPayload> type() {
        return TYPE;
    }
}
