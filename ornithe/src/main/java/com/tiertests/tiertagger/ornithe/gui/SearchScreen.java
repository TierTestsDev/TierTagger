package com.tiertests.tiertagger.ornithe.gui;

import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.source.TierList;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Map;
import java.util.function.BiConsumer;

/** Looks a username up on every tier list at once. */
public final class SearchScreen extends Screen {
    private static final int CONTENT_TOP = 34;
    private static final int LINE = 11;

    private final Screen parent;
    private final String initialQuery;
    private final TierTagger tagger = TierTagger.get();
    private TextFieldWidget field;
    private String status = "";
    private LookupResult result;
    private boolean searching;
    private int requestId;
    private int scroll;
    private int maxScroll;

    public SearchScreen(Screen parent, String initialQuery) {
        this.parent = parent;
        this.initialQuery = initialQuery;
    }

    @Override
    public void init() {
        Keyboard.enableRepeatEvents(true);
        int barW = Math.min(280, width - 40);
        int left = width / 2 - barW / 2;
        String previous = field != null ? field.getText() : initialQuery != null ? initialQuery : "";
        field = new TextFieldWidget(0, textRenderer, left + 4, 8, barW - 60, 20);
        field.setMaxLength(16);
        field.setText(previous);
        field.setFocused(true);
        buttons.clear();
        buttons.add(new ButtonWidget(1, left + barW - 52, 8, 52, 20, "Search"));
        buttons.add(new ButtonWidget(2, 4, 8, 50, 20, "← Back"));
        if (initialQuery != null && result == null && !searching && status.isEmpty()) search();
    }

    @Override
    public void removed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 1) search();
        else if (button.id == 2) minecraft.openScreen(parent);
    }

    private void search() {
        final String name = field.getText().trim();
        if (name.isEmpty()) return;
        if (!Strings.isValidUsername(name)) {
            status = "§cThat is not a valid username.";
            result = null;
            return;
        }
        status = "";
        result = null;
        searching = true;
        scroll = 0;
        final int request = ++requestId;
        tagger.search(name).whenComplete(new BiConsumer<LookupResult, Throwable>() {
            @Override
            public void accept(final LookupResult found, final Throwable error) {
                minecraft.executeTask(new Runnable() {
                    @Override
                    public void run() {
                        if (request != requestId) return;
                        searching = false;
                        if (error != null) status = "§cCould not reach the tier lists. Try again later.";
                        else if (found == null) status = "§cNo Minecraft account is called " + name + ".";
                        else if (found.isEmpty()) status = "§7" + found.name() + " is not ranked on any tier list.";
                        else result = found;
                    }
                });
            }
        });
    }

    @Override
    protected void keyPressed(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            search();
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            minecraft.openScreen(parent);
        } else {
            field.keyPressed(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        field.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(wheel) * 20));
    }

    @Override
    public void tick() {
        field.tick();
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        field.render();
        super.render(mouseX, mouseY, partialTicks);

        int centerX = width / 2;
        if (searching) {
            Draw.centered("§7Searching every tier list...", centerX, CONTENT_TOP + 20, Draw.TEXT_DIM);
            return;
        }
        if (!status.isEmpty()) {
            Draw.centered(status, centerX, CONTENT_TOP + 20, Draw.TEXT);
            return;
        }
        if (result == null) {
            Draw.centered("§7Search for a player above", centerX, CONTENT_TOP + 30, Draw.TEXT_DIM);
            return;
        }

        int contentW = Math.min(340, width - 20);
        int left = centerX - contentW / 2;
        int y = CONTENT_TOP - scroll;

        Draw.rect(left, y, contentW, 44, Draw.PANEL);
        Draw.border(left, y, contentW, 44, Draw.ACCENT);
        Draw.face(result.uuid(), result.name(), left + 6, y + 6, 32);
        Draw.text("§a§l" + result.name(), left + 46, y + 10, Draw.TEXT, true);
        int count = result.profiles().size();
        Draw.text("§7Ranked on §f" + count + "§7 tier list" + (count == 1 ? "" : "s"), left + 46, y + 24, Draw.TEXT_DIM, false);
        y += 56;

        for (Map.Entry<TierList, PlayerProfile> entry : result.profiles().entrySet()) {
            PlayerProfile profile = entry.getValue();
            int cardH = 24 + profile.rankings().size() * LINE + 6;
            if (y + cardH > CONTENT_TOP - 20 && y < height) {
                Draw.rect(left, y, contentW, cardH, Draw.PANEL);
                Draw.border(left, y, contentW, cardH, Draw.ACCENT_DIM);
                String meta = (profile.region() != null ? "§9" + profile.region().code() + "  " : "")
                        + (profile.rank() > 0 ? "§6#" + profile.rank() : "");
                Draw.text("§f§l" + entry.getKey().displayName(), left + 8, y + 7, Draw.TEXT, true);
                Draw.text(meta, left + contentW - Draw.width(meta) - 8, y + 7, Draw.TEXT, true);
                int lineY = y + 22;
                for (Map.Entry<GameMode, Ranking> ranking : profile.rankings().entrySet()) {
                    Draw.text(LookupHud.line(tagger, ranking.getKey(), ranking.getValue()), left + 10, lineY, Draw.TEXT, false);
                    lineY += LINE;
                }
            }
            y += cardH + 8;
        }
        maxScroll = Math.max(0, y + scroll - height + 10);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
