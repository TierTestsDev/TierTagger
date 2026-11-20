package com.tiertests.tiertagger.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tiertests.tiertagger.core.Log;
import com.tiertests.tiertagger.core.model.TierFormat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/** Reads and writes {@code tiertag.json}, importing settings from the YAML files older versions wrote. */
public final class ConfigStore {
    static final String FILE_NAME = "tiertag.json";
    private static final String[] LEGACY_FILES = {"tiertag.yml", "tiertagger.yml"};
    private static final String[] LEGACY_LIST_IDS = {"tiertests", "mctiers", "subtiers", "pvptiers"};
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path dir;
    private final String defaultList;

    public ConfigStore(Path dir, String defaultList) {
        this.dir = dir;
        this.defaultList = defaultList;
    }

    public TaggerConfig load() {
        TaggerConfig config = null;
        Path file = dir.resolve(FILE_NAME);
        if (Files.isRegularFile(file)) {
            try {
                config = GSON.fromJson(new String(Files.readAllBytes(file), UTF_8), TaggerConfig.class);
            } catch (Exception e) {
                Log.warn("Could not read " + file + ", using defaults", e);
            }
        } else {
            config = importLegacy();
        }
        if (config == null) config = defaults();
        config.sanitize(defaultList);
        if (!Files.isRegularFile(file)) save(config);
        return config;
    }

    public void save(TaggerConfig config) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(FILE_NAME);
            Path temp = dir.resolve(FILE_NAME + ".tmp");
            Files.write(temp, GSON.toJson(config).getBytes(UTF_8));
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            Log.warn("Could not save config", e);
        }
    }

    private TaggerConfig defaults() {
        TaggerConfig config = new TaggerConfig();
        config.primary.list = defaultList;
        return config;
    }

    private TaggerConfig importLegacy() {
        for (String name : LEGACY_FILES) {
            Path file = dir.resolve(name);
            if (!Files.isRegularFile(file)) continue;
            try {
                TaggerConfig config = fromLegacy(parseFlatYaml(new String(Files.readAllBytes(file), UTF_8)));
                Log.info("Imported settings from " + name);
                return config;
            } catch (Exception e) {
                Log.warn("Could not import " + name, e);
            }
        }
        return null;
    }

    TaggerConfig fromLegacy(Map<String, String> values) {
        TaggerConfig config = defaults();
        config.primary.list = listByOrdinal(values.get("tier-source"), config.primary.list);
        if (values.containsKey("game-mode")) config.primary.mode = "#" + values.get("game-mode").trim();

        config.secondary.enabled = "true".equalsIgnoreCase(values.get("secondary-enabled"));
        config.secondary.list = listByOrdinal(values.get("secondary-source"), config.secondary.list);
        String secondaryMode = values.get("secondary-game-mode");
        config.secondary.mode = secondaryMode == null || secondaryMode.isEmpty() ? null : secondaryMode;

        String mode = values.get("tier-display-mode");
        if ("HIGHEST_ALWAYS".equals(mode)) config.displayMode = DisplayMode.BEST;
        else if ("SELECTED_ONLY".equals(mode)) config.displayMode = DisplayMode.SELECTED;
        else if ("RANKING".equals(mode)) config.displayMode = DisplayMode.RANK;
        else if ("CROSS_API_SAME_MODE".equals(mode)) config.displayMode = DisplayMode.SELECTED_ANY_LIST;
        else if ("CROSS_API_ANY_MODE".equals(mode)) config.displayMode = DisplayMode.BEST_ANY_LIST;

        String type = values.get("display-type");
        if ("SUFFIX".equals(type)) config.layout = Layout.SUFFIX;
        else if ("SPLIT".equals(type)) config.layout = Layout.SPLIT;

        if ("CLASSIC".equals(values.get("icon-type"))) config.icons = IconStyle.CLASSIC;
        if ("false".equalsIgnoreCase(values.get("use-mc-tiers-format"))) config.tierFormat = TierFormat.LETTER;
        config.showRegion = !"false".equalsIgnoreCase(values.get("show-region"));
        config.showInTab = !"false".equalsIgnoreCase(values.get("show-in-tab"));
        return config;
    }

    private static String listByOrdinal(String raw, String fallback) {
        if (raw == null) return fallback;
        try {
            int ordinal = Integer.parseInt(raw.trim());
            return ordinal >= 0 && ordinal < LEGACY_LIST_IDS.length ? LEGACY_LIST_IDS[ordinal] : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Parses the flat key/value YAML older versions wrote, in either block or single-line flow style. */
    static Map<String, String> parseFlatYaml(String text) {
        Map<String, String> values = new HashMap<String, String>();
        String body = text.trim();
        String[] entries;
        if (body.startsWith("{") && body.endsWith("}")) {
            entries = body.substring(1, body.length() - 1).split(",");
        } else {
            entries = body.split("\\r?\\n");
        }
        for (String entry : entries) {
            int colon = entry.indexOf(':');
            if (colon <= 0 || entry.trim().startsWith("#")) continue;
            String key = entry.substring(0, colon).trim();
            String value = entry.substring(colon + 1).trim();
            if (value.length() >= 2 && (value.startsWith("'") && value.endsWith("'") || value.startsWith("\"") && value.endsWith("\""))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }
}
