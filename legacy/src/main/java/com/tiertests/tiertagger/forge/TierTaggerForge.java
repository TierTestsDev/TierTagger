package com.tiertests.tiertagger.forge;

import com.tiertests.tiertagger.core.Log;
import com.tiertests.tiertagger.core.Platform;
import com.tiertests.tiertagger.core.TierTagger;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.nio.file.Path;

@Mod(modid = TierTaggerForge.MOD_ID, name = "Tier Tagger", useMetadata = true, clientSideOnly = true,
        acceptedMinecraftVersions = "[1.8.9]", guiFactory = "com.tiertests.tiertagger.forge.gui.GuiFactory")
public final class TierTaggerForge {
    public static final String MOD_ID = "tiertag";
    private static final Logger LOGGER = LogManager.getLogger("Tier Tagger");

    static KeyBinding configKey;
    static KeyBinding lookupKey;
    static KeyBinding lastHitKey;

    private Path configDir;
    private String version;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory().toPath();
        version = event.getModMetadata().version;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        Log.setSink(new Log.Sink() {
            @Override
            public void info(String message) {
                LOGGER.info(message);
            }

            @Override
            public void warn(String message, Throwable error) {
                LOGGER.warn(message, error);
            }

            @Override
            public void debug(String message) {
                LOGGER.debug(message);
            }
        });
        TierTagger.start(new ForgePlatform(configDir, version));

        configKey = key("key.tiertag.openconfig", Keyboard.KEY_N);
        lookupKey = key("key.tiertag.lookup", Keyboard.KEY_U);
        lastHitKey = key("key.tiertag.lookup_last_hit", Keyboard.KEY_C);

        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        ClientCommandHandler.instance.registerCommand(new TierTagCommand());
    }

    private static KeyBinding key(String name, int code) {
        KeyBinding binding = new KeyBinding(name, code, "key.category.tiertag.general");
        ClientRegistry.registerKeyBinding(binding);
        return binding;
    }

    private static final class ForgePlatform implements Platform {
        private final Path configDir;
        private final String version;

        ForgePlatform(Path configDir, String version) {
            this.configDir = configDir;
            this.version = version;
        }

        @Override
        public String modVersion() {
            return version;
        }

        @Override
        public String minecraftVersion() {
            return "1.8.9";
        }

        @Override
        public String loader() {
            return "forge";
        }

        @Override
        public Path configDir() {
            return configDir;
        }

        @Override
        public boolean legacyCombat() {
            return true;
        }

        @Override
        public boolean supportsIconFont() {
            return false;
        }
    }
}
