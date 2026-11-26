package plotinium.camolgimbal.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import plotinium.camolgimbal.CamolGimbal;
import plotinium.camolgimbal.data.GimbalSettings;

/** The switches of the menu, all of them; the server clamps them when they are decoded. */
public record SettingsPayload(GimbalSettings settings) implements CustomPacketPayload {
    public static final Type<SettingsPayload> TYPE = new Type<>(CamolGimbal.id("settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> STREAM_CODEC =
            GimbalSettings.STREAM_CODEC.map(SettingsPayload::new, SettingsPayload::settings);

    @Override
    public Type<SettingsPayload> type() {
        return TYPE;
    }
}
