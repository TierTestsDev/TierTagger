package com.tiertests.tiertagger.ornithe.gui;

import com.tiertests.tiertagger.core.Settings;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import net.minecraft.client.gui.screen.ConfirmationListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.game.ConfirmChatLinkScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Settings in the style of the vanilla 1.8.9 option screens, with a preview of your own nametag. */
public final class ConfigScreen extends Screen implements ConfirmationListener {
    private static final int SEARCH = 100;
    private static final int DONE = 101;
    private static final int UPDATE = 102;
    private static final int DISCORD = 103;

    private final Screen parent;
    private final TierTagger tagger = TierTagger.get();
    private final Settings settings = tagger.settings();
    private final List<Option> options = new ArrayList<Option>();
    private int previewTop;
    private int previewBottom;
    private String pendingLink;

    private interface Value {
        String get();
    }

    private static final class Option {
        final ButtonWidget button;
        final String label;
        final Value value;
        final Runnable action;

        Option(ButtonWidget button, String label, Value value, Runnable action) {
            this.button = button;
            this.label = label;
            this.value = value;
            this.action = action;
        }
    }

    public ConfigScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void init() {
        buttons.clear();
        options.clear();
        add("Tier List", new Value() { public String get() { return settings.primaryListLabel(); } },
                new Runnable() { public void run() { settings.cyclePrimaryList(); } });
        add("Gamemode", new Value() { public String get() { return settings.primaryModeLabel(); } },
                new Runnable() { public void run() { settings.cyclePrimaryMode(); } });
        add("Second Tag", new Value() { public String get() { return settings.secondaryListLabel(); } },
                new Runnable() { public void run() { settings.cycleSecondaryList(); } });
        add("Second Gamemode", new Value() { public String get() { return settings.secondaryModeLabel(); } },
                new Runnable() { public void run() { settings.cycleSecondaryMode(); } });
        add("Position", new Value() { public String get() { return settings.layoutLabel(); } },
                new Runnable() { public void run() { settings.cycleLayout(); } });
        add("Icons", new Value() { public String get() { return onOff(settings.iconsOn()); } },
                new Runnable() { public void run() { settings.toggleIcons(); } });
        add("Region", new Value() { public String get() { return onOff(settings.regionOn()); } },
                new Runnable() { public void run() { settings.toggleRegion(); } });
        add("Badge", new Value() { public String get() { return onOff(settings.badgeOn()); } },
                new Runnable() { public void run() { settings.toggleBadge(); } });
        add("Tab List", new Value() { public String get() { return onOff(settings.tabOn()); } },
                new Runnable() { public void run() { settings.toggleTab(); } });

        int footerY = height - 28;
        buttons.add(new ButtonWidget(SEARCH, width / 2 - 155, footerY, 150, 20, "Search Player"));
        buttons.add(new ButtonWidget(DONE, width / 2 + 5, footerY, 150, 20, "Done"));
        buttons.add(new ButtonWidget(DISCORD, width - 54, footerY, 50, 20, "Discord"));
        String update = tagger.availableUpdate();
        if (update != null) buttons.add(new ButtonWidget(UPDATE, width - 104, 6, 100, 20, "§eUpdate to " + update));

        Option last = options.get(options.size() - 1);
        previewTop = last.button.y + 30;
        previewBottom = footerY - 6;
        updateLabels();
    }

    private void add(String label, Value value, Runnable action) {
        int index = options.size();
        int x = index % 2 == 0 ? width / 2 - 155 : width / 2 + 5;
        int y = 32 + 24 * (index / 2);
        ButtonWidget button = new ButtonWidget(index, x, y, 150, 20, "");
        buttons.add(button);
        options.add(new Option(button, label, value, action));
    }

    private void updateLabels() {
        for (Option option : options) option.button.message = option.label + ": " + option.value.get();
        options.get(3).button.active = settings.secondaryEnabled();
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id < options.size()) {
            options.get(button.id).action.run();
        } else if (button.id == SEARCH) {
            minecraft.openScreen(new SearchScreen(this, null));
        } else if (button.id == DONE) {
            minecraft.openScreen(parent);
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
        Draw.centered("Tier Tagger", width / 2, 15, Draw.TEXT);
        super.render(mouseX, mouseY, partialTicks);
        drawPreview();
    }

    /** Your own nametag on your skin, so changes show up right away. */
    private void drawPreview() {
        if (minecraft.player == null || previewBottom - previewTop < 12) return;
        String name = minecraft.player.getName();
        Tag tag = tagger.tag(minecraft.player.getUuid(), name, false);
        String text = tag != null ? tag.decorateLegacy(name)
                : tagger.store().entry(tagger.primaryList(), minecraft.player.getUuid(), name).isResolved()
                ? name : "§7Loading...";
        int bodyH = Math.min(96, previewBottom - previewTop - 12) & ~1;
        int tagY = bodyH >= 32 ? previewTop : previewTop + (previewBottom - previewTop - 10) / 2;
        int textW = Draw.width(text);
        Draw.rect(width / 2 - textW / 2 - 2, tagY - 1, textW + 4, 10, 0x40000000);
        Draw.text(text, width / 2 - textW / 2, tagY, Draw.TEXT, false);
        if (bodyH >= 32) Draw.body(minecraft.player.getUuid(), name, width / 2 - bodyH / 4, tagY + 12, bodyH / 2, bodyH);
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
        return value ? "ON" : "OFF";
    }
}
