package com.tiertests.tiertagger.core.model;

/** A player's tier in one gamemode, with the best tier they ever held when the list reports it. */
public final class Ranking implements Comparable<Ranking> {
    private final Tier tier;
    private final Tier peak;
    private final boolean retired;

    public Ranking(Tier tier, Tier peak, boolean retired) {
        if (tier == null) throw new IllegalArgumentException("tier");
        this.tier = tier;
        this.peak = peak;
        this.retired = retired;
    }

    public Tier tier() {
        return tier;
    }

    /** Peak tier, or null when unknown. */
    public Tier peak() {
        return peak;
    }

    public boolean retired() {
        return retired;
    }

    /** True when the peak is better than the current tier, so it is worth showing. */
    public boolean hasHigherPeak() {
        return peak != null && peak.isBetterThan(tier);
    }

    public Ranking withPeak(Tier newPeak) {
        return new Ranking(tier, newPeak, retired);
    }

    /** Better tiers first; active rankings before retired ones of the same tier. */
    @Override
    public int compareTo(Ranking other) {
        int byTier = tier.compareTo(other.tier);
        if (byTier != 0) return byTier;
        return Boolean.compare(retired, other.retired);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ranking)) return false;
        Ranking other = (Ranking) o;
        return tier == other.tier && peak == other.peak && retired == other.retired;
    }

    @Override
    public int hashCode() {
        return (tier.hashCode() * 31 + (peak == null ? 0 : peak.hashCode())) * 31 + (retired ? 1 : 0);
    }

    @Override
    public String toString() {
        return (retired ? "R" : "") + tier.code() + (peak != null ? " (peak " + peak.code() + ")" : "");
    }
}
