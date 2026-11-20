package com.tiertests.tiertagger.forge;

import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import com.tiertests.tiertagger.forge.gui.LegacyConfigScreen;
import com.tiertests.tiertagger.forge.gui.LegacyLookupHud;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hooks Tier Tagger into Forge's own events. Nametags go through {@link PlayerEvent.NameFormat}, which
 * 1.8.9 only fires when a player's name is refreshed, so the tick handler refreshes a name whenever its tag changes.
 */
public final class ForgeEvents {
    private static final double LOOKUP_RANGE_SQ = 64 * 64;
    private static final int REFRESH_INTERVAL = 10;

    private final Map<UUID, Tag> appliedTags = new HashMap<UUID, Tag>();
    private final TabNames tabNames = new TabNames();
    private UUID lastHit;
    private int ticks;

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        EntityPlayer player = event.entityPlayer;
        TierTagger tagger = TierTagger.get();
        if (tagger == null || player.worldObj == null || !player.worldObj.isRemote) return;
        Tag tag = tagger.tag(player.getUniqueID(), event.username, false);
        appliedTags.put(player.getUniqueID(), tag);
        if (tag != null) event.displayname = tag.decorateLegacy(event.displayname);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        TierTagger tagger = TierTagger.get();
        Minecraft mc = Minecraft.getMinecraft();
        if (tagger == null) return;
        tagger.tick();
        handleKeys(mc);

        if (mc.theWorld == null || ++ticks % REFRESH_INTERVAL != 0) return;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            UUID uuid = player.getUniqueID();
            Tag tag = tagger.tag(uuid, player.getName(), false);
            if (!appliedTags.containsKey(uuid) || appliedTags.get(uuid) != tag) player.refreshDisplayName();
        }
        tabNames.update(mc, tagger);
    }

    private void handleKeys(Minecraft mc) {
        while (TierTaggerForge.configKey.isPressed()) {
            mc.displayGuiScreen(new LegacyConfigScreen(null));
        }
        while (TierTaggerForge.lookupKey.isPressed()) {
            EntityPlayer target = nearest(mc);
            if (target != null) LegacyLookupHud.lookup(target.getUniqueID(), target.getName());
        }
        while (TierTaggerForge.lastHitKey.isPressed()) {
            EntityPlayer target = lastHit == null || mc.theWorld == null ? null : mc.theWorld.getPlayerEntityByUUID(lastHit);
            if (target != null && mc.thePlayer != null && target.getDistanceSqToEntity(mc.thePlayer) <= LOOKUP_RANGE_SQ) {
                LegacyLookupHud.lookup(target.getUniqueID(), target.getName());
            }
        }
    }

    private static EntityPlayer nearest(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        EntityPlayer nearest = null;
        double best = LOOKUP_RANGE_SQ;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead) continue;
            double distance = player.getDistanceSqToEntity(mc.thePlayer);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer.worldObj.isRemote && event.target instanceof EntityPlayer) {
            lastHit = event.target.getUniqueID();
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) LegacyLookupHud.render();
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                appliedTags.clear();
                tabNames.clear();
                lastHit = null;
            }
        });
    }
}
