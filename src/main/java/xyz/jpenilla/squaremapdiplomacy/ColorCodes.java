package xyz.jpenilla.squaremapdiplomacy;

import java.util.Map;

/** Converts Minecraft colour codes (§ or &amp;) to HTML for squaremap popups. */
final class ColorCodes {
    private static final Map<Character, String> COLORS = Map.ofEntries(
        Map.entry('0', "#000000"), Map.entry('1', "#0000AA"), Map.entry('2', "#00AA00"), Map.entry('3', "#00AAAA"),
        Map.entry('4', "#AA0000"), Map.entry('5', "#AA00AA"), Map.entry('6', "#FFAA00"), Map.entry('7', "#AAAAAA"),
        Map.entry('8', "#555555"), Map.entry('9', "#5555FF"), Map.entry('a', "#55FF55"), Map.entry('b', "#55FFFF"),
        Map.entry('c', "#FF5555"), Map.entry('d', "#FF55FF"), Map.entry('e', "#FFFF55"), Map.entry('f', "#FFFFFF")
    );

    private ColorCodes() {
    }

    static String toHtml(final String input) {
        final StringBuilder out = new StringBuilder();
        String color = null;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;
        final StringBuilder text = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if ((c == '\u00A7' || c == '&') && i + 1 < input.length()) {
                String newColor = null;
                int skip = 1;
                final char next = Character.toLowerCase(input.charAt(i + 1));
                if (next == '#' && i + 7 < input.length() + 0 && input.substring(i + 2, Math.min(input.length(), i + 8)).matches("[0-9a-fA-F]{6}")) {
                    newColor = "#" + input.substring(i + 2, i + 8);
                    skip = 7;
                } else if (COLORS.containsKey(next)) {
                    newColor = COLORS.get(next);
                } else if ("lonmrk".indexOf(next) < 0) {
                    out.append(escape(String.valueOf(c)));
                    continue;
                }
                flush(out, text, color, bold, italic, underline, strike);
                if (newColor != null) {
                    color = newColor;
                    bold = italic = underline = strike = false;
                } else {
                    switch (next) {
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'n' -> underline = true;
                        case 'm' -> strike = true;
                        case 'r' -> {
                            color = null;
                            bold = italic = underline = strike = false;
                        }
                        default -> { }
                    }
                }
                i += skip;
                continue;
            }
            text.append(c);
        }
        flush(out, text, color, bold, italic, underline, strike);
        return out.toString();
    }

    private static void flush(final StringBuilder out, final StringBuilder text, final String color,
                              final boolean bold, final boolean italic, final boolean underline, final boolean strike) {
        if (text.isEmpty()) {
            return;
        }
        final StringBuilder style = new StringBuilder();
        if (color != null) {
            style.append("color:").append(color.equalsIgnoreCase("#FFFFFF") ? "#000000" : color).append(';');
        }
        if (bold) {
            style.append("font-weight:bold;");
        }
        if (italic) {
            style.append("font-style:italic;");
        }
        if (underline || strike) {
            style.append("text-decoration:").append(underline ? "underline " : "").append(strike ? "line-through" : "").append(';');
        }
        final String escaped = escape(text.toString());
        out.append(style.isEmpty() ? escaped : "<span style=\"" + style + "\">" + escaped + "</span>");
        text.setLength(0);
    }

    private static String escape(final String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
