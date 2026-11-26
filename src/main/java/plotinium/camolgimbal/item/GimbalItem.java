package plotinium.camolgimbal.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import plotinium.camolgimbal.data.GimbalSettings;
import plotinium.camolgimbal.registry.ModRegistry;
import plotinium.camolgimbal.rules.Planner;
import plotinium.camolgimbal.server.GimbalActions;

public class GimbalItem extends Item {
    public GimbalItem(Properties properties) {
        super(properties);
    }

    public static GimbalSettings settings(ItemStack stack) {
        return stack.getOrDefault(ModRegistry.SETTINGS, GimbalSettings.DEFAULT);
    }

    /**
     * Right click on a block. It runs after {@code RightClickBlock} (claim mods answer there) and before the block's
     * own action, which it always replaces: applying wins over doors, levers and machines. With Shift it opens the
     * menu on the client and does nothing on the server.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide) {
                MenuHook.open(context.getHand(), new BlockHitResult(context.getClickLocation(),
                        context.getClickedFace(), context.getClickedPos(), context.isInside()));
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            GimbalActions.apply(serverPlayer, context.getHand(),
                    new Planner.Hit(context.getClickedPos(), context.getClickedFace(), context.getClickLocation()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Right click in the air: only Shift does something, the menu on the client. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            MenuHook.open(hand, null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** The Gimbal never breaks a block. */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }
}
