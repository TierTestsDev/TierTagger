package com.tiertests.tiertagger.gui;

/** Colors shared by the screens and the lookup popup (ARGB). */
public final class Theme {
    public static final int ACCENT = 0xFF6C5CE7;
    public static final int ACCENT_DIM = 0xFF3D3570;
    public static final int ERROR = 0xFFE74C3C;
    public static final int PANEL = 0xE01A1A2E;
    public static final int PANEL_LIGHT = 0xCC252540;
    public static final int PILL = 0xCC2A2A4A;
    public static final int HEADER = 0x40000000;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF888899;
    public static final int TEXT_MUTED = 0xFF555566;
    public static final int GOLD = 0xFFE8C840;
    public static final int REGION = 0xFF9999FF;
    public static final int NAME = 0xFF55FF55;

    private Theme() {
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    /** Mixes {@code amount} of {@code accent} into {@code base}, keeping the base alpha. */
    public static int blend(int base, int accent, float amount) {
        int r = mix(base >> 16 & 0xFF, accent >> 16 & 0xFF, amount);
        int g = mix(base >> 8 & 0xFF, accent >> 8 & 0xFF, amount);
        int b = mix(base & 0xFF, accent & 0xFF, amount);
        return base & 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int mix(int a, int b, float amount) {
        return (int) (a + (b - a) * amount);
    }
}
