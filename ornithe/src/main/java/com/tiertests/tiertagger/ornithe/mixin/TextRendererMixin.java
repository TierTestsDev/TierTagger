package com.tiertests.tiertagger.ornithe.mixin;

import com.tiertests.tiertagger.core.model.ModeKind;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives the gamemode icon characters a width so 1.8.9 draws them from the mod's unicode font pages. */
@Mixin(TextRenderer.class)
abstract class TextRendererMixin {
    @Shadow
    private byte[] glyphSizes;

    @Inject(method = "initGlyphSizes", at = @At("TAIL"))
    private void tiertag$markIconGlyphs(CallbackInfo ci) {
        ModeKind.markGlyphWidths(glyphSizes);
    }
}
