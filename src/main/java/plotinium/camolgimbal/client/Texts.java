package plotinium.camolgimbal.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

final class Texts {
    private static final String ELLIPSIS = "...";

    private Texts() {
    }

    /** The text, or its start followed by dots when it is wider than {@code width}. */
    static Component fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        FormattedText start = font.substrByWidth(text, Math.max(0, width - font.width(ELLIPSIS)));
        return Component.literal(start.getString() + ELLIPSIS);
    }
}
