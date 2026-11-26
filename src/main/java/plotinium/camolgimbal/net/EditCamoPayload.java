package plotinium.camolgimbal.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import plotinium.camolgimbal.CamolGimbal;

/** The menu changed the camouflage at {@code pos} to {@code state}, which must be a state of the same block. */
public record EditCamoPayload(BlockPos pos, BlockState state) implements CustomPacketPayload {
    public static final Type<EditCamoPayload> TYPE = new Type<>(CamolGimbal.id("edit_camo"));
    public static final StreamCodec<ByteBuf, EditCamoPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EditCamoPayload::pos,
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), EditCamoPayload::state,
            EditCamoPayload::new);

    @Override
    public Type<EditCamoPayload> type() {
        return TYPE;
    }
}
