package com.tiertests.tiertagger.forge;

import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tags tab list names through the display name every tab entry already has, so no class patching is needed.
 * The server's own display name is remembered and restored when the tag goes away, and a new name from
 * the server simply replaces ours until the next update re-applies the tag.
 */
final class TabNames {
    private static final class Applied {
        final Tag tag;
        final String base;
        final IChatComponent ours;
        final IChatComponent original;

        Applied(Tag tag, String base, IChatComponent ours, IChatComponent original) {
            this.tag = tag;
            this.base = base;
            this.ours = ours;
            this.original = original;
        }
    }

    private final Map<UUID, Applied> applied = new HashMap<UUID, Applied>();

    void update(Minecraft mc, TierTagger tagger) {
        NetHandlerPlayClient handler = mc.getNetHandler();
        if (handler == null) return;
        boolean enabled = tagger.config().showInTab;
        for (NetworkPlayerInfo info : handler.getPlayerInfoMap()) {
            UUID uuid = info.getGameProfile().getId();
            if (uuid == null) continue;
            Applied previous = applied.get(uuid);
            IChatComponent current = info.getDisplayName();
            if (previous != null && current != previous.ours) previous = null;
            IChatComponent original = previous != null ? previous.original : current;

            Tag tag = enabled ? tagger.tag(uuid, info.getGameProfile().getName(), true) : null;
            if (tag == null || tag.isEmpty()) {
                if (previous != null) info.setDisplayName(previous.original);
                applied.remove(uuid);
                continue;
            }
            String base = original != null ? original.getFormattedText()
                    : ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(), info.getGameProfile().getName());
            if (previous != null && previous.tag == tag && previous.base.equals(base)) continue;

            IChatComponent ours = new ChatComponentText(tag.decorateLegacy(base));
            info.setDisplayName(ours);
            applied.put(uuid, new Applied(tag, base, ours, original));
        }
    }

    void clear() {
        applied.clear();
    }
}
