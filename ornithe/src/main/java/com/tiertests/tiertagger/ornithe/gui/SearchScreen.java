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
    private static final int TOP = 48;
    private static final int ROW = 12;
    private static final int GRAY = 0xFFA0A0A0;

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
        int left = width / 2 - 132;
        String previous = field != null ? field.getText() : initialQuery != null ? initialQuery : "";
        field = new TextFieldWidget(0, textRenderer, left + 1, 23, 198, 18);
        field.setMaxLength(16);
        field.setText(previous);
        field.setFocused(true);
        buttons.clear();
        buttons.add(new ButtonWidget(1, left + 204, 22, 60, 20, "Search"));
        buttons.add(new ButtonWidget(2, width / 2 - 100, height - 28, 200, 20, "Done"));
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
        Draw.rect(0, TOP, width, height - 34 - TOP, 0x60000000);
        Draw.rect(0, TOP - 1, width, 1, 0x40FFFFFF);
        Draw.rect(0, height - 34, width, 1, 0x40FFFFFF);
        Draw.centered("Player Search", width / 2, 8, Draw.TEXT);
        field.render();
        super.render(mouseX, mouseY, partialTicks);

        int centerX = width / 2;
        if (searching) {
            Draw.centered("§7Searching...", centerX, TOP + 16, GRAY);
            return;
        }
        if (!status.isEmpty()) {
            Draw.centered(status, centerX, TOP + 16, GRAY);
            return;
        }
        if (result == null) {
            Draw.centered("§7Search for a player on every tier list", centerX, TOP + 16, GRAY);
            return;
        }

        int contentW = Math.min(260, width - 40);
        int left = centerX - contentW / 2;
        int bottom = height - 34;
        int y = TOP + 8 - scroll;
        if (y + 32 > TOP && y < bottom) {
            Draw.face(result.uuid(), result.name(), left, y, 32);
            Draw.text(result.name(), left + 38, y + 4, Draw.TEXT, true);
            int count = result.profiles().size();
            Draw.text("Ranked on " + count + " tier list" + (count == 1 ? "" : "s"), left + 38, y + 18, GRAY, false);
        }
        y += 44;

        for (Map.Entry<TierList, PlayerProfile> entry : result.profiles().entrySet()) {
            PlayerProfile profile = entry.getValue();
            if (visible(y, bottom)) Draw.centered("§l" + entry.getKey().displayName(), centerX, y, Draw.TEXT);
            y += ROW;
            String meta = (profile.region() != null ? profile.region().code() : "")
                    + (profile.rank() > 0 ? (profile.region() != null ? "  ·  " : "") + "#" + profile.rank() : "");
            if (!meta.isEmpty()) {
                if (visible(y, bottom)) Draw.centered("§7" + meta, centerX, y, GRAY);
                y += ROW;
            }
            y += 3;
            for (Map.Entry<GameMode, Ranking> ranking : profile.rankings().entrySet()) {
                if (visible(y, bottom)) {
                    Draw.text(LookupHud.name(tagger, ranking.getKey()), left, y, Draw.TEXT, true);
                    String tier = LookupHud.tier(tagger, ranking.getValue());
                    Draw.text(tier, left + contentW - Draw.width(tier), y, Draw.TEXT, true);
                }
                y += ROW;
            }
            y += 10;
        }
        maxScroll = Math.max(0, y + scroll - bottom);
    }

    private static boolean visible(int y, int bottom) {
        return y >= TOP && y + 9 <= bottom;
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
