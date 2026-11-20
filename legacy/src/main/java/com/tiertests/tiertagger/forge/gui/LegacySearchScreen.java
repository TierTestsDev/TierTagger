package com.tiertests.tiertagger.forge.gui;

import com.tiertests.tiertagger.core.LookupResult;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.model.GameMode;
import com.tiertests.tiertagger.core.model.PlayerProfile;
import com.tiertests.tiertagger.core.model.Ranking;
import com.tiertests.tiertagger.core.model.Strings;
import com.tiertests.tiertagger.core.source.TierList;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;

/** Looks a username up on every tier list at once. */
public final class LegacySearchScreen extends GuiScreen {
    private static final int CONTENT_TOP = 34;
    private static final int LINE = 11;

    private final GuiScreen parent;
    private final String initialQuery;
    private final TierTagger tagger = TierTagger.get();
    private GuiTextField field;
    private String status = "";
    private LookupResult result;
    private boolean searching;
    private int requestId;
    private int scroll;
    private int maxScroll;

    public LegacySearchScreen(GuiScreen parent, String initialQuery) {
        this.parent = parent;
        this.initialQuery = initialQuery;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int barW = Math.min(280, width - 40);
        int left = width / 2 - barW / 2;
        String previous = field != null ? field.getText() : initialQuery != null ? initialQuery : "";
        field = new GuiTextField(0, fontRendererObj, left + 4, 8, barW - 60, 20);
        field.setMaxStringLength(16);
        field.setText(previous);
        field.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(1, left + barW - 52, 8, 52, 20, "Search"));
        buttonList.add(new GuiButton(2, 4, 8, 50, 20, "← Back"));
        if (initialQuery != null && result == null && !searching && status.isEmpty()) search();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 1) search();
        else if (button.id == 2) mc.displayGuiScreen(parent);
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
                mc.addScheduledTask(new Runnable() {
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
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            search();
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
        } else {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        field.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll = Math.max(0, Math.min(maxScroll, scroll - Integer.signum(wheel) * 20));
    }

    @Override
    public void updateScreen() {
        field.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        field.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);

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
                    Draw.text(LegacyLookupHud.line(tagger, ranking.getKey(), ranking.getValue()), left + 10, lineY, Draw.TEXT, false);
                    lineY += LINE;
                }
            }
            y += cardH + 8;
        }
        maxScroll = Math.max(0, y + scroll - height + 10);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
