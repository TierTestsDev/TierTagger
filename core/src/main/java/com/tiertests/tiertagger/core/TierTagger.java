package com.tiertests.tiertagger.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tiertests.tiertagger.core.config.ConfigStore;
import com.tiertests.tiertagger.core.config.DisplayMode;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.model.Badge;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.net.Http;
import com.tiertests.tiertagger.core.net.Json;
import com.tiertests.tiertagger.core.source.McpvpList;
import com.tiertests.tiertagger.core.source.TierList;
import com.tiertests.tiertagger.core.source.TierTestsList;
import com.tiertests.tiertagger.core.source.V2TierList;
import com.tiertests.tiertagger.core.text.Tag;
import com.tiertests.tiertagger.core.text.TagBuilder;
import com.tiertests.tiertagger.core.text.Text;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point for platforms. Owns the tier lists, the profile cache, the config and the tag cache.
 * Everything here is thread-safe; {@link #tag} is meant to be called from render code every frame.
 */
public final class TierTagger {
    public static final String MODRINTH_SLUG = "tiertests";
    public static final String MODRINTH_URL = "https://modrinth.com/mod/tiertests";
    public static final String SOURCE_URL = "https://github.com/TierTestsDev/TierTagger";
    private static final String DEFAULT_INVITE = "https://discord.gg/tests";
    private static final long PRUNE_INTERVAL_MS = 60_000L;
    private static final long TAG_IDLE_MS = 5 * 60_000L;

    private static volatile TierTagger instance;

    private final Platform platform;
    private final ExecutorService executor;
    private final Http http;
    private final List<TierList> lists;
    private final TierTestsList badgeList;
    private final ProfileStore store;
    private final ConfigStore configStore;
    private final TaggerConfig config;
    private final TagBuilder tagBuilder;
    private final Settings settings;
    private final MojangApi mojang;
    private final AtomicInteger revision = new AtomicInteger();
    private final ConcurrentHashMap<UUID, CachedTag> tagCache = new ConcurrentHashMap<UUID, CachedTag>();

    private volatile String latestVersion;
    private volatile String discordInvite = DEFAULT_INVITE;
    private volatile long lastPrune = System.currentTimeMillis();

    private TierTagger(Platform platform) {
        this.platform = platform;
        this.executor = Executors.newFixedThreadPool(6, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger();

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "TierTagger-" + count.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        });
        this.http = new Http("TierTagger/" + platform.modVersion() + " (" + platform.loader() + " "
                + platform.minecraftVersion() + "; +" + SOURCE_URL + ")");
        http.limit(TierTestsList.HOST, 15, 4);
        http.limit("www.mcpvp.com", 6, 2);
        http.limit("api.minecraftservices.com", 5, 1);
        http.limit("api.mojang.com", 5, 1);

        TierTestsList modern = new TierTestsList(TierTestsList.Edition.MODERN, http);
        TierTestsList legacy = new TierTestsList(TierTestsList.Edition.LEGACY, http);
        List<TierList> others = Arrays.<TierList>asList(V2TierList.mcTiers(http), V2TierList.pvpTiers(http),
                V2TierList.subTiers(http), new McpvpList(http));
        List<TierList> ordered = new ArrayList<TierList>();
        ordered.add(platform.legacyCombat() ? legacy : modern);
        ordered.addAll(others);
        ordered.add(platform.legacyCombat() ? modern : legacy);
        this.lists = Collections.unmodifiableList(ordered);
        this.badgeList = platform.legacyCombat() ? legacy : modern;

        this.store = new ProfileStore(executor);
        this.configStore = new ConfigStore(platform.configDir(), badgeList.id());
        this.config = configStore.load();
        this.tagBuilder = new TagBuilder(platform.supportsIconFont());
        this.settings = new Settings(this);
        this.mojang = new MojangApi(http);
    }

    public static TierTagger start(Platform platform) {
        TierTagger tagger = new TierTagger(platform);
        instance = tagger;
        tagger.loadModes(tagger.primaryList());
        if (tagger.config.secondary.enabled) tagger.loadModes(tagger.secondaryList());
        tagger.executor.execute(tagger::checkForUpdate);
        tagger.executor.execute(tagger::fetchDiscordInvite);
        Log.info("Tier Tagger " + platform.modVersion() + " started");
        return tagger;
    }

    /** The running instance. Only valid after {@link #start}. */
    public static TierTagger get() {
        return instance;
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Platform platform() {
        return platform;
    }

    public TaggerConfig config() {
        return config;
    }

    public Settings settings() {
        return settings;
    }

    public ProfileStore store() {
        return store;
    }

    public TagBuilder tagBuilder() {
        return tagBuilder;
    }

    /** Incremented on every settings change. */
    public int revision() {
        return revision.get();
    }

    void configChanged() {
        revision.incrementAndGet();
        final TaggerConfig snapshot = config;
        executor.execute(() -> configStore.save(snapshot));
    }

    public List<TierList> lists() {
        return lists;
    }

    public TierList list(String id) {
        for (TierList list : lists) {
            if (list.id().equals(id)) return list;
        }
        return lists.get(0);
    }

    public TierList primaryList() {
        return list(config.primary.list);
    }

    public TierList secondaryList() {
        return list(config.secondary.list);
    }

    /** The slot's gamemode, falling back to the first gamemode once the list has loaded. */
    public GameMode selectedMode(TaggerConfig.Slot slot) {
        TierList list = list(slot.list);
        GameMode mode = list.findMode(slot.mode);
        if (mode != null) return mode;
        List<GameMode> modes = list.modes();
        return modes.isEmpty() ? null : modes.get(0);
    }

    /** Loads a list's gamemodes in the background and fixes up slots saved by older versions. */
    void loadModes(final TierList list) {
        if (list.modesLoaded()) return;
        executor.execute(() -> {
            list.loadModes();
            if (list.modesLoaded()) {
                boolean changed = resolveSlot(config.primary, list) | resolveSlot(config.secondary, list);
                if (changed) configChanged();
                else revision.incrementAndGet();
            }
        });
    }

    private static boolean resolveSlot(TaggerConfig.Slot slot, TierList list) {
        if (!list.id().equals(slot.list) || slot.mode == null || list.findMode(slot.mode) != null) return false;
        for (GameMode mode : list.modes()) {
            if (mode.title().equalsIgnoreCase(slot.mode)) {
                slot.mode = mode.id();
                return true;
            }
        }
        if (slot.mode.startsWith("#")) {
            List<String> ids = new ArrayList<String>();
            for (GameMode mode : list.modes()) ids.add(mode.id());
            Collections.sort(ids);
            try {
                int index = Integer.parseInt(slot.mode.substring(1));
                slot.mode = index >= 0 && index < ids.size() ? ids.get(index) : null;
            } catch (NumberFormatException e) {
                slot.mode = null;
            }
            return true;
        }
        return false;
    }

    /**
     * The tag for a player, or null when there is nothing to show yet. Never blocks: missing data is
     * fetched in the background and shows up on a later call.
     * @param compact icon and tier only, for the tab list
     */
    public Tag tag(UUID uuid, String name, boolean compact) {
        TaggerConfig cfg = config;
        TierList primaryList = list(cfg.primary.list);
        ProfileStore.Entry primary = store.entry(primaryList, uuid, name);
        TierList secondaryList = cfg.secondary.enabled ? list(cfg.secondary.list) : null;
        ProfileStore.Entry secondary = secondaryList != null ? store.entry(secondaryList, uuid, name) : null;
        ProfileStore.Entry badge = cfg.showBadge && !isTierTests(primaryList) && !isTierTests(secondaryList)
                ? store.entry(badgeList, uuid, name) : null;

        List<ProfileStore.Entry> others = null;
        int otherStamps = 0;
        if (cfg.displayMode.usesOtherLists()) {
            others = new ArrayList<ProfileStore.Entry>();
            for (TierList list : lists) {
                if (list == primaryList || isTierTests(list) && list != badgeList) continue;
                ProfileStore.Entry entry = store.entry(list, uuid, name);
                others.add(entry);
                otherStamps += entry.stamp() + 1;
            }
        }

        // Stamps are read before the profiles so a fetch finishing mid-build only causes one extra rebuild.
        int rev = revision.get();
        int primaryStamp = primary.stamp();
        int secondaryStamp = secondary != null ? secondary.stamp() : -1;
        int badgeStamp = badge != null ? badge.stamp() : -1;
        CachedTag cached = tagCache.get(uuid);
        long now = System.currentTimeMillis();
        if (cached != null && cached.matches(rev, primaryStamp, secondaryStamp, badgeStamp, otherStamps)) {
            cached.lastUse = now;
            return compact ? cached.compact : cached.full;
        }

        List<PlayerProfile> otherProfiles = null;
        if (others != null) {
            otherProfiles = new ArrayList<PlayerProfile>();
            for (ProfileStore.Entry entry : others) {
                if (entry.profile() != null) otherProfiles.add(entry.profile());
            }
        }
        PlayerProfile primaryProfile = primary.profile();
        PlayerProfile secondaryProfile = secondary != null ? secondary.profile() : null;
        Badge badgeValue = badgeOf(primaryList, primaryProfile, secondaryList, secondaryProfile,
                badge != null ? badge.profile() : null);

        Tag full = buildTag(cfg, primaryProfile, secondaryProfile, otherProfiles, badgeValue, false);
        Tag compactTag = buildTag(cfg, primaryProfile, secondaryProfile, otherProfiles, null, true);
        CachedTag fresh = new CachedTag(rev, primaryStamp, secondaryStamp, badgeStamp, otherStamps, full, compactTag);
        fresh.lastUse = now;
        tagCache.put(uuid, fresh);
        return compact ? compactTag : full;
    }

    private Tag buildTag(TaggerConfig cfg, PlayerProfile primaryProfile, PlayerProfile secondaryProfile,
                         List<PlayerProfile> others, Badge badge, boolean compact) {
        DisplayMode mode = cfg.displayMode;
        TagBuilder.Pick primaryPick = tagBuilder.pick(primaryProfile, selectedMode(cfg.primary), mode, others);
        Text primaryText = tagBuilder.slot(primaryPick, cfg);

        Text secondaryText = Text.EMPTY;
        if (cfg.secondary.enabled) {
            DisplayMode secondaryMode = mode == DisplayMode.SELECTED || mode == DisplayMode.BEST
                    ? mode : DisplayMode.SELECTED_OR_BEST;
            secondaryText = tagBuilder.slot(
                    tagBuilder.pick(secondaryProfile, selectedMode(cfg.secondary), secondaryMode, null), cfg);
        }

        if (!compact) {
            boolean onPrimary = !primaryText.isEmpty();
            if (onPrimary || !secondaryText.isEmpty()) {
                Text.Builder withDetails = Text.builder().append(onPrimary ? primaryText : secondaryText);
                tagBuilder.appendDetails(withDetails, onPrimary ? primaryProfile : secondaryProfile, badge, cfg);
                if (onPrimary) primaryText = withDetails.build();
                else secondaryText = withDetails.build();
            } else if (badge != null && cfg.showBadge) {
                primaryText = Text.builder().append(badge.symbol(), badge.color()).build();
            }
        }
        return tagBuilder.compose(primaryText, secondaryText, cfg);
    }

    private static boolean isTierTests(TierList list) {
        return list instanceof TierTestsList;
    }

    private static Badge badgeOf(TierList primaryList, PlayerProfile primary, TierList secondaryList,
                                 PlayerProfile secondary, PlayerProfile badgeProfile) {
        if (isTierTests(primaryList) && primary != null && primary.badge() != null) return primary.badge();
        if (isTierTests(secondaryList) && secondary != null && secondary.badge() != null) return secondary.badge();
        return badgeProfile != null ? badgeProfile.badge() : null;
    }

    /** Housekeeping; call from the client tick. Cheap when there is nothing to do. */
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastPrune < PRUNE_INTERVAL_MS) return;
        lastPrune = now;
        store.prune();
        Iterator<CachedTag> it = tagCache.values().iterator();
        while (it.hasNext()) {
            if (now - it.next().lastUse > TAG_IDLE_MS) it.remove();
        }
    }

    /** Forgets every cached profile so everything is fetched again. */
    public void refreshAll() {
        store.clear();
        tagCache.clear();
        revision.incrementAndGet();
    }

    /**
     * Profile shown by the lookup popup: the primary list with peaks, or if the player is not on it,
     * whichever other list ranks them highest.
     */
    public CompletableFuture<LookupResult> lookup(final UUID uuid, final String name) {
        final TierList primaryList = primaryList();
        return store.requestDetailed(primaryList, uuid, name).thenCompose(profile -> {
            if (profile != null && !profile.isEmpty()) {
                return CompletableFuture.completedFuture(single(uuid, name, primaryList, profile));
            }
            final List<TierList> candidates = new ArrayList<TierList>();
            final List<CompletableFuture<PlayerProfile>> futures = new ArrayList<CompletableFuture<PlayerProfile>>();
            for (TierList list : lists) {
                if (list == primaryList) continue;
                candidates.add(list);
                futures.add(store.requestDetailed(list, uuid, name));
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(ignored -> {
                TierList bestList = null;
                PlayerProfile best = null;
                for (int i = 0; i < candidates.size(); i++) {
                    PlayerProfile candidate = futures.get(i).join();
                    if (candidate == null || candidate.isEmpty()) continue;
                    if (best == null || candidate.best().getValue().compareTo(best.best().getValue()) < 0) {
                        best = candidate;
                        bestList = candidates.get(i);
                    }
                }
                return best == null ? new LookupResult(uuid, name, Collections.<TierList, PlayerProfile>emptyMap())
                        : single(uuid, name, bestList, best);
            });
        });
    }

    /** Every list's profile for a username. Completes with null when the account does not exist. */
    public CompletableFuture<LookupResult> search(final String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mojang.resolve(username);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor).thenCompose(account -> {
            if (account == null) return CompletableFuture.completedFuture(null);
            final List<CompletableFuture<PlayerProfile>> futures = new ArrayList<CompletableFuture<PlayerProfile>>();
            for (TierList list : lists) futures.add(store.requestDetailed(list, account.uuid, account.name));
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(ignored -> {
                Map<TierList, PlayerProfile> profiles = new LinkedHashMap<TierList, PlayerProfile>();
                for (int i = 0; i < lists.size(); i++) {
                    PlayerProfile profile = futures.get(i).join();
                    if (profile != null && !profile.isEmpty()) profiles.put(lists.get(i), profile);
                }
                return new LookupResult(account.uuid, account.name, profiles);
            });
        });
    }

    private static LookupResult single(UUID uuid, String name, TierList list, PlayerProfile profile) {
        Map<TierList, PlayerProfile> map = new LinkedHashMap<TierList, PlayerProfile>();
        map.put(list, profile);
        return new LookupResult(uuid, profile.name() != null ? profile.name() : name, map);
    }

    /** Newer release on Modrinth for this loader and Minecraft version, or null. */
    public String availableUpdate() {
        String latest = latestVersion;
        return latest != null && Versions.compare(latest, platform.modVersion()) > 0 ? latest : null;
    }

    public String discordInvite() {
        return discordInvite;
    }

    private void checkForUpdate() {
        try {
            String url = "https://api.modrinth.com/v2/project/" + MODRINTH_SLUG + "/version?loaders="
                    + encode("[\"" + platform.loader() + "\"]") + "&game_versions="
                    + encode("[\"" + platform.minecraftVersion() + "\"]");
            Http.Response response = http.get(url);
            if (!response.ok()) return;
            String latest = null;
            JsonElement root = Json.parse(response.body());
            if (!root.isJsonArray()) return;
            JsonArray versions = root.getAsJsonArray();
            for (JsonElement element : versions) {
                if (!element.isJsonObject()) continue;
                JsonObject version = element.getAsJsonObject();
                String number = Json.string(version, "version_number");
                if (number != null && "release".equals(Json.string(version, "version_type"))
                        && (latest == null || Versions.compare(number, latest) > 0)) {
                    latest = number;
                }
            }
            latestVersion = latest;
            if (availableUpdate() != null) Log.info("Tier Tagger " + latest + " is available");
        } catch (Exception e) {
            Log.debug("Update check failed: " + e.getMessage());
        }
    }

    private void fetchDiscordInvite() {
        try {
            String key = platform.legacyCombat() ? "legacy" : "modern";
            Http.Response response = http.get("https://" + TierTestsList.HOST + "/v1/config/" + key + ".guild_invite");
            if (!response.ok()) return;
            String url = Json.string(Json.object(Json.parseObject(response.body()), "data"), "value");
            if (isTrustedInvite(url)) discordInvite = url;
            else if (url != null) Log.warn("Ignoring unexpected Discord invite URL: " + url);
        } catch (Exception e) {
            Log.debug("Discord invite fetch failed: " + e.getMessage());
        }
    }

    /** Only https links to Discord's own domains are ever opened. */
    public static boolean isTrustedInvite(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && (host.equals("discord.gg") || host.equals("discord.com") || host.equals("www.discord.com"));
        } catch (Exception e) {
            return false;
        }
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class CachedTag {
        final int revision;
        final int primary;
        final int secondary;
        final int badge;
        final int others;
        final Tag full;
        final Tag compact;
        volatile long lastUse;

        CachedTag(int revision, int primary, int secondary, int badge, int others, Tag full, Tag compact) {
            this.revision = revision;
            this.primary = primary;
            this.secondary = secondary;
            this.badge = badge;
            this.others = others;
            this.full = full;
            this.compact = compact;
        }

        boolean matches(int revision, int primary, int secondary, int badge, int others) {
            return this.revision == revision && this.primary == primary && this.secondary == secondary
                    && this.badge == badge && this.others == others;
        }
    }
}
