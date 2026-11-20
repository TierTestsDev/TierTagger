package com.tiertests.tiertagger.gui;

import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.model.Tier;
import com.tiertests.tiertagger.core.source.TierList;
import com.tiertests.tiertagger.core.text.Text;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Looks a username up on every tier list at once. */
public final class SearchScreen extends BaseScreen {
    private static final int CONTENT_TOP = 34;
    private static final int PADDING = 8;
    private static final int PILL_H = 22;
    private static final int PILL_GAP = 4;
    private static final int SECTION_GAP = 12;
    private static final int CARD_HEADER = 28;

    private final TierTagger tagger = TierTagger.get();
    private EditBox field;
    private String query = "";
    private String status = "";
    private LookupResult result;
    private boolean searching;
    private int requestId;
    private int scroll;
    private int maxScroll;
    private int ticks;

    public SearchScreen(Screen parent) {
        super(Component.literal("Player search"), parent);
    }

    @Override
    protected void init() {
        int barW = Math.min(280, width - 40);
        int fieldW = barW - 76;
        int left = width / 2 - barW / 2;

        field = new EditBox(font, left + 4, 8, fieldW, 20, Component.literal("Username"));
        field.setMaxLength(16);
        field.setValue(query);
        field.setHint(Component.literal("Enter a username").withColor(0x777777));
        addRenderableWidget(field);
        setInitialFocus(field);

        addRenderableWidget(Button.builder(Component.literal("Search"), b -> search())
                .bounds(left + fieldW + 8, 8, 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal("✕"), b -> clear())
                .bounds(left + fieldW + 60, 8, 16, 20).build());
        addRenderableWidget(Button.builder(Component.literal("← Back"), b -> onClose())
                .bounds(4, 8, 50, 20).build());
    }

