package com.tiertests.tiertagger.core.source;

import com.tiertests.tiertagger.core.Log;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.net.Http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * One tier list website. Implementations do blocking I/O and are only called from worker threads;
 * {@link #modes()} is the exception and never blocks.
 */
public abstract class TierList {
    private static final long MODES_RETRY_MS = 60_000L;

    private final String id;
    private final String displayName;
    private final char logo;
    protected final Http http;

    private volatile List<GameMode> modes = Collections.emptyList();
    private volatile boolean modesLoaded;
    private long modesRetryAt;

    protected TierList(String id, String displayName, char logo, Http http) {
        this.id = id;
        this.displayName = displayName;
        this.logo = logo;
        this.http = http;
    }

    /** Stable id used in the config file. */
    public final String id() {
        return id;
    }

    public final String displayName() {
        return displayName;
    }

    /** Logo character in the mod's icon font, or 0 when there is none. */
    public final char logo() {
        return logo;
    }

    /** Gamemodes known so far, sorted by title. Empty until {@link #loadModes()} has succeeded. */
    public final List<GameMode> modes() {
        return modes;
    }

    public final boolean modesLoaded() {
        return modesLoaded;
    }

    /** Loads the gamemode list once; failures are retried at most once a minute. Blocking. */
    public final List<GameMode> loadModes() {
        if (modesLoaded) return modes;
        synchronized (this) {
            if (modesLoaded || System.currentTimeMillis() < modesRetryAt) return modes;
            try {
                List<GameMode> loaded = new ArrayList<GameMode>(fetchModes());
                Collections.sort(loaded, new Comparator<GameMode>() {
                    @Override
                    public int compare(GameMode a, GameMode b) {
                        return a.title().compareToIgnoreCase(b.title());
                    }
                });
                modes = Collections.unmodifiableList(loaded);
                modesLoaded = !loaded.isEmpty();
            } catch (IOException | RuntimeException e) {
                Log.warn("Could not load gamemodes for " + displayName + ": " + e.getMessage());
            }
            if (!modesLoaded) modesRetryAt = System.currentTimeMillis() + MODES_RETRY_MS;
            return modes;
        }
    }

    /** The loaded gamemode with this id, or null. */
    public final GameMode findMode(String modeId) {
        if (modeId == null) return null;
        for (GameMode mode : modes) {
            if (mode.id().equalsIgnoreCase(modeId)) return mode;
        }
        return null;
    }

    /** Like {@link #findMode} but invents a gamemode for ids the list added after the mode list was loaded. */
    protected final GameMode mode(String modeId) {
        GameMode known = findMode(modeId);
        return known != null ? known : new GameMode(modeId, null);
    }

    protected abstract List<GameMode> fetchModes() throws IOException;

    /** The player's profile, or null when the list has never ranked them. Blocking. */
    public abstract PlayerProfile fetchProfile(UUID uuid, String name) throws IOException;

    /** Whether {@link #fetchProfile} needs the player's name rather than only the UUID. */
    public boolean requiresName() {
        return false;
    }

    /** Whether peaks are missing from profiles and have to be fetched per gamemode with {@link #fetchPeak}. */
    public boolean hasSeparatePeaks() {
        return false;
    }

    /** Best tier the player ever held in one gamemode, or null. Only used when {@link #hasSeparatePeaks()}. */
    public Tier fetchPeak(PlayerProfile profile, GameMode mode) throws IOException {
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
