package com.tiertests.tiertagger.core;

import com.google.gson.JsonObject;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.net.Http;
import com.tiertests.tiertagger.core.net.Json;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Username to UUID lookups, cached for an hour. */
final class MojangApi {
    private static final long TTL_MS = 60 * 60_000L;

    static final class Account {
        final UUID uuid;
        final String name;
        final long expiresAt = System.currentTimeMillis() + TTL_MS;

        Account(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    private final Http http;
    private final Map<String, Account> cache = new ConcurrentHashMap<String, Account>();

    MojangApi(Http http) {
        this.http = http;
    }

    /** The account for a username, or null if it does not exist. Blocking. */
    Account resolve(String username) throws IOException {
        String key = username.toLowerCase(Locale.ROOT);
        Account cached = cache.get(key);
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) return cached;

        Http.Response response = http.get("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + username);
        if (!response.ok() && !response.notFound() && response.code() != 204) {
            response = http.get("https://api.mojang.com/users/profiles/minecraft/" + username);
        }
        if (!response.ok() || response.body().isEmpty()) return null;
        JsonObject body = Json.parseObject(response.body());
        UUID uuid = Strings.parseUuid(Json.string(body, "id"));
        if (uuid == null) return null;
        Account account = new Account(uuid, Json.string(body, "name") != null ? Json.string(body, "name") : username);
        cache.put(key, account);
        return account;
    }
}
