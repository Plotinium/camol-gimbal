package plotinium.camolgimbal.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import plotinium.camolgimbal.CamolGimbal;

/** Left click on a block with the Gimbal in the main hand: copy, or remove with {@code remove} (Shift held). */
public record LeftClickPayload(BlockPos pos, Direction face, boolean remove) implements CustomPacketPayload {
    public static final Type<LeftClickPayload> TYPE = new Type<>(CamolGimbal.id("left_click"));
    public static final StreamCodec<ByteBuf, LeftClickPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LeftClickPayload::pos,
            Direction.STREAM_CODEC, LeftClickPayload::face,
            ByteBufCodecs.BOOL, LeftClickPayload::remove,
            LeftClickPayload::new);

    @Override
    public Type<LeftClickPayload> type() {
        return TYPE;
    }
}
