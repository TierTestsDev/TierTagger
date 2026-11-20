package com.tiertests.tiertagger.mixin;

import com.tiertests.tiertagger.render.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tags the display name itself rather than only the vanilla nametag renderer, so clients that draw
 * nametags with their own code (Lunar, Feather and similar) show the tag too.
 */
@Mixin(Player.class)
abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void tiertag$decorateDisplayName(CallbackInfoReturnable<Component> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide()) return;
        Component decorated = Components.decorate(self.getUUID(), self.getScoreboardName(), cir.getReturnValue(), false);
        if (decorated != null) cir.setReturnValue(decorated);
    }
}
