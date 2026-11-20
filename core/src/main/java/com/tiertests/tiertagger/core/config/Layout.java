package com.tiertests.tiertagger.core.config;

/** Where tags go relative to the name. */
public enum Layout {
    PREFIX("Before Name"),
    SUFFIX("After Name"),
    /** Primary tag before the name, secondary tag after it. */
    SPLIT("Both Sides");

    private final String label;

    Layout(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
