package com.tiertests.tiertagger.gui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws player faces and front-facing bodies from the skin textures the game already manages.
 * Players in the tab list resolve instantly; anyone else is resolved once through the session service.
 */
public final class Skins {
    private static final int SKIN_SIZE = 64;
    private static final int BODY_W = 16;
    private static final int BODY_H = 32;

    private static final long RETRY_MS = 60_000;

    private static final Map<UUID, PlayerSkin> resolved = new ConcurrentHashMap<>();
    /** Players being resolved (value 0) or waiting to retry after a failed lookup (value = retry time). */
    private static final Map<UUID, Long> pending = new ConcurrentHashMap<>();

    private Skins() {
    }

    public static boolean has(UUID uuid) {
        return skin(uuid) != null;
    }

    public static void face(Gfx gfx, UUID uuid, int x, int y, int size) {
        PlayerSkin skin = skin(uuid);
        if (skin == null) return;
        Identifier texture = texture(skin);
        gfx.push();
        gfx.translate(x, y);
        gfx.scale(size / 8f);
        part(gfx, texture, 0, 0, 8, 8, 8, 8);
        part(gfx, texture, 0, 0, 8, 8, 40, 8);
        gfx.pop();
    }

    /** Front view of the whole body, scaled to fit the box and centered in it. */
    public static void body(Gfx gfx, UUID uuid, int x, int y, int maxW, int maxH) {
        PlayerSkin skin = skin(uuid);
        if (skin == null) return;
        Identifier texture = texture(skin);
        int arm = slim(skin) ? 3 : 4;
        float scale = Math.min((float) maxW / BODY_W, (float) maxH / BODY_H);

        gfx.push();
        gfx.translate(x + (maxW - BODY_W * scale) / 2f, y + (maxH - BODY_H * scale) / 2f);
        gfx.scale(scale);
        part(gfx, texture, 4, 0, 8, 8, 8, 8);
        part(gfx, texture, 4, 8, 8, 12, 20, 20);
        part(gfx, texture, 4 - arm, 8, arm, 12, 44, 20);
        part(gfx, texture, 12, 8, arm, 12, 36, 52);
        part(gfx, texture, 4, 20, 4, 12, 4, 20);
        part(gfx, texture, 8, 20, 4, 12, 20, 52);
        part(gfx, texture, 4, 0, 8, 8, 40, 8);
        part(gfx, texture, 4, 8, 8, 12, 20, 36);
        part(gfx, texture, 4 - arm, 8, arm, 12, 44, 36);
        part(gfx, texture, 12, 8, arm, 12, 52, 52);
        part(gfx, texture, 4, 20, 4, 12, 4, 36);
        part(gfx, texture, 8, 20, 4, 12, 4, 52);
        gfx.pop();
    }

    private static void part(Gfx gfx, Identifier texture, int x, int y, int w, int h, int u, int v) {
        gfx.texture(texture, x, y, w, h, u, v, w, h, SKIN_SIZE, SKIN_SIZE, 0xFFFFFFFF);
    }

    private static Identifier texture(PlayerSkin skin) {
        //? if >=1.21.9 {
        return skin.body().texturePath();
        //?} else
        /*return skin.texture();*/
    }

    private static boolean slim(PlayerSkin skin) {
        //? if >=1.21.9 {
        return skin.model() == PlayerModelType.SLIM;
        //?} else
        /*return skin.model() == PlayerSkin.Model.SLIM;*/
    }

    /** The skin if known, the default skin if it could not be looked up, or null while it loads. */
    public static PlayerSkin skin(UUID uuid) {
        if (uuid == null) return null;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(uuid);
        if (info != null) return info.getSkin();
        PlayerSkin known = resolved.get(uuid);
        if (known != null) return known;
        resolve(uuid);
        Long state = pending.get(uuid);
        return state != null && state != 0L ? DefaultPlayerSkin.get(uuid) : null;
    }

    private static void resolve(UUID uuid) {
        long now = System.currentTimeMillis();
        Long state = pending.putIfAbsent(uuid, 0L);
        if (state != null && (state == 0L || now < state || !pending.replace(uuid, state, 0L))) return;
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.supplyAsync(() -> fetchProfile(minecraft, uuid))
                .thenCompose(profile -> profile == null ? CompletableFuture.completedFuture(null) : load(minecraft, profile))
                .whenComplete((result, error) -> {
                    PlayerSkin skin = unwrap(result);
                    if (skin != null) {
                        resolved.put(uuid, skin);
                        pending.remove(uuid);
                    } else {
                        pending.put(uuid, System.currentTimeMillis() + RETRY_MS);
                    }
                });
    }

    private static GameProfile fetchProfile(Minecraft minecraft, UUID uuid) {
        try {
            //? if >=1.21.9 {
            var result = minecraft.services().sessionService().fetchProfile(uuid, false);
            //?} else
            /*var result = minecraft.getMinecraftSessionService().fetchProfile(uuid, false);*/
            return result != null ? result.profile() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static CompletableFuture<?> load(Minecraft minecraft, GameProfile profile) {
        //? if >=1.21.9 {
        return minecraft.getSkinManager().get(profile);
        //?} else
        /*return minecraft.getSkinManager().getOrLoad(profile);*/
    }

    /** Older versions return the skin directly, newer ones wrap it in an Optional. */
    private static PlayerSkin unwrap(Object result) {
        if (result instanceof Optional<?> optional) {
            return optional.orElse(null) instanceof PlayerSkin skin ? skin : null;
        }
        return result instanceof PlayerSkin skin ? skin : null;
    }

    public static void clear() {
        resolved.clear();
        pending.clear();
    }
}
