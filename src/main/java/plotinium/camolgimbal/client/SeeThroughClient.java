package plotinium.camolgimbal.client;

import com.mojang.logging.LogUtils;
import cy.jdkdigital.camol.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.item.SeeThrough;

/**
 * Keeps Camol's see-through drawing in step with what the local player holds.
 *
 * <p>Camol's {@code PlayerTickEvent.Post} handler writes {@code ClientEventHandler.shouldBeTransparent} once per
 * client player, remote players included, and section geometry is built on worker threads at any moment. So the value
 * is computed here, after every entity has ticked, and kept in a field of our own that the drawing mixin reads. The
 * copy written to Camol's flag drives Camol's rebuild of the camouflaged sections, which runs on the render thread
 * after the tick and therefore always sees this value.
 */
@EventBusSubscriber(modid = CamolGimbal.MOD_ID, value = Dist.CLIENT)
public final class SeeThroughClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean active;
    private static boolean camolFlagMissing;

    private SeeThroughClient() {
    }

    /** Read by the drawing mixin, on section build threads. */
    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            active = false;
            return;
        }
        boolean value = SeeThrough.wanted(player.getMainHandItem(), player.getOffhandItem());
        active = value;
        writeCamolFlag(value);
    }

    private static void writeCamolFlag(boolean value) {
        if (camolFlagMissing) {
            return;
        }
        try {
            ClientEventHandler.shouldBeTransparent = value;
        } catch (LinkageError error) {
            camolFlagMissing = true;
            LOGGER.warn("Camol's ClientEventHandler.shouldBeTransparent is missing; camouflaged blocks are not "
                    + "redrawn when the Gimbal is taken in hand ({})", error.toString());
        }
    }
}
