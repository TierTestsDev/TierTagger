package com.tiertests.tiertagger.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable snapshot of a player on one tier list. Rankings are ordered best first. */
public final class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private final Map<GameMode, Ranking> rankings;
    private final Region region;
    private final Badge badge;
    private final int rank;
    private final double points;
    private final String accountId;

    private PlayerProfile(Builder builder) {
        this.uuid = builder.uuid;
        this.name = builder.name;
        this.rankings = sorted(builder.rankings);
        this.region = builder.region;
        this.badge = builder.badge;
        this.rank = builder.rank;
        this.points = builder.points;
        this.accountId = builder.accountId;
    }

    public static Builder builder(UUID uuid) {
        return new Builder(uuid);
    }

    public UUID uuid() {
        return uuid;
    }

    /** Name as the list spells it, or null. */
    public String name() {
        return name;
    }

    public Map<GameMode, Ranking> rankings() {
        return rankings;
    }

    public Ranking ranking(GameMode mode) {
        return mode == null ? null : rankings.get(mode);
    }

    /** First ranking whose gamemode is the same kind as {@code mode}, even when the ids differ. */
    public Map.Entry<GameMode, Ranking> rankingOfKind(GameMode mode) {
        if (mode == null) return null;
        Ranking exact = rankings.get(mode);
        if (exact != null) return new java.util.AbstractMap.SimpleImmutableEntry<GameMode, Ranking>(mode, exact);
        for (Map.Entry<GameMode, Ranking> entry : rankings.entrySet()) {
            if (entry.getKey().isSameKind(mode)) return entry;
        }
        return null;
    }

    public Map.Entry<GameMode, Ranking> best() {
        return rankings.isEmpty() ? null : rankings.entrySet().iterator().next();
    }

    public Region region() {
        return region;
    }

    public Badge badge() {
        return badge;
    }

    /** Overall leaderboard position, or 0 when the list does not report one. */
    public int rank() {
        return rank;
    }

    public double points() {
        return points;
    }

    /** List-specific account id needed for follow-up calls (the Discord id on Tier Tests). */
    public String accountId() {
        return accountId;
    }

    public boolean isEmpty() {
        return rankings.isEmpty();
    }

    public Builder toBuilder() {
        Builder builder = new Builder(uuid);
        builder.name = name;
        builder.rankings.putAll(rankings);
        builder.region = region;
        builder.badge = badge;
        builder.rank = rank;
        builder.points = points;
        builder.accountId = accountId;
        return builder;
    }

    private static Map<GameMode, Ranking> sorted(Map<GameMode, Ranking> source) {
        List<Map.Entry<GameMode, Ranking>> entries = new ArrayList<Map.Entry<GameMode, Ranking>>(source.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<GameMode, Ranking>>() {
            @Override
            public int compare(Map.Entry<GameMode, Ranking> a, Map.Entry<GameMode, Ranking> b) {
                int byRanking = a.getValue().compareTo(b.getValue());
                return byRanking != 0 ? byRanking : a.getKey().title().compareToIgnoreCase(b.getKey().title());
            }
        });
        Map<GameMode, Ranking> map = new LinkedHashMap<GameMode, Ranking>();
        for (Map.Entry<GameMode, Ranking> entry : entries) map.put(entry.getKey(), entry.getValue());
        return Collections.unmodifiableMap(map);
    }

    public static final class Builder {
        private final UUID uuid;
        private final Map<GameMode, Ranking> rankings = new LinkedHashMap<GameMode, Ranking>();
        private String name;
        private Region region;
        private Badge badge;
        private int rank;
        private double points;
        private String accountId;

        private Builder(UUID uuid) {
            this.uuid = uuid;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ranking(GameMode mode, Ranking ranking) {
            if (mode != null && ranking != null) rankings.put(mode, ranking);
            return this;
        }

        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        public Builder badge(Badge badge) {
            this.badge = badge;
            return this;
        }

        public Builder rank(int rank) {
            this.rank = Math.max(0, rank);
            return this;
        }

        public Builder points(double points) {
            this.points = points;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public PlayerProfile build() {
            return new PlayerProfile(this);
        }
    }
}
