package com.tiertests.tiertagger.ornithe.mixin;

import com.mojang.authlib.GameProfile;
import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import net.minecraft.client.gui.overlay.PlayerTabOverlay;
import net.minecraft.client.network.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
abstract class PlayerTabOverlayMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tiertag$decorateTabName(PlayerInfo info, CallbackInfoReturnable<String> cir) {
        TierTagger tagger = TierTagger.get();
        if (tagger == null || !tagger.config().showInTab) return;
        GameProfile profile = info.getProfile();
        if (profile.getId() == null) return;
        Tag tag = tagger.tag(profile.getId(), profile.getName(), true);
        if (tag != null) cir.setReturnValue(tag.decorateLegacy(cir.getReturnValue()));
    }
}
