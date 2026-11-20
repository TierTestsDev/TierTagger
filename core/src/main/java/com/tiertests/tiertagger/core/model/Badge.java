package com.tiertests.tiertagger.core.model;

/** A short symbol shown after the tier, such as a Tier Tests role badge. */
public final class Badge {
    private final String symbol;
    private final int color;

    public Badge(String symbol, int color) {
        this.symbol = symbol;
        this.color = color;
    }

    public String symbol() {
        return symbol;
    }

    public int color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Badge)) return false;
        Badge other = (Badge) o;
        return color == other.color && symbol.equals(other.symbol);
    }

    @Override
    public int hashCode() {
        return symbol.hashCode() * 31 + color;
    }
}
