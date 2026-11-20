package com.tiertests.tiertagger.core.config;

import com.tiertests.tiertagger.core.model.TierFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {
    @Test
    void parsesFlowAndBlockYaml() {
        Map<String, String> flow = ConfigStore.parseFlatYaml("{game-mode: 3, show-region: false, secondary-game-mode: ''}\n");
        assertEquals("3", flow.get("game-mode"));
        assertEquals("false", flow.get("show-region"));
        assertEquals("", flow.get("secondary-game-mode"));

        Map<String, String> block = ConfigStore.parseFlatYaml("game-mode: 2\ndisplay-type: SPLIT\n");
        assertEquals("2", block.get("game-mode"));
        assertEquals("SPLIT", block.get("display-type"));
    }

    @Test
    void importsOldSettings(@TempDir Path dir) throws Exception {
        String yaml = "{game-mode: 4, tier-display-mode: CROSS_API_ANY_MODE, display-type: SPLIT, "
                + "use-mc-tiers-format: false, show-region: false, show-in-tab: true, tier-source: 3, icon-type: CLASSIC, "
                + "secondary-enabled: true, secondary-source: 2, secondary-game-mode: Bed}";
        Files.write(dir.resolve("tiertag.yml"), yaml.getBytes(StandardCharsets.UTF_8));

        TaggerConfig config = new ConfigStore(dir, "tiertests").load();

        assertEquals("pvptiers", config.primary.list);
        assertEquals("#4", config.primary.mode);
        assertTrue(config.secondary.enabled);
        assertEquals("subtiers", config.secondary.list);
        assertEquals("Bed", config.secondary.mode);
        assertEquals(DisplayMode.BEST_ANY_LIST, config.displayMode);
        assertEquals(Layout.SPLIT, config.layout);
        assertEquals(IconStyle.CLASSIC, config.icons);
        assertEquals(TierFormat.LETTER, config.tierFormat);
        assertFalse(config.showRegion);
        assertTrue(Files.isRegularFile(dir.resolve(ConfigStore.FILE_NAME)));
    }

    @Test
    void roundTripsAndRepairsJson(@TempDir Path dir) throws Exception {
        ConfigStore store = new ConfigStore(dir, "tiertests_legacy");
        TaggerConfig config = store.load();
        assertEquals("tiertests_legacy", config.primary.list);
        config.layout = Layout.SUFFIX;
        store.save(config);
        assertEquals(Layout.SUFFIX, store.load().layout);

        Files.write(dir.resolve(ConfigStore.FILE_NAME), "{\"layout\":\"SIDEWAYS\",\"primary\":null}".getBytes(StandardCharsets.UTF_8));
        TaggerConfig repaired = store.load();
        assertEquals(Layout.PREFIX, repaired.layout);
        assertEquals("tiertests_legacy", repaired.primary.list);
        assertNull(repaired.primary.mode);
    }
}
