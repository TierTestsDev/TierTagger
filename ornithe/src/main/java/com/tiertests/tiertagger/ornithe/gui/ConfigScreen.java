package com.tiertests.tiertagger.ornithe.gui;

import com.tiertests.tiertagger.core.Settings;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.config.TaggerConfig;
import com.tiertests.tiertagger.core.text.Tag;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.game.ConfirmChatLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ConfirmationListener;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Settings screen with the same options as the Fabric version, built from vanilla 1.8.9 buttons. */
public final class ConfigScreen extends Screen implements ConfirmationListener {
    private static final int TOP = 26;
    private static final int GAP = 8;
    private static final int HEADER = 14;
    private static final int ROW = 22;
    private static final int SEARCH = 100;
    private static final int REFRESH = 101;
    private static final int UPDATE = 102;
    private static final int DISCORD = 103;
    private static final long REFRESH_COOLDOWN_MS = 15_000;

    private static long lastRefresh;

    private final Screen parent;
    private final TierTagger tagger = TierTagger.get();
    private final Settings settings = tagger.settings();
    private final List<Option> options = new ArrayList<Option>();
    private final List<int[]> headers = new ArrayList<int[]>();
    private final List<String> headerTitles = new ArrayList<String>();
    private int previewTop;
    private String pendingLink;

    private interface Value {
        String get();
    }

    private static final class Option {
        final ButtonWidget button;
        final String label;
        final Value value;
        final Runnable action;
        final boolean secondary;

        Option(ButtonWidget button, String label, Value value, Runnable action, boolean secondary) {
            this.button = button;
            this.label = label;
            this.value = value;
            this.action = action;
            this.secondary = secondary;
        }
    }

    public ConfigScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void init() {
        buttons.clear();
        options.clear();
        headers.clear();
        headerTitles.clear();
        final TaggerConfig config = tagger.config();

        int columnW = Math.min(200, (width - GAP - 16) / 2);
        int leftX = width / 2 - columnW - GAP / 2;
        int rightX = width / 2 + GAP / 2;

        int y = header("PRIMARY TAG", leftX, TOP);
        y = option(leftX, y, columnW, "Tier list", new Value() { public String get() { return settings.primaryListLabel(); } },
                new Runnable() { public void run() { settings.cyclePrimaryList(); } }, false);
        y = option(leftX, y, columnW, "Gamemode", new Value() { public String get() { return settings.primaryModeLabel(); } },
                new Runnable() { public void run() { settings.cyclePrimaryMode(); } }, false);

        y = header("SECONDARY TAG", leftX, y + 6);
        y = option(leftX, y, columnW, "Enabled", new Value() { public String get() { return onOff(config.secondary.enabled); } },
                new Runnable() { public void run() { settings.toggleSecondary(); } }, false);
        y = option(leftX, y, columnW, "Tier list", new Value() { public String get() { return settings.secondaryListLabel(); } },
                new Runnable() { public void run() { settings.cycleSecondaryList(); } }, true);
        int leftBottom = option(leftX, y, columnW, "Gamemode", new Value() { public String get() { return settings.secondaryModeLabel(); } },
                new Runnable() { public void run() { settings.cycleSecondaryMode(); } }, true);

        y = header("DISPLAY", rightX, TOP);
        y = option(rightX, y, columnW, "Mode", new Value() { public String get() { return config.displayMode.label(); } },
                new Runnable() { public void run() { settings.cycleDisplayMode(); } }, false);
        y = option(rightX, y, columnW, "Layout", new Value() { public String get() { return config.layout.label(); } },
                new Runnable() { public void run() { settings.cycleLayout(); } }, false);
        y = option(rightX, y, columnW, "Tiers", new Value() { public String get() { return config.tierFormat.label(); } },
                new Runnable() { public void run() { settings.cycleTierFormat(); } }, false);
        y = option(rightX, y, columnW, "Region", new Value() { public String get() { return onOff(config.showRegion); } },
                new Runnable() { public void run() { settings.toggleRegion(); } }, false);
        y = option(rightX, y, columnW, "Badge", new Value() { public String get() { return onOff(config.showBadge); } },
                new Runnable() { public void run() { settings.toggleBadge(); } }, false);
        int rightBottom = option(rightX, y, columnW, "Tab list", new Value() { public String get() { return onOff(config.showInTab); } },
                new Runnable() { public void run() { settings.toggleTab(); } }, false);

        previewTop = Math.max(leftBottom, rightBottom) + 6;

        String update = tagger.availableUpdate();
        int total = 100 + 4 + 60 + 4 + 60 + (update != null ? 94 : 0);
        int x = width / 2 - total / 2;
        int barY = height - 26;
        buttons.add(new ButtonWidget(SEARCH, x, barY, 100, 20, "Search player"));
        x += 104;
        buttons.add(new ButtonWidget(REFRESH, x, barY, 60, 20, "Refresh"));
        x += 64;
        if (update != null) {
            buttons.add(new ButtonWidget(UPDATE, x, barY, 90, 20, "§eUpdate to " + update));
            x += 94;
        }
        buttons.add(new ButtonWidget(DISCORD, x, barY, 60, 20, "Discord"));
        updateLabels();
    }

