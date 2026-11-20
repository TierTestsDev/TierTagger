package com.tiertests.tiertagger.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.source.TierList;
import com.tiertests.tiertagger.core.text.Text;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Map;

/** Looks a username up on every tier list at once, laid out like the vanilla list screens. */
public final class SearchScreen extends BaseScreen {
    private static final int HEADER = 48;
    private static final int FOOTER = 33;
    private static final int ROW = 12;
    private static final int GRAY = 0xFFA0A0A0;

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
    private boolean searchOnOpen;

    public SearchScreen(Screen parent) {
        this(parent, null);
    }

    /** Opens with {@code player} already searched. */
    public SearchScreen(Screen parent, String player) {
        super(Component.literal("Player Search"), parent);
        if (player != null) query = player;
        searchOnOpen = player != null;
    }

    @Override
    protected void init() {
        int fieldW = 200;
        int buttonW = 60;
        int left = width / 2 - (fieldW + 4 + buttonW) / 2;
        field = new EditBox(font, left, 22, fieldW, 20, Component.literal("Username"));
        field.setMaxLength(16);
        field.setValue(query);
        field.setHint(Component.literal("Username").withColor(0x808080));
        addRenderableWidget(field);
        setInitialFocus(field);
        addRenderableWidget(Button.builder(Component.literal("Search"), b -> search())
                .bounds(left + fieldW + 4, 22, buttonW, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - FOOTER + 7, 200, 20).build());
        if (searchOnOpen) {
            searchOnOpen = false;
            search();
        }
    }

    @Override
    protected boolean onKey(int keyCode) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            search();
            return true;
        }
        return false;
    }

    private void search() {
        String name = field.getValue().trim();
        if (name.isEmpty()) return;
        if (!Strings.isValidUsername(name)) {
            status = "That is not a valid username.";
            result = null;
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
            if (error != null) status = "Could not reach the tier lists. Try again later.";
            else if (found == null) status = "No Minecraft account is called " + name + ".";
            else if (found.isEmpty()) status = found.name() + " is not ranked on any tier list.";
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
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 18)));
        return true;
    }

    @Override
    protected void drawBackground(Gfx gfx) {
        listArea(gfx, HEADER, height - FOOTER);
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int top = HEADER;
        int bottom = height - FOOTER;
        gfx.centered(title, centerX, 8, Theme.TEXT);

        if (searching) {
            gfx.centered(Component.literal("Searching" + ".".repeat(ticks / 6 % 4)), centerX, top + 16, GRAY);
            return;
        }
        if (!status.isEmpty()) {
            gfx.centered(Component.literal(status), centerX, top + 16, result == null && !query.isEmpty() ? 0xFFFF5555 : GRAY);
            return;
        }
        if (result == null) {
            gfx.centered(Component.literal("Search for a player on every tier list"), centerX, top + 16, GRAY);
            return;
        }

        int contentW = Math.min(260, width - 40);
        int left = centerX - contentW / 2;
        gfx.scissor(0, top, width, bottom);
        int y = top + 8 - scroll;
        y = drawPlayer(gfx, left, y, contentW) + 12;
        for (Map.Entry<TierList, PlayerProfile> entry : result.profiles().entrySet()) {
            y = drawList(gfx, left, y, contentW, entry.getKey(), entry.getValue()) + 10;
        }
        gfx.endScissor();

        maxScroll = Math.max(0, y + scroll - bottom);
        if (maxScroll > 0) {
            int x = left + contentW + 8;
            int track = bottom - top;
            int bar = Math.max(24, track * track / (track + maxScroll));
            int barY = top + (int) ((float) scroll / maxScroll * (track - bar));
            gfx.rect(x, top, 6, track, 0xFF000000);
            gfx.rect(x, barY, 6, bar, 0xFF808080);
            gfx.rect(x, barY, 5, bar - 1, 0xFFC0C0C0);
        }
    }

    private int drawPlayer(Gfx gfx, int x, int y, int w) {
        Skins.face(gfx, result.uuid(), x, y, 32);
        gfx.text(result.name(), x + 38, y + 4, Theme.TEXT, true);
        int count = result.profiles().size();
        gfx.text("Ranked on " + count + " tier list" + (count == 1 ? "" : "s"), x + 38, y + 18, GRAY, false);
        return y + 32;
    }

    private int drawList(Gfx gfx, int x, int y, int w, TierList list, PlayerProfile profile) {
        int centerX = x + w / 2;
        Component title = Component.literal(list.displayName()).withStyle(s -> s.withBold(true));
        if (list.logo() != 0) {
            Component logo = Components.icon(list.logo());
            int total = gfx.width(logo) + 3 + gfx.width(title);
            gfx.text(logo, centerX - total / 2, y - 2, Theme.TEXT, false);
            gfx.text(title, centerX - total / 2 + gfx.width(logo) + 3, y, Theme.TEXT, true);
        } else {
            gfx.centered(title, centerX, y, Theme.TEXT);
        }
        y += 12;

        StringBuilder meta = new StringBuilder();
        if (profile.region() != null) meta.append(profile.region().code());
        if (profile.rank() > 0) meta.append(meta.length() > 0 ? "  ·  " : "").append('#').append(profile.rank());
        if (profile.points() > 0) meta.append(meta.length() > 0 ? "  ·  " : "").append(formatPoints(profile.points())).append(" points");
        if (meta.length() > 0) {
            gfx.centered(Component.literal(meta.toString()), centerX, y, GRAY);
            y += 12;
        }
        y += 3;

        for (Map.Entry<GameMode, Ranking> entry : profile.rankings().entrySet()) {
            drawRow(gfx, x, y, w, entry.getKey(), entry.getValue());
            y += ROW;
        }
        return y;
    }

    private void drawRow(Gfx gfx, int x, int y, int w, GameMode mode, Ranking ranking) {
        Text.Builder label = Text.builder();
        tagger.tagBuilder().appendIcon(label, mode, tagger.config().icons);
        label.append(mode.title(), 0xFFFFFF);
        gfx.text(Components.of(label.build()), x, y, Theme.TEXT, true);

        Text.Builder tier = Text.builder();
        if (ranking.hasHigherPeak()) {
            tier.append("peak ", 0x808080).append(ranking.peak().format(tagger.config().tierFormat), 0x808080).append("  ", -1);
        }
        tagger.tagBuilder().appendTier(tier, ranking, tagger.config().tierFormat);
        Component tierText = Components.of(tier.build());
        gfx.text(tierText, x + w - gfx.width(tierText), y, Theme.TEXT, true);
    }

    private static String formatPoints(double points) {
        return points == Math.rint(points) ? String.valueOf((long) points) : String.valueOf(points);
    }
}
