package com.tiertests.tiertagger.core.config;

/** Which ranking a tag picks for a player. */
public enum DisplayMode {
    SELECTED_OR_BEST("Selected, else best"),
    BEST("Best tier"),
    SELECTED("Selected only"),
    RANK("Overall rank"),
    SELECTED_ANY_LIST("Selected, any list"),
    BEST_ANY_LIST("Best, any list");

    private final String label;

    DisplayMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean usesOtherLists() {
        return this == SELECTED_ANY_LIST || this == BEST_ANY_LIST;
    }
}
