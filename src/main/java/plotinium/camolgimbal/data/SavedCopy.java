package plotinium.camolgimbal.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A copied camouflage: Camol's camo type ({@link #NORMAL} or {@link #SOLID}), the state of the camo block, and the
 * horizontal direction the player faced while copying it.
 */
public record SavedCopy(String type, BlockState state, Direction look) {
    public static final String NORMAL = "normal";
    public static final String SOLID = "solid";

    public static final Codec<SavedCopy> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    Codec.STRING.fieldOf("type").forGetter(SavedCopy::type),
                    BlockState.CODEC.fieldOf("state").forGetter(SavedCopy::state),
                    Direction.CODEC.fieldOf("look").forGetter(SavedCopy::look))
            .apply(builder, SavedCopy::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SavedCopy> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SavedCopy::type,
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), SavedCopy::state,
            Direction.STREAM_CODEC, SavedCopy::look,
            SavedCopy::new);

    public SavedCopy {
        type = SOLID.equals(type) ? SOLID : NORMAL;
        if (look.getAxis().isVertical()) {
            look = Direction.NORTH;
        }
    }

    public SavedCopy withState(BlockState newState) {
        return new SavedCopy(type, newState, look);
    }
}
