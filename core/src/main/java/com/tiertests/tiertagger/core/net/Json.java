package com.tiertests.tiertagger.core.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

/** Null-safe JSON accessors. Only uses Gson API that exists from 2.2.4 (Minecraft 1.8.9) onward. */
public final class Json {
    private Json() {
    }

    public static JsonElement parse(String body) throws IOException {
        try {
            return new JsonParser().parse(body);
        } catch (RuntimeException e) {
            throw new IOException("Malformed JSON", e);
        }
    }

    public static JsonObject parseObject(String body) throws IOException {
        JsonElement element = parse(body);
        if (!element.isJsonObject()) throw new IOException("Expected a JSON object");
        return element.getAsJsonObject();
    }

    public static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent == null ? null : parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public static JsonArray array(JsonObject parent, String key) {
        JsonElement element = parent == null ? null : parent.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    public static String string(JsonObject parent, String key) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        String value = element.getAsString();
        return value == null || value.isEmpty() || value.equalsIgnoreCase("null") ? null : value;
    }

    public static int integer(JsonObject parent, String key, int fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            return element.getAsInt();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double number(JsonObject parent, String key, double fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            return element.getAsDouble();
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject parent, String key, boolean fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        return element.getAsBoolean();
    }
}
