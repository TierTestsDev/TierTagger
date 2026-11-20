package com.tiertests.tiertagger.core.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tiertests.tiertagger.core.model.Badge;
import com.tiertests.tiertagger.core.model.Colors;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Region;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.net.Http;
import com.tiertests.tiertagger.core.net.Json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * api.tiertests.com. One request per player gives tiers, region, badge and rank; peaks need one
 * history request per gamemode, so they are only fetched for lookups and kept in memory for a while.
 */
public final class TierTestsList extends TierList {
    public static final String HOST = "api.tiertests.com";
    private static final String BASE = "https://" + HOST + "/v1";
    private static final long PEAK_TTL_MS = 30 * 60_000L;

    public enum Edition {
        MODERN("tiertests", "Tier Tests", "modern", "MODERN", "rankModern"),
        LEGACY("tiertests_legacy", "Tier Tests Legacy", "legacy", "LEGACY", "rankLegacy");

        final String listId;
        final String displayName;
        final String path;
        final String version;
        final String rankField;

        Edition(String listId, String displayName, String path, String version, String rankField) {
            this.listId = listId;
            this.displayName = displayName;
            this.path = path;
            this.version = version;
            this.rankField = rankField;
        }

        public String path() {
            return path;
        }
    }

    private final Edition edition;
    private final Map<String, CachedPeak> peaks = new ConcurrentHashMap<String, CachedPeak>();

    public TierTestsList(Edition edition, Http http) {
        super(edition.listId, edition.displayName, '', http);
        this.edition = edition;
    }

    public Edition edition() {
        return edition;
    }

    @Override
    protected List<GameMode> fetchModes() throws IOException {
        Http.Response response = http.get(BASE + "/gamemodes/" + edition.path);
        if (!response.ok()) throw new IOException("HTTP " + response.code());
        JsonArray data = Json.array(Json.parseObject(response.body()), "data");
        List<GameMode> modes = new ArrayList<GameMode>();
        if (data == null) return modes;
        for (JsonElement element : data) {
            if (!element.isJsonObject()) continue;
            JsonObject mode = element.getAsJsonObject();
            String name = Json.string(mode, "name");
            if (name == null || Json.bool(mode, "archived", false)) continue;
            int color = Colors.parseHex(Json.string(mode, "colorHex"));
            if (color < 0) color = Colors.byName(Json.string(mode, "color"));
            modes.add(new GameMode(name, Json.string(mode, "beautifiedName"), color));
        }
        return modes;
    }

    @Override
    public PlayerProfile fetchProfile(UUID uuid, String name) throws IOException {
        loadModes();
        Http.Response response = http.get(BASE + "/tiers/current/all?minecraftUuid=" + uuid + "&version=" + edition.version);
        if (response.notFound()) return null;
        if (!response.ok()) throw new IOException("HTTP " + response.code());

        JsonObject root = Json.parseObject(response.body());
        JsonArray data = Json.array(root, "data");
        if (!Json.bool(root, "success", true) || data == null || data.size() == 0) return null;

        PlayerProfile.Builder profile = PlayerProfile.builder(uuid);
        JsonObject first = data.get(0).getAsJsonObject();
        JsonObject user = Json.object(first, "user");
        String accountId = Json.string(user, "discordId");
        profile.name(Json.string(user, "username"))
                .region(Region.parse(Json.string(user, "region")))
                .badge(parseBadge(Json.object(first, "badge")))
                .accountId(accountId);

        JsonObject rank = Json.object(first, edition.rankField);
        if (rank != null) profile.rank(Json.integer(rank, "rank", 0)).points(Json.number(rank, "points", 0));

        for (JsonElement element : data) {
            if (!element.isJsonObject()) continue;
            JsonObject entry = element.getAsJsonObject();
            JsonObject gamemode = Json.object(entry, "gamemode");
            String modeId = Json.string(gamemode, "name");
            String version = Json.string(gamemode, "version");
            Tier tier = Tier.parse(Json.string(entry, "tier"));
            if (modeId == null || tier == null || version != null && !version.equalsIgnoreCase(edition.version)) continue;
            GameMode mode = mode(modeId);
            profile.ranking(mode, new Ranking(tier, cachedPeak(accountId, mode), Json.bool(entry, "retired", false)));
        }
        return profile.build();
    }

    @Override
    public boolean hasSeparatePeaks() {
        return true;
    }

    @Override
    public Tier fetchPeak(PlayerProfile profile, GameMode mode) throws IOException {
        String accountId = profile.accountId();
        if (accountId == null) return null;
        String key = accountId + '/' + mode.id();
        CachedPeak cached = peaks.get(key);
        if (cached != null && !cached.expired()) return cached.tier;

        Http.Response response = http.get(BASE + "/tiers/history/" + accountId + "/" + mode.id().toUpperCase(Locale.ROOT));
        Tier peak = null;
        if (response.ok()) {
            JsonArray history = Json.array(Json.parseObject(response.body()), "data");
            if (history != null) {
                for (JsonElement element : history) {
                    if (!element.isJsonObject()) continue;
                    Tier tier = Tier.parse(Json.string(element.getAsJsonObject(), "tier"));
                    if (tier != null && tier.isBetterThan(peak)) peak = tier;
                }
            }
        } else if (!response.notFound()) {
            throw new IOException("HTTP " + response.code());
        }
        peaks.put(key, new CachedPeak(peak));
        return peak;
    }

    private Tier cachedPeak(String accountId, GameMode mode) {
        if (accountId == null) return null;
        CachedPeak cached = peaks.get(accountId + '/' + mode.id());
        return cached != null && !cached.expired() ? cached.tier : null;
    }

    private static Badge parseBadge(JsonObject badge) {
        if (badge == null) return null;
        String slug = Json.string(badge, "slug");
        String emoji = Json.string(badge, "emoji");
        if (Strings.isBlank(emoji) || "member".equalsIgnoreCase(slug) || "creator".equalsIgnoreCase(slug)) return null;
        int color = Colors.parseHex(Json.string(badge, "color"));
        if (color < 0) color = Colors.byLegacyCode(Json.string(badge, "legacyColor"));
        return new Badge(emoji.trim(), color < 0 ? 0xFFFFFF : color);
    }

    private static final class CachedPeak {
        final Tier tier;
        final long expiresAt = System.currentTimeMillis() + PEAK_TTL_MS;

        CachedPeak(Tier tier) {
            this.tier = tier;
        }

        boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
