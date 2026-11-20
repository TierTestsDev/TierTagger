package com.tiertests.tiertagger.core;

import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.source.TierList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Per-list profile cache that fetches lazily, serves stale data while it refreshes, and backs off on errors.
 *
 * {@link #entry} is cheap enough to call every frame: it never blocks and only queues a request when the
 * cached value is missing or due. Players that are no longer looked at are dropped by {@link #prune()}.
 */
public final class ProfileStore {
    static final long FRESH_MS = 10 * 60_000L;
    static final long MISSING_RETRY_MS = 15 * 60_000L;
    static final long ERROR_BACKOFF_MS = 15_000L;
    static final long MAX_ERROR_BACKOFF_MS = 5 * 60_000L;
    static final long IDLE_EVICT_MS = 20 * 60_000L;

    private final Executor executor;
    private final ConcurrentHashMap<String, ConcurrentHashMap<UUID, Entry>> lists = new ConcurrentHashMap<String, ConcurrentHashMap<UUID, Entry>>();

    public ProfileStore(Executor executor) {
        this.executor = executor;
    }

    /** Cache entry for the player, queueing a fetch when needed. Never blocks. */
    public Entry entry(TierList list, UUID uuid, String name) {
        Entry entry = slot(list, uuid);
        long now = System.currentTimeMillis();
        entry.lastAccess = now;
        if (name != null) entry.name = name;
        if (entry.inflight == null && now >= entry.nextFetchAt && (entry.name != null || !list.requiresName())) {
            fetch(list, uuid, entry);
        }
        return entry;
    }

    /** Current (possibly stale) profile, or null. Same side effects as {@link #entry}. */
    public PlayerProfile get(TierList list, UUID uuid, String name) {
        return entry(list, uuid, name).profile;
    }

    /** Fetches now unless a fetch is already running or the cached value is still fresh. */
    public CompletableFuture<PlayerProfile> request(TierList list, UUID uuid, String name) {
        Entry entry = slot(list, uuid);
        entry.lastAccess = System.currentTimeMillis();
        if (name != null) entry.name = name;
        if (entry.inflight != null) return entry.inflight;
        if (entry.status == Status.FOUND && System.currentTimeMillis() < entry.fetchedAt + FRESH_MS
                || entry.status == Status.MISSING && System.currentTimeMillis() < entry.nextFetchAt) {
            return CompletableFuture.completedFuture(entry.profile);
        }
        return fetch(list, uuid, entry);
    }

    /** Like {@link #request} and then fills in peak tiers for lists that report them separately. */
    public CompletableFuture<PlayerProfile> requestDetailed(final TierList list, final UUID uuid, String name) {
        return request(list, uuid, name).thenCompose(profile -> {
            if (profile == null || profile.isEmpty() || !list.hasSeparatePeaks()) {
                return CompletableFuture.completedFuture(profile);
            }
            final List<GameMode> modes = new ArrayList<GameMode>();
            final List<CompletableFuture<Tier>> peaks = new ArrayList<CompletableFuture<Tier>>();
            for (Map.Entry<GameMode, Ranking> ranking : profile.rankings().entrySet()) {
                if (ranking.getValue().peak() != null) continue;
                final GameMode mode = ranking.getKey();
                modes.add(mode);
                peaks.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return list.fetchPeak(profile, mode);
                    } catch (Exception e) {
                        Log.debug("Peak lookup failed for " + mode.id() + ": " + e.getMessage());
                        return null;
                    }
                }, executor));
            }
            if (modes.isEmpty()) return CompletableFuture.completedFuture(profile);
            return CompletableFuture.allOf(peaks.toArray(new CompletableFuture[0])).thenApply(ignored -> {
                PlayerProfile.Builder builder = profile.toBuilder();
                for (int i = 0; i < modes.size(); i++) {
                    Tier peak = peaks.get(i).join();
                    if (peak != null) builder.ranking(modes.get(i), profile.ranking(modes.get(i)).withPeak(peak));
                }
                PlayerProfile detailed = builder.build();
                Entry entry = slot(list, uuid);
                if (entry.profile == profile) entry.update(detailed);
                return detailed;
            });
        });
    }

    /** Drops players nobody asked about for a while. Call about once a minute. */
    public void prune() {
        long cutoff = System.currentTimeMillis() - IDLE_EVICT_MS;
        for (ConcurrentHashMap<UUID, Entry> entries : lists.values()) {
            Iterator<Entry> it = entries.values().iterator();
            while (it.hasNext()) {
                Entry entry = it.next();
                if (entry.inflight == null && entry.lastAccess < cutoff) it.remove();
            }
        }
    }

    public void clear() {
        lists.clear();
    }

    private Entry slot(TierList list, UUID uuid) {
        ConcurrentHashMap<UUID, Entry> entries = lists.get(list.id());
        if (entries == null) {
            entries = new ConcurrentHashMap<UUID, Entry>();
            ConcurrentHashMap<UUID, Entry> existing = lists.putIfAbsent(list.id(), entries);
            if (existing != null) entries = existing;
        }
        Entry entry = entries.get(uuid);
        if (entry == null) {
            entry = new Entry();
            Entry existing = entries.putIfAbsent(uuid, entry);
            if (existing != null) entry = existing;
        }
        return entry;
    }

    private CompletableFuture<PlayerProfile> fetch(final TierList list, final UUID uuid, final Entry entry) {
        final CompletableFuture<PlayerProfile> future;
        synchronized (entry) {
            if (entry.inflight != null) return entry.inflight;
            future = new CompletableFuture<PlayerProfile>();
            entry.inflight = future;
        }
        final String name = entry.name;
        try {
            executor.execute(() -> {
                try {
                    PlayerProfile profile = list.fetchProfile(uuid, name);
                    entry.complete(profile);
                    future.complete(profile);
                } catch (Throwable e) {
                    Log.debug(list.displayName() + " lookup failed for " + uuid + ": " + e.getMessage());
                    entry.fail();
                    future.complete(entry.profile);
                } finally {
                    entry.inflight = null;
                }
            });
        } catch (RuntimeException rejected) {
            entry.inflight = null;
            future.complete(entry.profile);
        }
        return future;
    }

    enum Status { NEW, FOUND, MISSING, FAILED }

    /** Cached state for one player on one list. */
    public static final class Entry {
        volatile PlayerProfile profile;
        volatile Status status = Status.NEW;
        volatile long fetchedAt;
        volatile long nextFetchAt;
        volatile long lastAccess;
        volatile int failures;
        volatile int stamp;
        volatile String name;
        volatile CompletableFuture<PlayerProfile> inflight;

        public PlayerProfile profile() {
            return profile;
        }

        /** Changes whenever the cached profile does; lets render caches skip rebuilding. */
        public int stamp() {
            return stamp;
        }

        public boolean isLoading() {
            return inflight != null;
        }

        /** True once a lookup finished, whether or not the player was found. */
        public boolean isResolved() {
            return status == Status.FOUND || status == Status.MISSING;
        }

        void complete(PlayerProfile result) {
            long now = System.currentTimeMillis();
            fetchedAt = now;
            failures = 0;
            status = result != null ? Status.FOUND : Status.MISSING;
            nextFetchAt = now + (result != null ? FRESH_MS : MISSING_RETRY_MS);
            update(result);
        }

        void fail() {
            int attempt = Math.min(failures, 5);
            failures = attempt + 1;
            if (status == Status.NEW) status = Status.FAILED;
            nextFetchAt = System.currentTimeMillis() + Math.min(MAX_ERROR_BACKOFF_MS, ERROR_BACKOFF_MS << attempt);
        }

        void update(PlayerProfile result) {
            profile = result;
            stamp++;
        }
    }
}
