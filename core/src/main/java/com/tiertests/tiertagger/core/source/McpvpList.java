package com.tiertests.tiertagger.core.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Region;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.net.Http;
import com.tiertests.tiertagger.core.net.Json;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * mcpvp.com tiers. The API only searches by name and returns fuzzy matches, so the result is matched
 * back to the player's UUID. Ranks use high, middle and low tiers (HT3, MT3, LT3).
 */
public final class McpvpList extends TierList {
    private static final String SEARCH = "https://www.mcpvp.com/tiers/search?kit=overall&include_retired=1&q=";

    private static final List<GameMode> MODES = Arrays.asList(
            new GameMode("sword", "Sword"), new GameMode("mace", "Mace"), new GameMode("spear", "Spear"),
            new GameMode("shield", "Shield"), new GameMode("pot", "Pot"), new GameMode("netherite-pot", "Netherite Pot"),
            new GameMode("uhc", "UHC"), new GameMode("smp", "SMP"), new GameMode("diamond-smp", "Diamond SMP"),
            new GameMode("crystal", "Crystal"), new GameMode("cart", "Cart"), new GameMode("creeper", "Creeper"),
            new GameMode("bow", "Bow"), new GameMode("early-game", "Early Game"), new GameMode("late-game", "Late Game"),
            new GameMode("end-game", "End Game"));

    public McpvpList(Http http) {
        super("mcpvp", "MCPVP", (char) 0, http);
    }

    @Override
    protected List<GameMode> fetchModes() {
        return MODES;
    }

    @Override
    public boolean requiresName() {
        return true;
    }

    @Override
    public PlayerProfile fetchProfile(UUID uuid, String name) throws IOException {
        if (!Strings.isValidUsername(name)) return null;
        loadModes();
        Http.Response response = http.get(SEARCH + URLEncoder.encode(name, "UTF-8"));
        if (response.notFound()) return null;
        if (!response.ok()) throw new IOException("HTTP " + response.code());

        JsonArray players = Json.array(Json.parseObject(response.body()), "players");
        JsonObject match = players == null ? null : findPlayer(players, uuid, name);
        return match == null ? null : parsePlayer(uuid, match);
    }

    private static JsonObject findPlayer(JsonArray players, UUID uuid, String name) {
        JsonObject byName = null;
        for (JsonElement element : players) {
            if (!element.isJsonObject()) continue;
            JsonObject player = element.getAsJsonObject();
            UUID id = Strings.parseUuid(Json.string(player, "uuid"));
            if (uuid != null && uuid.equals(id)) return player;
            if (byName == null && name.equalsIgnoreCase(Json.string(player, "name"))) byName = player;
        }
        return uuid == null ? byName : null;
    }

    PlayerProfile parsePlayer(UUID uuid, JsonObject player) {
        PlayerProfile.Builder profile = PlayerProfile.builder(uuid)
                .name(Json.string(player, "name"))
                .region(Region.parse(Json.string(player, "region")))
                .rank(Json.integer(player, "rank", 0))
                .points(Json.number(player, "points", 0));

        JsonObject ranks = Json.object(player, "kitRanks");
        JsonObject peaks = Json.object(player, "kitPeakRanks");
        JsonObject retired = Json.object(player, "kitRetired");
        if (ranks != null) {
            for (Map.Entry<String, JsonElement> entry : ranks.entrySet()) {
                Tier tier = Tier.parse(Json.string(ranks, entry.getKey()));
                if (tier == null) continue;
                Tier peak = Tier.parse(Json.string(peaks, entry.getKey()));
                profile.ranking(mode(entry.getKey()), new Ranking(tier, peak, Json.bool(retired, entry.getKey(), false)));
            }
        }
        return profile.build();
    }
}
