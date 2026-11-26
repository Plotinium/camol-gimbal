package plotinium.camolgimbal.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The switches of a Gimbal (D-19). Values come from the item and from the client, so the constructor keeps them in
 * their allowed range. {@code skippedStates} holds the names of the states that are not pasted.
 */
public record GimbalSettings(boolean pasteType, Set<String> skippedStates, boolean seeThrough, DirectionMode direction,
        SeriesShape shape, int limit, boolean replaceBlock, boolean preview) {
    public static final List<Integer> LIMITS = List.of(8, 16, 32, 64);
    public static final int MAX_SKIPPED = 64;
    private static final int MAX_NAME_LENGTH = 64;
    private static final int DEFAULT_LIMIT = 64;

    public static final GimbalSettings DEFAULT = new GimbalSettings(true, Set.of(), true, DirectionMode.AS_COPIED,
            SeriesShape.CONNECTED, DEFAULT_LIMIT, false, true);

    public static final Codec<GimbalSettings> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                    Codec.BOOL.optionalFieldOf("paste_type", true).forGetter(GimbalSettings::pasteType),
                    Codec.STRING.listOf().optionalFieldOf("skipped_states", List.of())
                            .forGetter(settings -> settings.skippedStates().stream().sorted().toList()),
                    Codec.BOOL.optionalFieldOf("see_through", true).forGetter(GimbalSettings::seeThrough),
                    DirectionMode.CODEC.optionalFieldOf("direction", DirectionMode.AS_COPIED)
                            .forGetter(GimbalSettings::direction),
                    SeriesShape.CODEC.optionalFieldOf("shape", SeriesShape.CONNECTED).forGetter(GimbalSettings::shape),
                    Codec.INT.optionalFieldOf("limit", DEFAULT_LIMIT).forGetter(GimbalSettings::limit),
                    Codec.BOOL.optionalFieldOf("replace_block", false).forGetter(GimbalSettings::replaceBlock),
                    Codec.BOOL.optionalFieldOf("preview", true).forGetter(GimbalSettings::preview))
            .apply(builder, (pasteType, skipped, seeThrough, direction, shape, limit, replaceBlock, preview) ->
                    new GimbalSettings(pasteType, Set.copyOf(skipped), seeThrough, direction, shape, limit,
                            replaceBlock, preview)));

    public static final StreamCodec<RegistryFriendlyByteBuf, GimbalSettings> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public GimbalSettings {
        skippedStates = skippedStates.stream()
                .filter(name -> name.length() <= MAX_NAME_LENGTH)
                .sorted()
                .limit(MAX_SKIPPED)
                .collect(Collectors.toUnmodifiableSet());
        limit = LIMITS.contains(limit) ? limit : DEFAULT_LIMIT;
    }
}
