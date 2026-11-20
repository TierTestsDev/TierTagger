package com.tiertests.tiertagger.ornithe.mixin;

import com.tiertests.tiertagger.ornithe.gui.LookupHud;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the lookup popup on top of the HUD. */
@Mixin(GameGui.class)
abstract class GameGuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void tiertag$renderLookup(float tickDelta, CallbackInfo ci) {
        LookupHud.render();
    }
}
