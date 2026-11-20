package com.tiertests.tiertagger.core.text;

/**
 * What goes around a player's name. Both parts already include their " | " separator,
 * so a platform only has to put {@link #prefix()} before the name and {@link #suffix()} after it.
 */
public final class Tag {
    private final Text prefix;
    private final Text suffix;

    public Tag(Text prefix, Text suffix) {
        this.prefix = prefix == null ? Text.EMPTY : prefix;
        this.suffix = suffix == null ? Text.EMPTY : suffix;
    }

    public Text prefix() {
        return prefix;
    }

    public Text suffix() {
        return suffix;
    }

    public boolean isEmpty() {
        return prefix.isEmpty() && suffix.isEmpty();
    }

    /** The whole tag around {@code name} as a section-sign formatted string. */
    public String decorateLegacy(String name) {
        return prefix.toLegacyString() + name + suffix.toLegacyString();
    }
}
