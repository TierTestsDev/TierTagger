package com.tiertests.tiertagger.mixin;

import com.mojang.authlib.GameProfile;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.render.Components;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void tiertag$decorateTabName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null || !tagger.config().showInTab) return;
        GameProfile profile = info.getProfile();
        //? if >=1.21.9 {
        Component decorated = Components.decorate(profile.id(), profile.name(), cir.getReturnValue(), true);
        //?} else
        /*Component decorated = Components.decorate(profile.getId(), profile.getName(), cir.getReturnValue(), true);*/
        if (decorated != null) cir.setReturnValue(decorated);
    }
}
