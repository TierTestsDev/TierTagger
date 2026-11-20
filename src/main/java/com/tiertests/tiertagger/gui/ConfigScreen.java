package com.tiertests.tiertagger.gui;

import com.tiertests.tiertagger.TierTaggerClient;
import com.tiertests.tiertagger.core.Settings;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Settings in the style of the vanilla option screens, with a preview of your own nametag. */
public final class ConfigScreen extends BaseScreen {
    private static final Identifier DISCORD_ICON = TierTaggerClient.id("textures/gui/discord.png");
    private static final int HEADER = 33;
    private static final int FOOTER = 33;
    private static final int BUTTON_W = 150;
    private static final int GAP = 10;
    private static final int ROW = 24;

    private final TierTagger tagger = TierTagger.get();
    private final Settings settings = tagger.settings();
    private final List<Runnable> labelUpdates = new ArrayList<>();
    private Button secondaryMode;
    private int optionsBottom;
    private int discordX;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Tier Tagger"), parent);
    }

    @Override
    protected void init() {
        labelUpdates.clear();
        int left = width / 2 - BUTTON_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        int y = HEADER + 8;

        add(left, y, cycle("Tier List", settings::primaryListLabel, settings::cyclePrimaryList,
                "Which tier list the tag shows."));
        add(right, y, cycle("Gamemode", settings::primaryModeLabel, settings::cyclePrimaryMode, MODE_TIP));
        y += ROW;
        add(left, y, cycle("Second Tag", settings::secondaryListLabel, settings::cycleSecondaryList,
                "Show a second tier list next to the first one."));
        secondaryMode = add(right, y, cycle("Second Gamemode", settings::secondaryModeLabel, settings::cycleSecondaryMode,
                MODE_TIP));
        y += ROW;
        add(left, y, cycle("Position", settings::layoutLabel, settings::cycleLayout,
                "Where tiers go: before the name, after it, or one tag on each side."));
        add(right, y, toggle("Icons", settings::iconsOn, settings::toggleIcons, "Gamemode icons next to tiers."));
        y += ROW;
        add(left, y, toggle("Region", settings::regionOn, settings::toggleRegion, "The player's region, like NA or EU."));
        add(right, y, toggle("Badge", settings::badgeOn, settings::toggleBadge, "The Tier Tests badge next to tiers."));
        y += ROW;
        add(left, y, toggle("Tab List", settings::tabOn, settings::toggleTab, "Show tiers in the player list too."));
        optionsBottom = y + 20;

        int footerY = height - FOOTER + 7;
        addRenderableWidget(Button.builder(Component.literal("Search Player"), b -> open(new SearchScreen(this)))
                .bounds(left, footerY, BUTTON_W, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(right, footerY, BUTTON_W, 20).build());
        discordX = width - 24;
        addRenderableWidget(Button.builder(Component.empty(), b -> openDiscord())
                .bounds(discordX, footerY, 20, 20).tooltip(Tooltip.create(Component.literal("Tier Tests Discord"))).build());

        String update = tagger.availableUpdate();
        if (update != null) {
            addRenderableWidget(Button.builder(Component.literal("Update to " + update).withColor(0xFFFF55),
                    b -> openLink(TierTagger.MODRINTH_URL)).bounds(width - 104, 6, 100, 20).build());
        }
        updateLabels();
    }

    private static final String MODE_TIP = "Best tier shows a player's highest tier. A gamemode shows their tier in it, "
            + "or their best tier if they are not ranked in it. Rank shows their overall position.";

    private Button add(int x, int y, Button button) {
        button.setX(x);
        button.setY(y);
        return addRenderableWidget(button);
    }

    private Button cycle(String name, Supplier<String> value, Runnable action, String tip) {
        return option(() -> Component.literal(value.get()), name, action, tip);
    }

    private Button toggle(String name, BooleanSupplier value, Runnable action, String tip) {
        return option(() -> value.getAsBoolean() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF, name, action, tip);
    }

    private Button option(Supplier<Component> value, String name, Runnable action, String tip) {
        Button button = Button.builder(Component.empty(), b -> {
            action.run();
            updateLabels();
        }).bounds(0, 0, BUTTON_W, 20).tooltip(Tooltip.create(Component.literal(tip))).build();
        labelUpdates.add(() -> button.setMessage(CommonComponents.optionNameValue(Component.literal(name), value.get())));
        return button;
    }

    private void updateLabels() {
        labelUpdates.forEach(Runnable::run);
        if (secondaryMode != null) secondaryMode.active = settings.secondaryEnabled();
    }

    private void openDiscord() {
        String url = tagger.discordInvite();
        if (TierTagger.isTrustedInvite(url)) openLink(url);
    }

    @Override
    public void tick() {
        updateLabels();
    }

    @Override
    protected void drawBackground(Gfx gfx) {
        listArea(gfx, HEADER, height - FOOTER);
    }

    @Override
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.centered(title, width / 2, (HEADER - 9) / 2, Theme.TEXT);
        gfx.texture(DISCORD_ICON, discordX + 3, height - FOOTER + 10, 14, 14, 0, 0, 128, 128, 128, 128, 0xFFFFFFFF);
        drawPreview(gfx);
    }

    /** Your own nametag on your skin, so changes show up right away. */
    private void drawPreview(Gfx gfx) {
        LocalPlayer player = minecraft.player;
        int top = optionsBottom + 10;
        int bottom = height - FOOTER - 6;
        if (player == null || bottom - top < 12) return;

        String name = player.getScoreboardName();
        Component plain = Component.literal(name);
        Component tagged = Components.decorate(player.getUUID(), name, plain, false);
        boolean loading = tagged == null && !tagger.store().entry(tagger.primaryList(), player.getUUID(), name).isResolved();
        Component nametag = tagged != null ? tagged : loading ? Component.literal("Loading...").withColor(0xAAAAAA) : plain;

        int centerX = width / 2;
        int bodyH = Math.min(96, bottom - top - 12) & ~1;
        int tagY = bodyH >= 32 ? top : top + (bottom - top - 10) / 2;
        int textW = gfx.width(nametag);
        gfx.rect(centerX - textW / 2 - 2, tagY - 1, textW + 4, 10, 0x40000000);
        gfx.text(nametag, centerX - textW / 2, tagY, Theme.TEXT, false);
        if (bodyH >= 32) Skins.body(gfx, player.getUUID(), centerX - bodyH / 4, tagY + 12, bodyH / 2, bodyH);
    }
}
