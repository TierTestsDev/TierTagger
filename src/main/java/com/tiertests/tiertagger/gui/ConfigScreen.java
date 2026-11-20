package com.tiertests.tiertagger.gui;

import com.tiertests.tiertagger.TierTaggerClient;
import com.tiertests.tiertagger.core.Settings;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Settings screen: tag choices on the left, display options on the right, a live preview below. */
public final class ConfigScreen extends BaseScreen {
    private static final Identifier DISCORD_ICON = TierTaggerClient.id("textures/gui/discord.png");
    private static final Identifier REFRESH_ICON = TierTaggerClient.id("textures/gui/refresh.png");
    private static final long REFRESH_COOLDOWN_MS = 15_000;
    private static final int TOP = 26;
    private static final int GAP = 8;
    private static final int HEADER = 14;
    private static final int ROW = 22;
    private static final int BUTTON_H = 20;
    private static final int BOTTOM_BAR = 32;

    private static long lastRefresh;

    private final TierTagger tagger = TierTagger.get();
    private final Settings settings = tagger.settings();
    private final List<Runnable> labelUpdates = new ArrayList<>();
    private final List<Header> headers = new ArrayList<>();
    private final List<Button> secondaryButtons = new ArrayList<>();
    private Button refreshButton;
    private int previewTop;
    private int discordX;
    private int refreshX;
    private int actionY;

    private record Header(String title, int x, int y) {
    }

    public ConfigScreen(Screen parent) {
        super(Component.literal("Tier Tagger"), parent);
    }

    @Override
    protected void init() {
        labelUpdates.clear();
        headers.clear();
        secondaryButtons.clear();

        int columnW = Math.min(200, (width - GAP - 16) / 2);
        int leftX = width / 2 - columnW - GAP / 2;
        int rightX = width / 2 + GAP / 2;

        int y = section("Primary tag", leftX, TOP, columnW, List.of(
                option("Tier list", settings::primaryListLabel, settings::cyclePrimaryList),
                option("Gamemode", settings::primaryModeLabel, settings::cyclePrimaryMode)));

        Button secondaryList = option("Tier list", settings::secondaryListLabel, settings::cycleSecondaryList);
        Button secondaryMode = option("Gamemode", settings::secondaryModeLabel, settings::cycleSecondaryMode);
        secondaryButtons.add(secondaryList);
        secondaryButtons.add(secondaryMode);
        int leftBottom = section("Secondary tag", leftX, y + 6, columnW, List.of(
                option("Enabled", () -> onOff(config().secondary.enabled), settings::toggleSecondary),
                secondaryList, secondaryMode));

        int rightBottom = section("Display", rightX, TOP, columnW, List.of(
                option("Mode", () -> config().displayMode.label(), settings::cycleDisplayMode),
                option("Layout", () -> config().layout.label(), settings::cycleLayout),
                option("Icons", () -> config().icons.label(), settings::cycleIcons),
                option("Tiers", () -> config().tierFormat.label(), settings::cycleTierFormat),
                option("Region", () -> onOff(config().showRegion), settings::toggleRegion),
                option("Badge", () -> onOff(config().showBadge), settings::toggleBadge),
                option("Tab list", () -> onOff(config().showInTab), settings::toggleTab)));

        previewTop = Math.max(leftBottom, rightBottom) + 6;
        addActionBar();
        updateLabels();
    }

    private TaggerConfig config() {
        return tagger.config();
    }

    private int section(String title, int x, int y, int w, List<Button> buttons) {
        headers.add(new Header(title, x, y));
        int buttonY = y + HEADER;
        for (Button button : buttons) {
            button.setX(x);
            button.setY(buttonY);
            button.setWidth(w);
            addRenderableWidget(button);
            buttonY += ROW;
        }
        return buttonY;
    }

    private Button option(String label, Supplier<String> value, Runnable action) {
        Button button = Button.builder(Component.empty(), b -> {
            action.run();
            updateLabels();
        }).bounds(0, 0, 100, BUTTON_H).build();
        labelUpdates.add(() -> button.setMessage(Component.literal("§7" + label + ": §f" + value.get())));
        return button;
    }

