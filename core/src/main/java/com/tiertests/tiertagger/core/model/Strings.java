package com.tiertests.tiertagger.core.model;

import java.util.Locale;

public final class Strings {
    private Strings() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** "neth_pot" and "early-game" become "Neth Pot" and "Early Game"; short ids like "uhc" become "UHC". */
    public static String titleCase(String id) {
        if (isBlank(id)) return "";
        String[] words = id.trim().replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (out.length() > 0) out.append(' ');
            if (word.length() <= 3 && !word.equalsIgnoreCase("pot") && !word.equalsIgnoreCase("bed")
                    && !word.equalsIgnoreCase("bow") && !word.equalsIgnoreCase("axe")) {
                out.append(word.toUpperCase(Locale.ROOT));
            } else {
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return out.toString();
    }

    public static String undashed(java.util.UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static java.util.UUID parseUuid(String raw) {
        if (raw == null) return null;
        String hex = raw.replace("-", "");
        if (hex.length() != 32) return null;
        try {
            return new java.util.UUID(Long.parseUnsignedLong(hex.substring(0, 16), 16),
                    Long.parseUnsignedLong(hex.substring(16), 16));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Plain Minecraft usernames: 3 to 16 letters, digits or underscores. */
    public static boolean isValidUsername(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
            if (!ok) return false;
        }
        return true;
    }
}