    private int header(String title, int x, int y) {
        headers.add(new int[]{x, y});
        headerTitles.add(title);
        return y + HEADER;
    }

    private int option(int x, int y, int w, String label, Value value, Runnable action, boolean secondary) {
        ButtonWidget button = new ButtonWidget(options.size(), x, y, w, 20, "");
        buttons.add(button);
        options.add(new Option(button, label, value, action, secondary));
        return y + ROW;
    }

    private void updateLabels() {
        for (Option option : options) {
            option.button.message = "§7" + option.label + ": §f" + option.value.get();
            if (option.secondary) option.button.active = tagger.config().secondary.enabled;
        }
        for (ButtonWidget button : buttons) {
            if (button.id == REFRESH) button.active = System.currentTimeMillis() - lastRefresh >= REFRESH_COOLDOWN_MS;
        }
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id < options.size()) {
            options.get(button.id).action.run();
        } else if (button.id == SEARCH) {
            minecraft.openScreen(new SearchScreen(this, null));
        } else if (button.id == REFRESH) {
            lastRefresh = System.currentTimeMillis();
            tagger.refreshAll();
        } else if (button.id == UPDATE) {
            confirmLink(TierTagger.MODRINTH_URL);
        } else if (button.id == DISCORD && TierTagger.isTrustedInvite(tagger.discordInvite())) {
            confirmLink(tagger.discordInvite());
        }
        updateLabels();
    }

    private void confirmLink(String url) {
        pendingLink = url;
        minecraft.openScreen(new ConfirmChatLinkScreen(this, url, 0, false));
    }

    @Override
    public void confirmResult(boolean result, int id) {
        if (result && pendingLink != null) {
            try {
                Desktop.getDesktop().browse(new URI(pendingLink));
            } catch (Exception ignored) {
            }
        }
        pendingLink = null;
        minecraft.openScreen(this);
    }

    @Override
    public void tick() {
        updateLabels();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
        Draw.centered("§lTier Tagger", width / 2, 9, Draw.ACCENT);
        Draw.text("v" + tagger.platform().modVersion(), 4, height - 12, 0xFF555566, false);
        for (int i = 0; i < headers.size(); i++) {
            Draw.text("§l" + headerTitles.get(i), headers.get(i)[0] + 1, headers.get(i)[1] + 2, Draw.TEXT, true);
        }
        drawPreview();
    }

    private void drawPreview() {
        if (minecraft.player == null || height - 32 - previewTop < 14) return;
        String name = minecraft.player.getName();
        Tag tag = tagger.tag(minecraft.player.getUuid(), name, false);
        String text = tag != null ? tag.decorateLegacy("§a" + name)
                : tagger.store().entry(tagger.primaryList(), minecraft.player.getUuid(), name).isResolved()
                ? "§a" + name : "§7Loading...";
        int boxW = Draw.width(text) + 12;
        Draw.rect(width / 2 - boxW / 2, previewTop, boxW, 16, 0x40000000);
        Draw.centered(text, width / 2, previewTop + 4, Draw.TEXT);
        int skinTop = previewTop + 20;
        if (height - 34 - skinTop >= 40) Draw.body(minecraft.player.getUuid(), name, width / 2 - 40, skinTop, 80, height - 36 - skinTop);
    }

    @Override
    protected void keyPressed(char typedChar, int keyCode) {
        if (keyCode == 1) {
            minecraft.openScreen(parent);
            return;
        }
        super.keyPressed(typedChar, keyCode);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    private static String onOff(boolean value) {
        return value ? "§aOn" : "§cOff";
    }
}
