package plotinium.camolgimbal.client;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;
import plotinium.camolgimbal.rules.VisibleStates;

/**
 * The states the menu and the label offer for a block (D-1), from how each block state is drawn: render shape, baked
 * model quads per render type, their tint, and the outline and collision shapes (a Shape Retaining camo collides).
 * The answer is kept per block until the resources are reloaded.
 */
public final class BlockLooks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MODEL_SEED = 42L;
    private static final Map<Block, List<Property<?>>> CACHE = new HashMap<>();

    private BlockLooks() {
    }

    /** Render thread only. */
    public static List<Property<?>> visibleStates(Block block) {
        List<Property<?>> cached = CACHE.get(block);
        if (cached == null) {
            cached = VisibleStates.of(block, BlockLooks::look);
            CACHE.put(block, cached);
        }
        return cached;
    }

    public static void clear() {
        CACHE.clear();
    }

    /**
     * Everything that can make two states look different, comparable with {@code equals}. A block that fails here
     * gets a value equal to nothing else, so its states stay offered.
     */
    private static Object look(BlockState state) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BlockColors colors = minecraft.getBlockColors();
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
            RandomSource random = RandomSource.create(MODEL_SEED);
            List<Object> parts = new ArrayList<>();
            parts.add(state.getRenderShape());
            for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                parts.add(type);
                for (Direction side : Direction.values()) {
                    random.setSeed(MODEL_SEED);
                    addQuads(parts, state, colors, model.getQuads(state, side, random, ModelData.EMPTY, type));
                }
                random.setSeed(MODEL_SEED);
                addQuads(parts, state, colors, model.getQuads(state, null, random, ModelData.EMPTY, type));
            }
            parts.add(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs());
            parts.add(state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).toAabbs());
            return parts;
        } catch (RuntimeException exception) {
            LOGGER.debug("Cannot compare how {} is drawn", state, exception);
            return new Object();
        }
    }

    private static void addQuads(List<Object> parts, BlockState state, BlockColors colors, List<BakedQuad> quads) {
        // The count keeps apart the same quads listed under different faces.
        parts.add(quads.size());
        for (BakedQuad quad : quads) {
            parts.add(IntArrayList.wrap(quad.getVertices()));
            parts.add(quad.getDirection());
            parts.add(quad.isShade());
            if (quad.isTinted()) {
                parts.add(colors.getColor(state, null, null, quad.getTintIndex()));
            }
        }
    }
}
