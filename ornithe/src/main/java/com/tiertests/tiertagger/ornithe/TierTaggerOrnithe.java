package com.tiertests.tiertagger.ornithe;

import com.tiertests.tiertagger.core.Log;
import com.tiertests.tiertagger.core.Platform;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.ornithe.gui.ConfigScreen;
import com.tiertests.tiertagger.ornithe.gui.LookupHud;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.living.player.PlayerEntity;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
import net.ornithemc.osl.keybinds.api.KeybindEvents;
import net.ornithemc.osl.keybinds.api.KeybindRegistry;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.nio.file.Path;
import java.util.UUID;

/** Ornithe (Fabric for 1.8.9) entrypoint. Game hooks live in the mixin package; this wires up keys and ticks. */
public final class TierTaggerOrnithe implements ClientModInitializer {
    public static final String MOD_ID = "tiertag";
    private static final double LOOKUP_RANGE_SQ = 64 * 64;
    private static final String CATEGORY = "key.category.tiertag.general";
    private static final Logger LOGGER = LogManager.getLogger("Tier Tagger");

    private static KeyBinding configKey;
    private static KeyBinding lookupKey;
    private static KeyBinding lastHitKey;
    private static UUID lastHit;

    @Override
    public void initClient() {
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
        TierTagger.start(new OrnithePlatform());

        KeybindEvents.REGISTER_KEYBINDS.register(() -> {
            configKey = KeybindRegistry.register("key.tiertag.openconfig", Keyboard.KEY_N, CATEGORY);
            lookupKey = KeybindRegistry.register("key.tiertag.lookup", Keyboard.KEY_U, CATEGORY);
            lastHitKey = KeybindRegistry.register("key.tiertag.lookup_last_hit", Keyboard.KEY_C, CATEGORY);
        });
        MinecraftClientEvents.TICK_END.register(TierTaggerOrnithe::tick);
        MinecraftClientEvents.STOP.register(minecraft -> TierTagger.get().shutdown());
    }

    public static void onAttack(PlayerEntity target) {
        lastHit = target.getUuid();
    }

    private static void tick(Minecraft minecraft) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null || configKey == null) return;
        tagger.tick();
        if (minecraft.world == null) lastHit = null;

        while (configKey.consumeClick()) {
            minecraft.openScreen(new ConfigScreen(null));
        }
        while (lookupKey.consumeClick()) {
            PlayerEntity target = nearest(minecraft);
            if (target != null) LookupHud.lookup(target.getUuid(), target.getName());
        }
        while (lastHitKey.consumeClick()) {
            PlayerEntity target = lastHit == null || minecraft.world == null ? null : minecraft.world.getPlayer(lastHit);
            if (target != null && minecraft.player != null && target.squaredDistanceTo(minecraft.player) <= LOOKUP_RANGE_SQ) {
                LookupHud.lookup(target.getUuid(), target.getName());
            }
        }
    }

    private static PlayerEntity nearest(Minecraft minecraft) {
        if (minecraft.world == null || minecraft.player == null) return null;
        PlayerEntity nearest = null;
        double best = LOOKUP_RANGE_SQ;
        for (PlayerEntity player : minecraft.world.players) {
            if (player == minecraft.player || player.removed) continue;
            double distance = player.squaredDistanceTo(minecraft.player);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static final class OrnithePlatform implements Platform {
        @Override
        public String modVersion() {
            return FabricLoader.getInstance().getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        }

        @Override
        public String minecraftVersion() {
            return "1.8.9";
        }

        @Override
        public String loader() {
            return "ornithe";
        }

        @Override
        public Path configDir() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public boolean legacyCombat() {
            return true;
        }

        @Override
        public boolean supportsIconFont() {
            return true;
        }
    }
}