    @Override
    protected boolean onKey(int keyCode) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            search();
            return true;
        }
        return false;
    }

    private void clear() {
        field.setValue("");
        query = "";
        status = "";
        result = null;
        searching = false;
        scroll = 0;
        requestId++;
    }

    private void search() {
        String name = field.getValue().trim();
        if (name.isEmpty()) return;
        if (!Strings.isValidUsername(name)) {
            status = "§cThat is not a valid username.";
            return;
        }
        query = name;
        status = "";
        result = null;
        searching = true;
        scroll = 0;
        final int request = ++requestId;
        tagger.search(name).whenComplete((found, error) -> minecraft.execute(() -> {
            if (request != requestId) return;
            searching = false;
            if (error != null) status = "§cCould not reach the tier lists. Try again later.";
            else if (found == null) status = "§cNo Minecraft account is called " + name + ".";
            else if (found.isEmpty()) status = "§7" + found.name() + " is not ranked on any tier list.";
            else result = found;
            if (found != null) Skins.skin(found.uuid());
        }));
    }

    @Override
    public void tick() {
        ticks++;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 20)));
        return true;
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int contentW = Math.min(340, width - 20);
        int left = centerX - contentW / 2;

        if (searching) {
            gfx.centered("§7Searching every tier list" + ".".repeat(ticks / 5 % 4), centerX, CONTENT_TOP + 20, Theme.TEXT_DIM);
            float pulse = (float) (Math.sin(ticks * 0.3) * 0.5 + 0.5);
            gfx.rect(centerX - 40, CONTENT_TOP + 36, 80, 3, (int) (100 + 155 * pulse) << 24 | Theme.ACCENT & 0xFFFFFF);
            return;
        }
        if (!status.isEmpty()) {
            gfx.centered(status, centerX, CONTENT_TOP + 20, Theme.TEXT);
            return;
        }
        if (result == null) {
            gfx.centered("§7Search for a player above", centerX, CONTENT_TOP + 30, Theme.TEXT_DIM);
            return;
        }

        gfx.scissor(0, CONTENT_TOP, width, height);
        int y = CONTENT_TOP - scroll;
        y = drawHeader(gfx, left, y, contentW) + SECTION_GAP;
        for (Map.Entry<TierList, PlayerProfile> entry : result.profiles().entrySet()) {
            y = drawCard(gfx, left, y, contentW, entry.getKey(), entry.getValue()) + SECTION_GAP;
        }
        gfx.endScissor();

        maxScroll = Math.max(0, y + scroll - CONTENT_TOP - (height - CONTENT_TOP) + 10);
        if (maxScroll > 0) {
            int track = height - CONTENT_TOP;
            int bar = Math.max(10, track * track / (track + maxScroll));
            int barY = CONTENT_TOP + (int) ((float) scroll / maxScroll * (track - bar));
            gfx.rect(width - 4, CONTENT_TOP, 2, track, 0x20FFFFFF);
            gfx.rect(width - 4, barY, 2, bar, 0x80FFFFFF);
        }
    }

    private int drawHeader(Gfx gfx, int x, int y, int w) {
        int h = 52;
        gfx.roundRect(x, y, w, h, Theme.PANEL);
        gfx.border(x, y, w, h, Theme.ACCENT);
        Skins.face(gfx, result.uuid(), x + 6, y + 6, h - 12);

        gfx.text(Component.literal("§l" + result.name()).withColor(0x55FF55), x + 60, y + 10, Theme.TEXT, true);
        int count = result.profiles().size();
        gfx.text("§7Ranked on §f" + count + "§7 tier list" + (count == 1 ? "" : "s"), x + 60, y + 24, Theme.TEXT_DIM, false);

        Tier best = null;
        for (PlayerProfile profile : result.profiles().values()) {
            Tier tier = profile.best().getValue().tier();
            if (tier.isBetterThan(best)) best = tier;
        }
        if (best != null) {
            Component label = Component.literal("Best: ").withColor(0xAAAAAA)
                    .append(Component.literal(best.format(tagger.config().tierFormat)).withColor(best.color()));
            gfx.text(label, x + w - gfx.width(label) - PADDING, y + 14, Theme.TEXT, true);
        }
        return y + h;
    }

    private int drawCard(Gfx gfx, int x, int y, int w, TierList list, PlayerProfile profile) {
        int pillW = pillWidth(w);
        int columns = Math.max(1, (w - PADDING * 2 + PILL_GAP) / (pillW + PILL_GAP));
        int count = profile.rankings().size();
        int rows = (count + columns - 1) / columns;
        int h = CARD_HEADER + 16 + PADDING + rows * (PILL_H + PILL_GAP) + PADDING;

        gfx.roundRect(x, y, w, h, Theme.PANEL);
        gfx.border(x, y, w, h, Theme.ACCENT_DIM);
        gfx.fill(x + 1, y + 1, x + w - 1, y + CARD_HEADER, Theme.HEADER);
        gfx.fill(x + 1, y + CARD_HEADER - 1, x + w - 1, y + CARD_HEADER, Theme.ACCENT_DIM);

        int titleX = x + PADDING + 2;
        int titleY = y + (CARD_HEADER - 8) / 2;
        if (list.logo() != 0) {
            Component logo = Components.icon(list.logo());
            gfx.text(logo, titleX, titleY - 2, Theme.TEXT, false);
            titleX += gfx.width(logo) + 3;
        }
        gfx.text(Component.literal("§l" + list.displayName()), titleX, titleY, Theme.TEXT, true);

        int metaX = x + PADDING + 2;
        int metaY = y + CARD_HEADER + 3;
        if (profile.region() != null) metaX += pill(gfx, metaX, metaY, "⚑ " + profile.region().code(), Theme.REGION) + 4;
        if (profile.rank() > 0) {
            String points = profile.points() > 0 ? " §8(" + formatPoints(profile.points()) + " pts)" : "";
            pill(gfx, metaX, metaY, "✦ #" + profile.rank() + points, Theme.GOLD);
        }

        int top = metaY + 16 + 2;
        List<Map.Entry<GameMode, Ranking>> rankings = new ArrayList<>(profile.rankings().entrySet());
        for (int i = 0; i < rankings.size(); i++) {
            int pillX = x + PADDING + i % columns * (pillW + PILL_GAP);
            int pillY = top + i / columns * (PILL_H + PILL_GAP);
            drawTier(gfx, pillX, pillY, pillW, rankings.get(i).getKey(), rankings.get(i).getValue());
        }
        return y + h;
    }

    private int pill(Gfx gfx, int x, int y, String text, int color) {
        int w = gfx.width(text) + 10;
        gfx.roundRect(x, y, w, 12, Theme.PILL);
        gfx.text(text, x + 5, y + 2, color, false);
        return w;
    }

    private void drawTier(Gfx gfx, int x, int y, int w, GameMode mode, Ranking ranking) {
        int color = Theme.opaque(mode.color());
        gfx.roundRect(x, y, w, PILL_H, Theme.blend(Theme.PANEL_LIGHT, color, 0.15f));
        gfx.fill(x, y + 2, x + 2, y + PILL_H - 2, color);

        Text.Builder label = Text.builder();
        tagger.tagBuilder().appendIcon(label, mode, tagger.config().icons);
        label.append(mode.title(), mode.color());
        int textY = y + (PILL_H - 8) / 2;
        gfx.text(Components.of(label.build()), x + 6, textY, Theme.TEXT, false);

        Text.Builder tier = Text.builder();
        if (ranking.hasHigherPeak()) {
            tier.append("↑" + ranking.peak().format(tagger.config().tierFormat) + " ", 0x777777);
        }
        tagger.tagBuilder().appendTier(tier, ranking, tagger.config().tierFormat);
        Component tierText = Components.of(tier.build());
        gfx.text(tierText, x + w - gfx.width(tierText) - 6, textY, Theme.TEXT, true);
    }

    private static int pillWidth(int cardW) {
        int available = cardW - PADDING * 2;
        return available >= 2 * 120 + PILL_GAP ? (available - PILL_GAP) / 2 : available;
    }

    private static String formatPoints(double points) {
        return points == Math.rint(points) ? String.valueOf((long) points) : String.valueOf(points);
    }
}