    private void addActionBar() {
        String update = tagger.availableUpdate();
        int searchW = 100;
        int iconW = 22;
        int updateW = 90;
        int total = searchW + 4 + iconW + 4 + iconW + (update != null ? updateW + 4 : 0);
        int x = width / 2 - total / 2;
        actionY = height - 26;

        addRenderableWidget(Button.builder(Component.literal("Search player"), b -> open(new SearchScreen(this)))
                .bounds(x, actionY, searchW, BUTTON_H).build());
        x += searchW + 4;

        refreshX = x;
        refreshButton = addRenderableWidget(Button.builder(Component.empty(), b -> refresh())
                .bounds(x, actionY, iconW, BUTTON_H).build());
        x += iconW + 4;

        if (update != null) {
            addRenderableWidget(Button.builder(Component.literal("§eUpdate to " + update),
                    b -> openLink(TierTagger.MODRINTH_URL))
                    .bounds(x, actionY, updateW, BUTTON_H).build());
            x += updateW + 4;
        }

        discordX = x;
        addRenderableWidget(Button.builder(Component.empty(), b -> openDiscord())
                .bounds(x, actionY, iconW, BUTTON_H).build());
    }

    private void updateLabels() {
        labelUpdates.forEach(Runnable::run);
        for (Button button : secondaryButtons) button.active = config().secondary.enabled;
        if (refreshButton != null) refreshButton.active = !refreshCoolingDown();
    }

    private void refresh() {
        if (refreshCoolingDown()) return;
        lastRefresh = System.currentTimeMillis();
        tagger.refreshAll();
    }

    private static boolean refreshCoolingDown() {
        return System.currentTimeMillis() - lastRefresh < REFRESH_COOLDOWN_MS;
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
    protected void draw(Gfx gfx, int mouseX, int mouseY, float delta) {
        gfx.centered(Component.literal("§lTier Tagger"), width / 2, 9, Theme.ACCENT);
        gfx.text("v" + tagger.platform().modVersion(), 4, height - 12, Theme.TEXT_MUTED, false);
        for (Header header : headers) {
            gfx.text(Component.literal("§l" + header.title().toUpperCase()), header.x() + 1, header.y() + 2, Theme.TEXT, true);
        }

        int iconY = actionY + (BUTTON_H - 14) / 2;
        gfx.texture(DISCORD_ICON, discordX + 4, iconY, 14, 14, 0, 0, 128, 128, 128, 128, 0xFFFFFFFF);
        int tint = refreshCoolingDown() ? 0xFF777777 : 0xFFFFFFFF;
        gfx.texture(REFRESH_ICON, refreshX + 6, actionY + 5, 10, 10, 0, 0, 10, 10, 10, 10, tint);

        drawPreview(gfx);
    }

    private void drawPreview(Gfx gfx) {
        LocalPlayer player = minecraft.player;
        int bottom = height - BOTTOM_BAR;
        if (player == null || bottom - previewTop < 14) return;

        String name = player.getScoreboardName();
        Component plain = Component.literal(name).withColor(0x55FF55);
        Component tagged = Components.decorate(player.getUUID(), name, plain, false);
        boolean loading = tagged == null && !tagger.store().entry(tagger.primaryList(), player.getUUID(), name).isResolved();
        Component nametag = tagged != null ? tagged : loading ? Component.literal("Loading...").withColor(0xAAAAAA) : plain;

        int centerX = width / 2;
        int textW = gfx.width(nametag);
        float scale = textW * 1.5f + 12 <= width - 32 ? 1.5f : 1f;
        int boxW = (int) (textW * scale) + 12;
        int boxH = (int) (9 * scale) + 6;
        gfx.rect(centerX - boxW / 2, previewTop, boxW, boxH, 0x40000000);
        gfx.push();
        gfx.translate(centerX, previewTop + 3);
        gfx.scale(scale);
        gfx.text(nametag, -textW / 2, 0, Theme.TEXT, true);
        gfx.pop();

        int skinTop = previewTop + boxH + 4;
        if (bottom - skinTop >= 40) Skins.body(gfx, player.getUUID(), centerX - 40, skinTop, 80, bottom - skinTop - 2);
    }

    private static String onOff(boolean value) {
        return value ? "§aOn" : "§cOff";
    }
}
