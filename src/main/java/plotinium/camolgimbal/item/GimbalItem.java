package plotinium.camolgimbal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.registry.ModRegistry;

public class GimbalItem extends Item {
    public GimbalItem(Properties properties) {
        super(properties);
    }

    public static GimbalSettings settings(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.SETTINGS, GimbalSettings.DEFAULT);
    }

    /** The Gimbal never breaks a block. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }
}
