package com.tiertests.tiertagger.core.model;

import java.util.Locale;

/** The sixteen Minecraft chat colors and helpers to convert between them and RGB. */
public final class Colors {
    private static final int[] LEGACY = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };
    private static final String[] NAMES = {
            "BLACK", "DARK_BLUE", "DARK_GREEN", "DARK_AQUA", "DARK_RED", "DARK_PURPLE", "GOLD", "GRAY",
            "DARK_GRAY", "BLUE", "GREEN", "AQUA", "RED", "LIGHT_PURPLE", "YELLOW", "WHITE"
    };

    public static final int WHITE = 0xFFFFFF;
    public static final int GRAY = 0xAAAAAA;
    public static final int DARK_GRAY = 0x555555;

    private Colors() {
    }

    /** "#RRGGBB" or "RRGGBB" to RGB, or -1. */
    public static int parseHex(String hex) {
        if (hex == null) return -1;
        String value = hex.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.length() != 6) return -1;
        try {
            return Integer.parseInt(value, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Chat color name such as "dark_red" to RGB, or -1. */
    public static int byName(String name) {
        if (name == null) return -1;
        String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(key)) return LEGACY[i];
        }
        return -1;
    }

    /** Legacy code such as "7", "&7" or "§7" to RGB, or -1. */
    public static int byLegacyCode(String code) {
        if (code == null) return -1;
        String value = code.trim();
        if (value.isEmpty()) return -1;
        int index = Character.digit(value.charAt(value.length() - 1), 16);
        return index < 0 ? -1 : LEGACY[index];
    }

    /** Lightens colors too dark to read on the mod's dark panels, keeping their hue. */
    public static int readable(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        if (luminance >= 0.35) return rgb;
        double mix = 0.45;
        return (int) (r + (255 - r) * mix) << 16 | (int) (g + (255 - g) * mix) << 8 | (int) (b + (255 - b) * mix);
    }

    /** Closest of the sixteen chat colors as its code character (0-9, a-f). */
    public static char nearestLegacyCode(int rgb) {
        int best = 15;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < LEGACY.length; i++) {
            int dr = (LEGACY[i] >> 16 & 0xFF) - (rgb >> 16 & 0xFF);
            int dg = (LEGACY[i] >> 8 & 0xFF) - (rgb >> 8 & 0xFF);
            int db = (LEGACY[i] & 0xFF) - (rgb & 0xFF);
            long distance = 2L * dr * dr + 4L * dg * dg + 3L * db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return Character.forDigit(best, 16);
    }
}
