package com.tiertests.tiertagger.ornithe.mixin;

import com.tiertests.tiertagger.ornithe.TierTaggerOrnithe;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs {@code /tiertagger} on the client instead of sending it to the server. */
@Mixin(LocalClientPlayerEntity.class)
abstract class LocalClientPlayerEntityMixin {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void tiertag$command(String message, CallbackInfo ci) {
        if (TierTaggerOrnithe.onChatCommand(message)) ci.cancel();
    }
}
