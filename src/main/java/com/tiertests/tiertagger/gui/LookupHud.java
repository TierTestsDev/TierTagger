package com.tiertests.tiertagger.gui;

import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.Colors;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.source.TierList;
import com.tiertests.tiertagger.core.text.Text;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The popup in the top-left corner that shows one player's tiers after pressing a lookup key. */
public final class LookupHud {
    private static final long SHOW_MS = 6000;
    private static final long COOLDOWN_MS = 500;
    private static final int MARGIN = 6;
    private static final int PADDING = 8;
    private static final int SKIN_AREA = 46;
    private static final int LINE = 12;
    private static final int BOTTOM_CLEARANCE = 50;

    private static UUID uuid;
    private static String name;
    private static LookupResult result;
    private static boolean loading;
    private static long shownAt;
    private static long requestedAt;
    private static int generation;

    private LookupHud() {
    }

    public static void lookup(UUID target, String targetName) {
        long now = System.currentTimeMillis();
        if (now - requestedAt < COOLDOWN_MS) return;
        TierTagger tagger = TierTagger.get();
        if (tagger == null) return;
        requestedAt = now;
        final int request = ++generation;
        uuid = target;
        name = targetName;
        result = null;
        loading = true;
        shownAt = now;
        Skins.skin(target);
        tagger.lookup(target, targetName).whenComplete((found, error) -> Minecraft.getInstance().execute(() -> {
            if (request != generation) return;
            result = found;
            loading = false;
            shownAt = System.currentTimeMillis();
        }));
    }

    public static void render(Gfx gfx) {
        if (uuid == null) return;
        if (loading) {
            panel(gfx, Component.literal("Looking up ").withColor(0xFFFFFF)
                    .append(Component.literal(name).withColor(0xFFFF55)).append("..."), Theme.ACCENT, -1);
            return;
        }
        long elapsed = System.currentTimeMillis() - shownAt;
        if (elapsed > SHOW_MS) {
            uuid = null;
            result = null;
            return;
        }
        float remaining = 1f - (float) elapsed / SHOW_MS;
        Map.Entry<TierList, PlayerProfile> entry = result == null ? null : result.first();
        if (entry == null) {
            panel(gfx, Component.literal(name).withColor(0xFF5555)
                    .append(Component.literal(" - no tiers found").withColor(0xAAAAAA)), Theme.ERROR, remaining);
            return;
        }
        details(gfx, entry.getKey(), entry.getValue(), remaining);
    }

    private static void panel(Gfx gfx, Component text, int accent, float remaining) {
        int width = Math.max(160, gfx.width(text) + PADDING * 2);
        int height = 30;
        gfx.rect(MARGIN, MARGIN, width, height, Theme.PANEL);
        gfx.border(MARGIN, MARGIN, width, height, accent);
        gfx.text(text, MARGIN + PADDING, MARGIN + 11, Theme.TEXT, true);
        if (remaining >= 0) gfx.rect(MARGIN, MARGIN + height - 2, (int) (width * remaining), 2, accent);
    }

    private static void details(Gfx gfx, TierList list, PlayerProfile profile, float remaining) {
        TierTagger tagger = TierTagger.get();
        Component title = Component.literal(result.name() != null ? result.name() : name).withColor(0x55FF55);
        if (tagger.config().showRegion && profile.region() != null) {
            title = title.copy().append(Component.literal(" [" + profile.region().code() + "]").withColor(0x777777));
        }
        String rankText = profile.rank() > 0 ? "#" + profile.rank() + " " : "";
        Component subtitle = Component.literal(rankText + "(" + list.displayName() + ")").withColor(0xAAAAAA);

        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int maxLines = Math.max(1, (screenHeight - BOTTOM_CLEARANCE - MARGIN - PADDING * 2 - 28) / LINE);
        List<Component> lines = new ArrayList<>();
        int total = profile.rankings().size();
        for (Map.Entry<GameMode, Ranking> ranking : profile.rankings().entrySet()) {
            if (lines.size() == maxLines - 1 && total > maxLines) {
                lines.add(Component.literal("+" + (total - lines.size()) + " more").withColor(0x777777));
                break;
            }
            lines.add(line(tagger, ranking.getKey(), ranking.getValue()));
        }

        int textWidth = Math.max(130, Math.max(gfx.width(title), gfx.width(subtitle)));
        for (Component line : lines) textWidth = Math.max(textWidth, gfx.width(line));
        int width = SKIN_AREA + textWidth + PADDING + 4;
        int height = Math.max(70, PADDING * 2 + 24 + lines.size() * LINE + 4);
        int x = MARGIN;
        int y = MARGIN;

        gfx.rect(x, y, width, height, Theme.PANEL);
        gfx.border(x, y, width, height, Theme.ACCENT);
        Skins.body(gfx, uuid, x + 4, y + 6, SKIN_AREA - 8, height - 16);

        int textX = x + SKIN_AREA;
        gfx.text(title, textX, y + PADDING, Theme.TEXT, true);
        gfx.text(subtitle, textX, y + PADDING + 11, Theme.TEXT, true);
        int lineY = y + PADDING + 24;
        for (Component line : lines) {
            gfx.text(line, textX, lineY, Theme.TEXT, false);
            lineY += LINE;
        }
        gfx.rect(x, y + height - 2, (int) (width * remaining), 2, Theme.ACCENT);
    }

    static Component line(TierTagger tagger, GameMode mode, Ranking ranking) {
        Text.Builder text = Text.builder();
        tagger.tagBuilder().appendIcon(text, mode, tagger.config().icons);
        text.append(mode.title() + " ", Colors.readable(mode.color()));
        tagger.tagBuilder().appendTier(text, ranking, tagger.config().tierFormat);
        if (ranking.hasHigherPeak()) {
            text.append(" (peak ", 0xAAAAAA).append(ranking.peak().format(tagger.config().tierFormat), ranking.peak().color())
                    .append(")", 0xAAAAAA);
        }
        return Components.of(text.build());
    }
}
