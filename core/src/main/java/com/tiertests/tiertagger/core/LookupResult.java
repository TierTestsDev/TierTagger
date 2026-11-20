package com.tiertests.tiertagger.core;

import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.source.TierList;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/** A player's profiles across tier lists, in list order. Lists without data are left out. */
public final class LookupResult {
    private final UUID uuid;
    private final String name;
    private final Map<TierList, PlayerProfile> profiles;

    LookupResult(UUID uuid, String name, Map<TierList, PlayerProfile> profiles) {
        this.uuid = uuid;
        this.name = name;
        this.profiles = Collections.unmodifiableMap(profiles);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public Map<TierList, PlayerProfile> profiles() {
        return profiles;
    }

    public boolean isEmpty() {
        return profiles.isEmpty();
    }

    /** First list with data, which is the preferred one when the caller put it first. */
    public Map.Entry<TierList, PlayerProfile> first() {
        return profiles.isEmpty() ? null : profiles.entrySet().iterator().next();
    }
}
