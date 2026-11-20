package com.tiertests.tiertagger.core.model;

import java.util.Locale;

/**
 * A tier on the shared 1 to 5 scale, best first.
 * Most lists only use high and low tiers; MCPVP also has a middle tier per level.
 */
public enum Tier {
    HT1(1, 'H', "S", 0xFFAA00),
    MT1(1, 'M', null, 0xFFAA00),
    LT1(1, 'L', "A+", 0x55FF55),
    HT2(2, 'H', "A-", 0x55FF55),
    MT2(2, 'M', null, 0x55FF55),
    LT2(2, 'L', "B+", 0x5555FF),
    HT3(3, 'H', "B-", 0x5555FF),
    MT3(3, 'M', null, 0x5555FF),
    LT3(3, 'L', "C+", 0xFFFF55),
    HT4(4, 'H', "C-", 0xFFFF55),
    MT4(4, 'M', null, 0xFFFF55),
    LT4(4, 'L', "D+", 0xFF5555),
    HT5(5, 'H', "D-", 0xFF5555),
    MT5(5, 'M', null, 0xFF5555),
    LT5(5, 'L', "F", 0xAA0000);

    private static final Tier[] VALUES = values();

    private final int level;
    private final char position;
    private final String letter;
    private final int color;

    Tier(int level, char position, String letter, int color) {
        this.level = level;
        this.position = position;
        this.letter = letter;
        this.color = color;
    }

    public int level() {
        return level;
    }

    public int color() {
        return color;
    }

    public String code() {
        return name();
    }

    public String format(TierFormat format) {
        return format == TierFormat.LETTER && letter != null ? letter : name();
    }

    public boolean isBetterThan(Tier other) {
        return other == null || ordinal() < other.ordinal();
    }

    /** Tier from the numeric form used by MCTiers-style APIs, where position 0 is high and 1 is low. */
    public static Tier of(int level, int position) {
        if (level < 1 || level > 5) return null;
        char pos = position == 0 ? 'H' : 'L';
        for (Tier tier : VALUES) {
            if (tier.level == level && tier.position == pos) return tier;
        }
        return null;
    }

    /** Parses HT1 / MT2 / LT3 codes, letter grades (S, A+, A_PLUS) and returns null for anything else. */
    public static Tier parse(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty()) return null;
        if (value.length() == 3 && value.charAt(1) == 'T') {
            for (Tier tier : VALUES) {
                if (tier.name().equals(value)) return tier;
            }
            return null;
        }
        String letter = value.replace("_PLUS", "+").replace("_MINUS", "-");
        for (Tier tier : VALUES) {
            if (letter.equals(tier.letter)) return tier;
        }
        return null;
    }
}
