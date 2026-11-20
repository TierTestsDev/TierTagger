package com.tiertests.tiertagger.core.model;

import java.util.Locale;

/** Short region code such as NA, EU or AS. */
public final class Region {
    private final String code;

    private Region(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Normalises the region strings the different APIs return. Unknown or global values give null. */
    public static Region parse(String raw) {
        if (Strings.isBlank(raw)) return null;
        String value = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (value.equals("NORTH_AMERICA") || value.equals("AMERICA") || value.equals("US")) return new Region("NA");
        if (value.equals("EUROPE")) return new Region("EU");
        if (value.equals("ASIA") || value.equals("APAC") || value.equals("ASIA_PACIFIC")) return new Region("AS");
        if (value.equals("OCEANIA") || value.equals("OC") || value.equals("AUSTRALIA")) return new Region("AU");
        if (value.equals("SOUTH_AMERICA")) return new Region("SA");
        if (value.equals("AFRICA")) return new Region("AF");
        if (value.equals("MIDDLE_EAST")) return new Region("ME");
        if (value.length() >= 2 && value.length() <= 3 && isLetters(value)
                && !value.equals("ANY") && !value.equals("ALL")) {
            return new Region(value);
        }
        return null;
    }

    private static boolean isLetters(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLetter(value.charAt(i))) return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Region && code.equals(((Region) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
