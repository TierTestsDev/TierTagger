package com.tiertests.tiertagger.core.config;

public enum IconStyle {
    /** Pixel-art gamemode icons, with symbols for gamemodes that have none. */
    PIXEL("Pixel"),
    /** Colored unicode symbols. */
    CLASSIC("Classic"),
    NONE("Off");

    private final String label;

    IconStyle(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
