package com.tiertests.tiertagger.mixin;

import com.tiertests.tiertagger.TierTaggerClient;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the last player we hit for the "look up last hit player" key. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void tiertag$rememberTarget(Player attacker, Entity target, CallbackInfo ci) {
        TierTaggerClient.onAttack(target);
    }
}
