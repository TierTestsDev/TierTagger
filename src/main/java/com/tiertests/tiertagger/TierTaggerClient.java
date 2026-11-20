package com.tiertests.tiertagger;

import com.tiertests.tiertagger.core.Log;
import com.tiertests.tiertagger.core.Platform;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.gui.BaseScreen;
import com.tiertests.tiertagger.gui.ConfigScreen;
import com.tiertests.tiertagger.gui.LookupHud;
import com.tiertests.tiertagger.gui.Skins;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/** Loader-independent client logic: startup, key handling and lookups. Loader entrypoints call into this. */
public final class TierTaggerClient {
    public static final String MOD_ID = "tiertag";
    public static final double LOOKUP_RANGE = 64;
    private static final Logger LOGGER = LoggerFactory.getLogger("Tier Tagger");

    private static KeyMapping configKey;
    private static KeyMapping lookupKey;
    private static KeyMapping lastHitKey;
    private static UUID lastHit;

    private TierTaggerClient() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void init(Platform platform, KeyMapping config, KeyMapping lookup, KeyMapping lastHitLookup) {
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
        configKey = config;
        lookupKey = lookup;
        lastHitKey = lastHitLookup;
        TierTagger.start(platform);
    }

    public static void tick(Minecraft minecraft) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null) return;
        tagger.tick();

        while (configKey.consumeClick()) {
            BaseScreen.show(new ConfigScreen(null));
        }
        while (lookupKey.consumeClick()) {
            AbstractClientPlayer target = nearestPlayer(minecraft);
            if (target != null) LookupHud.lookup(target.getUUID(), target.getScoreboardName());
        }
        while (lastHitKey.consumeClick()) {
            AbstractClientPlayer target = findPlayer(minecraft, lastHit);
            if (target != null) LookupHud.lookup(target.getUUID(), target.getScoreboardName());
        }
    }

    public static void onAttack(Entity target) {
        if (target instanceof Player player) lastHit = player.getUUID();
    }

    public static void onDisconnect() {
        lastHit = null;
        Skins.clear();
    }

    public static void shutdown() {
        TierTagger tagger = TierTagger.get();
        if (tagger != null) tagger.shutdown();
    }

    private static AbstractClientPlayer nearestPlayer(Minecraft minecraft) {
        LocalPlayer self = minecraft.player;
        if (self == null || minecraft.level == null) return null;
        AbstractClientPlayer nearest = null;
        double best = LOOKUP_RANGE * LOOKUP_RANGE;
        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (player == self || !player.isAlive()) continue;
            double distance = player.distanceToSqr(self);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static AbstractClientPlayer findPlayer(Minecraft minecraft, UUID uuid) {
        if (uuid == null || minecraft.level == null || minecraft.player == null) return null;
        for (AbstractClientPlayer player : minecraft.level.players()) {
            if (player.getUUID().equals(uuid) && player.isAlive()
                    && player.distanceToSqr(minecraft.player) <= LOOKUP_RANGE * LOOKUP_RANGE) {
                return player;
            }
        }
        return null;
    }
}
