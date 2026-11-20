package com.tiertests.tiertagger.core.config;

/** Which ranking a tag picks for a player. */
public enum DisplayMode {
    SELECTED_OR_BEST("Selected, Else Best"),
    BEST("Best Tier"),
    SELECTED("Selected Only"),
    RANK("Overall Rank"),
    SELECTED_ANY_LIST("Selected, Any List"),
    BEST_ANY_LIST("Best, Any List");

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
