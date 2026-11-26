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
 * every tick the button is held, so the block is never broken. The packet goes at the raw press of the attack binding,
 * with no screen open, for the block under the crosshair at that moment: one press, one packet. It does not wait for
 * the attack input, which vanilla holds back for up to 10 ticks after a click in the air ({@code missTime}), so a
 * click on a camo right after a missed one is not lost, and a press that began elsewhere never acts on a block the
 * crosshair slides onto. Holding the button sends nothing more, and neither does closing a screen with the button
 * held, where {@code MouseHandler.grabMouse} sets the binding down again without a press. Entities and the air are
 * left to vanilla.
 *
 * <p>Menu key: only a real press, seen with no screen open, opens the menu. A key held after the menu closed keeps
 * repeating, and each repeat counts as a click for Minecraft.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, value = Dist.CLIENT)
public final class GimbalInput {
    private static boolean menuArmed;
    private static boolean seriesSent;

    private GimbalInput() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            if (Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) {
                attackPressed();
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
                attackPressed();
            }
            if (GimbalKeys.MENU.matches(event.getKey(), event.getScanCode())) {
                menuArmed = true;
            }
        }
    }

    /** A real press of the attack binding: copy or remove the block under the crosshair, if the Gimbal acts on it. */
    private static void attackPressed() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        BlockHitResult hit = gimbalTarget(minecraft);
        if (hit == null || minecraft.getOverlay() != null || player.isSpectator() || player.isUsingItem()
                || player.isHandsBusy()) {
            return;
        }
        PacketDistributor.sendToServer(new LeftClickPayload(hit.getBlockPos(), hit.getDirection(), player.isShiftKeyDown()));
        player.swing(InteractionHand.MAIN_HAND);
    }

    /** The block under the crosshair while the local player holds the Gimbal in the main hand, or null. */
    private static BlockHitResult gimbalTarget(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !player.getMainHandItem().is(ModRegistry.GIMBAL)
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || minecraft.level.getBlockState(hit.getBlockPos()).isAir()) {
            return null;
        }
        return hit;
    }

    @SubscribeEvent
    public static void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && gimbalTarget(Minecraft.getInstance()) != null) {
            event.setCanceled(true);
            event.setSwingHand(false);
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
