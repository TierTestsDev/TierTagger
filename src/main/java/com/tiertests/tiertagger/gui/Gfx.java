package com.tiertests.tiertagger.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2
/*import net.minecraft.client.renderer.RenderType;*/

/**
 * Thin wrapper over the GUI drawing context. Every drawing call that changed between Minecraft
 * versions goes through here, so screens and HUDs are written once.
 * Colors are always ARGB.
 */
public final class Gfx {
    //? if >=26.1 {
    private final GuiGraphicsExtractor g;
    //?} else
    /*private final GuiGraphics g;*/
    private final Font font;

    //? if >=26.1 {
    public Gfx(GuiGraphicsExtractor g) {
    //?} else
    /*public Gfx(GuiGraphics g) {*/
        this.g = g;
        this.font = Minecraft.getInstance().font;
    }

    public Font font() {
        return font;
    }

    public int width(Component text) {
        return font.width(text);
    }

    public int width(String text) {
        return font.width(text);
    }

    public void fill(int x1, int y1, int x2, int y2, int argb) {
        g.fill(x1, y1, x2, y2, argb);
    }

    public void rect(int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + h, argb);
    }

    public void border(int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** Rectangle with its four corner pixels cut off. */
    public void roundRect(int x, int y, int w, int h, int argb) {
        g.fill(x + 1, y, x + w - 1, y + 1, argb);
        g.fill(x, y + 1, x + w, y + h - 1, argb);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, argb);
    }

    public void text(Component text, int x, int y, int argb, boolean shadow) {
        //? if >=26.1 {
        g.text(font, text, x, y, argb, shadow);
        //?} else
        /*g.drawString(font, text, x, y, argb, shadow);*/
    }

    public void text(String text, int x, int y, int argb, boolean shadow) {
        text(Component.literal(text), x, y, argb, shadow);
    }

    public void centered(Component text, int centerX, int y, int argb) {
        text(text, centerX - font.width(text) / 2, y, argb, true);
    }

    public void centered(String text, int centerX, int y, int argb) {
        centered(Component.literal(text), centerX, y, argb);
    }

    /** Draws a region of a texture; {@code tint} multiplies the texture colors (0xFFFFFFFF for none). */
    public void texture(Identifier id, int x, int y, int w, int h, float u, float v, int regionW, int regionH,
                        int textureW, int textureH, int tint) {
        //? if >=1.21.6 {
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, regionW, regionH, textureW, textureH, tint);
        //?} else if >=1.21.2 {
        /*g.blit(RenderType::guiTextured, id, x, y, u, v, w, h, regionW, regionH, textureW, textureH, tint);
        *///?} else {
        /*g.setColor((tint >> 16 & 0xFF) / 255f, (tint >> 8 & 0xFF) / 255f, (tint & 0xFF) / 255f, (tint >>> 24) / 255f);
        g.blit(id, x, y, w, h, u, v, regionW, regionH, textureW, textureH);
        g.setColor(1f, 1f, 1f, 1f);
        *///?}
    }

    public void push() {
        //? if >=1.21.6 {
        g.pose().pushMatrix();
        //?} else
        /*g.pose().pushPose();*/
    }

    public void pop() {
        //? if >=1.21.6 {
        g.pose().popMatrix();
        //?} else
        /*g.pose().popPose();*/
    }

    public void translate(float x, float y) {
        //? if >=1.21.6 {
        g.pose().translate(x, y);
        //?} else
        /*g.pose().translate(x, y, 0);*/
    }

    public void scale(float factor) {
        //? if >=1.21.6 {
        g.pose().scale(factor, factor);
        //?} else
        /*g.pose().scale(factor, factor, 1f);*/
    }

    public void scissor(int x1, int y1, int x2, int y2) {
        g.enableScissor(x1, y1, x2, y2);
    }

    public void endScissor() {
        g.disableScissor();
    }
}
