package plotinium.camolgimbal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.net.LeftClickPayload;
import plotinium.camolgimbal.net.SeriesKeyPayload;
import plotinium.camolgimbal.registry.ModRegistry;

/**
 * Left click, series key and menu key on the client (D-16).
 *
 * <p>Left click: while the Gimbal is in the main hand and the crosshair is on a block, every attack input is
 * cancelled: NeoForge fires it once from {@code startAttack} on the press and again from {@code continueAttack} on
 * every tick the button is held, so the block is never broken. The packet goes only for a real press: a raw press of
 * the attack binding, seen with no screen open, arms one packet, which the next block attack input of that tick
 * spends. Holding the button sends nothing more, and neither does closing a screen with the button held, where
 * {@code MouseHandler.grabMouse} sets the binding down again without a press. Entities and the air are left to
 * vanilla.
 *
 * <p>Menu key: only a real press, seen with no screen open, opens the menu. A key held after the menu closed keeps
 * repeating, and each repeat counts as a click for Minecraft.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, value = Dist.CLIENT)
public final class GimbalInput {
    private static boolean pressArmed;
    private static boolean menuArmed;
    private static boolean seriesSent;

    private GimbalInput() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            if (Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) {
                pressArmed = true;
            }
            if (GimbalKeys.MENU.matchesMouse(event.getButton())) {
                menuArmed = true;
            }
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            if (Minecraft.getInstance().options.keyAttack.matches(event.getKey(), event.getScanCode())) {
                pressArmed = true;
            }
            if (GimbalKeys.MENU.matches(event.getKey(), event.getScanCode())) {
                menuArmed = true;
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isAttack() || player == null || !player.getMainHandItem().is(ModRegistry.GIMBAL)
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level == null || minecraft.level.getBlockState(hit.getBlockPos()).isAir()) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(pressArmed);
        if (pressArmed) {
            pressArmed = false;
            PacketDistributor.sendToServer(
                    new LeftClickPayload(hit.getBlockPos(), hit.getDirection(), player.isShiftKeyDown()));
        }
    }

    /** Before the tick's key handling, so the series state reaches the server ahead of this tick's use packet. */
    @SubscribeEvent
    public static void onTickStart(ClientTickEvent.Pre event) {
        if (Minecraft.getInstance().getConnection() == null) {
            seriesSent = false;
            return;
        }
        boolean held = GimbalKeys.SERIES.isDown();
        if (held != seriesSent) {
            seriesSent = held;
            PacketDistributor.sendToServer(new SeriesKeyPayload(held));
        }
    }

    @SubscribeEvent
    public static void onTickEnd(ClientTickEvent.Post event) {
        pressArmed = false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean pressed = menuArmed;
        menuArmed = false;
        while (GimbalKeys.MENU.consumeClick()) {
            if (pressed && player != null) {
                // Does nothing without a Gimbal in hand or with a screen already open.
                GimbalMenuScreen.open(minecraft.hitResult instanceof BlockHitResult hit
                        && hit.getType() == HitResult.Type.BLOCK ? hit : null, true);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        seriesSent = false;
        pressArmed = false;
        menuArmed = false;
    }

    /** The main hand if it holds a Gimbal, else the off hand, else none. */
    public static InteractionHand gimbalHand(LocalPlayer player) {
        if (player.getMainHandItem().is(ModRegistry.GIMBAL)) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack off = player.getOffhandItem();
        return off.is(ModRegistry.GIMBAL) ? InteractionHand.OFF_HAND : null;
    }
}
