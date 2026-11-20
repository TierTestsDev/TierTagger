package com.tiertests.tiertagger.core.model;

public enum TierFormat {
    /** HT1, LT3, MT2. */
    CODE("HT1 / LT5"),
    /** S, A+, F. Tiers without a letter grade fall back to their code. */
    LETTER("S / F");

    private final String label;

    TierFormat(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
