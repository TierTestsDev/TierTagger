package com.tiertests.tiertagger.core.source;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lists that share the MCTiers profile format: rankings keyed by gamemode with numeric tier, position,
 * peak and retired flag. MCTiers, SubTiers and PvPTiers all speak it.
 */
public final class V2TierList extends TierList {
    private final String profileUrl;
    private final String modesUrl;
    private final List<GameMode> fixedModes;

    private V2TierList(String id, String name, char logo, Http http, String profileUrl, String modesUrl,
                       List<GameMode> fixedModes) {
        super(id, name, logo, http);
        this.profileUrl = profileUrl;
        this.modesUrl = modesUrl;
        this.fixedModes = fixedModes;
    }

    public static V2TierList mcTiers(Http http) {
        return new V2TierList("mctiers", "MCTiers", '', http,
                "https://mctiers.net/api/v2/profile/%s", "https://mctiers.net/api/v2/mode/list", null);
    }

    public static V2TierList subTiers(Http http) {
        return new V2TierList("subtiers", "SubTiers", '', http,
                "https://subtiers.net/api/v2/profile/%s", "https://subtiers.net/api/v2/mode/list", null);
    }

    /** PvPTiers has no lightweight mode list (its /tierlists response is ~120 KB of rules text), so it is fixed here. */
    public static V2TierList pvpTiers(Http http) {
        List<GameMode> modes = Arrays.asList(
                new GameMode("sword", "Sword"), new GameMode("axe", "Axe"), new GameMode("mace", "Mace"),
                new GameMode("uhc", "UHC"), new GameMode("pot", "Pot"), new GameMode("neth_pot", "Netherite Pot"),
                new GameMode("smp", "SMP"), new GameMode("crystal", "Crystal"), new GameMode("elytra", "Elytra"));
        return new V2TierList("pvptiers", "PvPTiers", '', http, "https://pvptiers.com/api/profile/%s", null, modes);
    }

    @Override
    protected List<GameMode> fetchModes() throws IOException {
        if (fixedModes != null) return fixedModes;
        Http.Response response = http.get(modesUrl);
        if (!response.ok()) throw new IOException("HTTP " + response.code());
        List<GameMode> modes = new ArrayList<GameMode>();
        for (Map.Entry<String, JsonElement> entry : Json.parseObject(response.body()).entrySet()) {
            JsonObject info = entry.getValue().isJsonObject() ? entry.getValue().getAsJsonObject() : null;
            modes.add(new GameMode(entry.getKey(), Json.string(info, "title")));
        }
        return modes;
    }

    @Override
    public PlayerProfile fetchProfile(UUID uuid, String name) throws IOException {
        loadModes();
        Http.Response response = http.get(String.format(profileUrl, Strings.undashed(uuid)));
        if (response.notFound()) return null;
        if (!response.ok()) throw new IOException("HTTP " + response.code());
        return parseProfile(uuid, Json.parseObject(response.body()));
    }

    PlayerProfile parseProfile(UUID uuid, JsonObject root) {
        PlayerProfile.Builder profile = PlayerProfile.builder(uuid)
                .name(Json.string(root, "name"))
                .region(Region.parse(Json.string(root, "region")))
                .rank(Json.integer(root, "overall", 0))
                .points(Json.number(root, "points", 0));

        JsonObject rankings = Json.object(root, "rankings");
        if (rankings != null) {
            for (Map.Entry<String, JsonElement> entry : rankings.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject ranking = entry.getValue().getAsJsonObject();
                Tier tier = Tier.of(Json.integer(ranking, "tier", 0), Json.integer(ranking, "pos", 1));
                if (tier == null) continue;
                Tier peak = Tier.of(Json.integer(ranking, "peak_tier", 0), Json.integer(ranking, "peak_pos", 1));
                profile.ranking(mode(entry.getKey()), new Ranking(tier, peak, Json.bool(ranking, "retired", false)));
            }
        }
        return profile.build();
    }
}
