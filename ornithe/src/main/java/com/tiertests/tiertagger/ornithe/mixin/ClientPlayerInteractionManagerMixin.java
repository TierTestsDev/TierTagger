package com.tiertests.tiertagger.ornithe.mixin;

import com.tiertests.tiertagger.ornithe.TierTaggerOrnithe;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the last player we hit for the "look up last hit player" key. */
@Mixin(ClientPlayerInteractionManager.class)
abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void tiertag$rememberTarget(PlayerEntity attacker, Entity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity) TierTaggerOrnithe.onAttack((PlayerEntity) target);
    }
}
