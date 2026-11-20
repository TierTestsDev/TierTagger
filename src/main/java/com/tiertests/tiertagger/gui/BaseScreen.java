package com.tiertests.tiertagger.gui;

import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
//? if >=1.21.9
import net.minecraft.client.input.KeyEvent;

/**
 * Screen base that hides the render and input signatures that changed between versions.
 * Subclasses draw in {@link #draw}, which runs after the widgets.
 */
public abstract class BaseScreen extends Screen {
    protected final Screen parent;

    protected BaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected abstract void draw(Gfx gfx, int mouseX, int mouseY, float delta);

    /** Return true to consume a key press. */
    protected boolean onKey(int keyCode) {
        return false;
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        draw(new Gfx(graphics), mouseX, mouseY, delta);
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        draw(new Gfx(graphics), mouseX, mouseY, delta);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean keyPressed(KeyEvent event) {
        return onKey(event.key()) || super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return onKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    /** Opens a web link after the vanilla confirmation prompt. */
    protected void openLink(String url) {
        ConfirmLinkScreen.confirmLinkNow(this, java.net.URI.create(url));
    }

    public void open(Screen screen) {
        show(screen);
    }

    public static void show(Screen screen) {
        //? if >=26.2 {
        net.minecraft.client.Minecraft.getInstance().gui.setScreen(screen);
        //?} else
        /*net.minecraft.client.Minecraft.getInstance().setScreen(screen);*/
    }

    @Override
    public void onClose() {
        show(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
