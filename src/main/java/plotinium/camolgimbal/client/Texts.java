package plotinium.camolgimbal.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

final class Texts {
    private static final String ELLIPSIS = "...";
    private static final String SEPARATOR = " · ";

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

    /** The text on at most {@code maxLines} lines broken between words; what does not fit ends in dots. */
    static List<Component> wrap(Font font, Component text, int width, int maxLines) {
        List<Component> lines = new ArrayList<>();
        for (FormattedText line : font.getSplitter().splitLines(text, width, text.getStyle())) {
            lines.add(Component.literal(line.getString()));
        }
        return cut(font, lines, width, maxLines);
    }

    /**
     * The entries on one line separated by middle dots, continued on further lines without splitting an entry. An
     * entry wider than a line, and whatever follows the last line, ends in dots.
     */
    static List<Component> joined(Font font, List<Component> entries, int width, int maxLines) {
        List<Component> lines = new ArrayList<>();
        MutableComponent line = null;
        for (Component entry : entries) {
            if (line != null && font.width(line) + font.width(SEPARATOR) + font.width(entry) <= width) {
                line.append(SEPARATOR).append(entry);
                continue;
            }
            if (line != null) {
                lines.add(line);
            }
            line = Component.empty().append(fit(font, entry, width));
        }
        if (line != null) {
            lines.add(line);
        }
        return cut(font, lines, width, maxLines);
    }

    private static List<Component> cut(Font font, List<Component> lines, int width, int maxLines) {
        if (lines.size() <= maxLines) {
            return lines;
        }
        List<Component> kept = new ArrayList<>(lines.subList(0, Math.max(0, maxLines)));
        if (!kept.isEmpty()) {
            int last = kept.size() - 1;
            String start = font.substrByWidth(kept.get(last), width - font.width(ELLIPSIS)).getString();
            kept.set(last, Component.literal(start + ELLIPSIS));
        }
        return kept;
    }
}
