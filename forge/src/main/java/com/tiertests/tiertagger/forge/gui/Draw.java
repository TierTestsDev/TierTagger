package com.tiertests.tiertagger.forge.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Drawing helpers for the 1.8.9 screens, including skins for players that are not in the tab list. */
public final class Draw {
    public static final int ACCENT = 0xFF6C5CE7;
    public static final int ACCENT_DIM = 0xFF3D3570;
    public static final int ERROR = 0xFFE74C3C;
    public static final int PANEL = 0xE01A1A2E;
    public static final int PILL = 0xCC2A2A4A;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFF888899;

    private static final Map<UUID, ResourceLocation> skins = new ConcurrentHashMap<UUID, ResourceLocation>();
    private static final long RETRY_MS = 60_000;
    /** When each unresolved skin may be looked up again. */
    private static final Map<UUID, Long> retryAt = new ConcurrentHashMap<UUID, Long>();

    private Draw() {
    }

    public static FontRenderer font() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    public static void rect(int x, int y, int w, int h, int argb) {
        Gui.drawRect(x, y, x + w, y + h, argb);
    }

    public static void border(int x, int y, int w, int h, int argb) {
        rect(x, y, w, 1, argb);
        rect(x, y + h - 1, w, 1, argb);
        rect(x, y + 1, 1, h - 2, argb);
        rect(x + w - 1, y + 1, 1, h - 2, argb);
    }

    public static void text(String text, int x, int y, int argb, boolean shadow) {
        font().drawString(text, x, y, argb, shadow);
    }

    public static void centered(String text, int centerX, int y, int argb) {
        text(text, centerX - font().getStringWidth(text) / 2, y, argb, true);
    }

    public static int width(String text) {
        return font().getStringWidth(text);
    }

    public static void face(UUID uuid, String name, int x, int y, int size) {
        ResourceLocation skin = skin(uuid, name);
        if (skin == null) return;
        bind(skin);
        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
    }

    /** Front view of a body scaled into the box; arms are drawn 4 pixels wide for every model. */
    public static void body(UUID uuid, String name, int x, int y, int maxW, int maxH) {
        ResourceLocation skin = skin(uuid, name);
        if (skin == null) return;
        float scale = Math.min(maxW / 16f, maxH / 32f);
        bind(skin);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + (maxW - 16 * scale) / 2f, y + (maxH - 32 * scale) / 2f, 0);
        GlStateManager.scale(scale, scale, 1);
        part(4, 0, 8, 8, 8, 8);
        part(4, 8, 8, 12, 20, 20);
        part(0, 8, 4, 12, 44, 20);
        part(12, 8, 4, 12, 36, 52);
        part(4, 20, 4, 12, 4, 20);
        part(8, 20, 4, 12, 20, 52);
        part(4, 0, 8, 8, 40, 8);
        GlStateManager.popMatrix();
    }

    private static void part(int x, int y, int w, int h, int u, int v) {
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, 64, 64);
    }

    private static void bind(ResourceLocation texture) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    private static ResourceLocation skin(UUID uuid, String name) {
        if (uuid == null) return null;
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient handler = mc.getNetHandler();
        NetworkPlayerInfo info = handler == null ? null : handler.getPlayerInfo(uuid);
        if (info != null) return info.getLocationSkin();
        ResourceLocation known = skins.get(uuid);
        Long next = retryAt.get(uuid);
        if (known != null || next != null && System.currentTimeMillis() < next) return known;
        retryAt.put(uuid, System.currentTimeMillis() + RETRY_MS);
        final GameProfile profile = new GameProfile(uuid, name);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final GameProfile filled = Minecraft.getMinecraft().getSessionService().fillProfileProperties(profile, false);
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures =
                                Minecraft.getMinecraft().getSkinManager().loadSkinFromCache(filled);
                        MinecraftProfileTexture texture = textures.get(MinecraftProfileTexture.Type.SKIN);
                        if (texture != null) {
                            skins.put(filled.getId(), Minecraft.getMinecraft().getSkinManager()
                                    .loadSkin(texture, MinecraftProfileTexture.Type.SKIN));
                        }
                    }
                });
            }
        }, "TierTagger-Skin").start();
        return null;
    }
}
