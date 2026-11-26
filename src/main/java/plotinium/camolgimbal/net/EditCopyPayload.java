package plotinium.camolgimbal.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plotinium.camolgimbal.CamolGimbal;

/** The menu changed the saved copy to {@code state}, which must be a state of the copied block. */
public record EditCopyPayload(BlockState state) implements CustomPacketPayload {
    public static final Type<EditCopyPayload> TYPE = new Type<>(CamolGimbal.id("edit_copy"));
    public static final StreamCodec<ByteBuf, EditCopyPayload> STREAM_CODEC =
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).map(EditCopyPayload::new, EditCopyPayload::state);

    @Override
    public Type<EditCopyPayload> type() {
        return TYPE;
    }
}
