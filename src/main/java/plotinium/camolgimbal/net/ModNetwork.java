package plotinium.camolgimbal.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import plotinium.camolgimbal.server.GimbalActions;
import plotinium.camolgimbal.server.SeriesKeys;

/**
 * Client to server packets. Handlers run on the server thread (NeoForge's default); each one goes through
 * {@link GimbalActions}, which checks the held Gimbal, reach, loaded chunk and permission.
 */
public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(LeftClickPayload.TYPE, LeftClickPayload.STREAM_CODEC, ModNetwork::onLeftClick);
        registrar.playToServer(SeriesKeyPayload.TYPE, SeriesKeyPayload.STREAM_CODEC, ModNetwork::onSeriesKey);
        registrar.playToServer(EditCamoPayload.TYPE, EditCamoPayload.STREAM_CODEC, ModNetwork::onEditCamo);
        registrar.playToServer(EditCopyPayload.TYPE, EditCopyPayload.STREAM_CODEC, ModNetwork::onEditCopy);
        registrar.playToServer(ClearCopyPayload.TYPE, ClearCopyPayload.STREAM_CODEC, ModNetwork::onClearCopy);
        registrar.playToServer(SettingsPayload.TYPE, SettingsPayload.STREAM_CODEC, ModNetwork::onSettings);
    }

    private static void onLeftClick(LeftClickPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (payload.remove()) {
                GimbalActions.remove(player, payload.pos(), payload.face());
            } else {
                GimbalActions.copy(player, payload.pos(), payload.face());
            }
        }
    }

    private static void onSeriesKey(SeriesKeyPayload payload, IPayloadContext context) {
        SeriesKeys.set(context.player(), payload.held());
    }

    private static void onEditCamo(EditCamoPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GimbalActions.editCamo(player, payload.pos(), payload.state());
        }
    }

    private static void onEditCopy(EditCopyPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GimbalActions.editCopy(player, payload.state());
        }
    }

    private static void onClearCopy(ClearCopyPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GimbalActions.clearCopy(player);
        }
    }

    private static void onSettings(SettingsPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            GimbalActions.changeSettings(player, payload.settings());
        }
    }
}
