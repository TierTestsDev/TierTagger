package com.tiertests.tiertagger.forge.gui;

import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.Colors;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.source.TierList;
import com.tiertests.tiertagger.core.text.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/** The popup in the top-left corner that shows one player's tiers after pressing a lookup key. */
public final class LegacyLookupHud {
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

    private LegacyLookupHud() {
    }

    public static void lookup(UUID target, String targetName) {
        long now = System.currentTimeMillis();
        TierTagger tagger = TierTagger.get();
        if (tagger == null || now - requestedAt < COOLDOWN_MS) return;
        requestedAt = now;
        final int request = ++generation;
        uuid = target;
        name = targetName;
        result = null;
        loading = true;
        shownAt = now;
        tagger.lookup(target, targetName).whenComplete(new BiConsumer<LookupResult, Throwable>() {
            @Override
            public void accept(final LookupResult found, Throwable error) {
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        if (request != generation) return;
                        result = found;
                        loading = false;
                        shownAt = System.currentTimeMillis();
                    }
                });
            }
        });
    }

    public static void render() {
        if (uuid == null) return;
        if (loading) {
            panel("§fLooking up §e" + name + "§f...", Draw.ACCENT, -1);
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
            panel("§c" + name + " §7- no tiers found", Draw.ERROR, remaining);
            return;
        }
        details(entry.getKey(), entry.getValue(), remaining);
    }

    private static void panel(String text, int accent, float remaining) {
        int width = Math.max(160, Draw.width(text) + PADDING * 2);
        int height = 30;
        Draw.rect(MARGIN, MARGIN, width, height, Draw.PANEL);
        Draw.border(MARGIN, MARGIN, width, height, accent);
        Draw.text(text, MARGIN + PADDING, MARGIN + 11, Draw.TEXT, true);
        if (remaining >= 0) Draw.rect(MARGIN, MARGIN + height - 2, (int) (width * remaining), 2, accent);
    }

    private static void details(TierList list, PlayerProfile profile, float remaining) {
        TierTagger tagger = TierTagger.get();
        String title = "§a" + (result.name() != null ? result.name() : name);
        if (tagger.config().showRegion && profile.region() != null) title += " §8[" + profile.region().code() + "]";
        String subtitle = "§7" + (profile.rank() > 0 ? "#" + profile.rank() + " " : "") + "§8(" + list.displayName() + ")";

        int screenHeight = new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight();
        int maxLines = Math.max(1, (screenHeight - BOTTOM_CLEARANCE - MARGIN - PADDING * 2 - 28) / LINE);
        List<String> lines = new ArrayList<String>();
        int total = profile.rankings().size();
        for (Map.Entry<GameMode, Ranking> ranking : profile.rankings().entrySet()) {
            if (lines.size() == maxLines - 1 && total > maxLines) {
                lines.add("§8+" + (total - lines.size()) + " more");
                break;
            }
            lines.add(line(tagger, ranking.getKey(), ranking.getValue()));
        }
        int textWidth = Math.max(130, Math.max(Draw.width(title), Draw.width(subtitle)));
        for (String line : lines) textWidth = Math.max(textWidth, Draw.width(line));
        int width = SKIN_AREA + textWidth + PADDING + 4;
        int height = Math.max(70, PADDING * 2 + 24 + lines.size() * LINE + 4);

        Draw.rect(MARGIN, MARGIN, width, height, Draw.PANEL);
        Draw.border(MARGIN, MARGIN, width, height, Draw.ACCENT);
        Draw.body(uuid, name, MARGIN + 4, MARGIN + 6, SKIN_AREA - 8, height - 16);

        int textX = MARGIN + SKIN_AREA;
        Draw.text(title, textX, MARGIN + PADDING, Draw.TEXT, true);
        Draw.text(subtitle, textX, MARGIN + PADDING + 11, Draw.TEXT, true);
        int lineY = MARGIN + PADDING + 24;
        for (String line : lines) {
            Draw.text(line, textX, lineY, Draw.TEXT, false);
            lineY += LINE;
        }
        Draw.rect(MARGIN, MARGIN + height - 2, (int) (width * remaining), 2, Draw.ACCENT);
    }

    static String line(TierTagger tagger, GameMode mode, Ranking ranking) {
        Text.Builder text = Text.builder();
        tagger.tagBuilder().appendIcon(text, mode, tagger.config().icons);
        text.append(mode.title() + " ", Colors.readable(mode.color()));
        tagger.tagBuilder().appendTier(text, ranking, tagger.config().tierFormat);
        if (ranking.hasHigherPeak()) {
            text.append(" (peak ", 0xAAAAAA).append(ranking.peak().format(tagger.config().tierFormat), ranking.peak().color())
                    .append(")", 0xAAAAAA);
        }
        return text.build().toLegacyString();
    }
}
