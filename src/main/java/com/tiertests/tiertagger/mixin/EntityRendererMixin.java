package com.tiertests.tiertagger.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.2 {
import com.tiertests.tiertagger.render.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?}

/**
 * Covers renderers that build the nametag without going through {@link net.minecraft.world.entity.player.Player#getDisplayName()}.
 * Before 1.21.2 every nametag comes straight from the display name, so there is nothing to hook.
 */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    //? if >=1.21.2 {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void tiertag$decorateNameTag(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player)) return;
        Component decorated = Components.decorate(player.getUUID(), player.getScoreboardName(), cir.getReturnValue(), false);
        if (decorated != null) cir.setReturnValue(decorated);
    }
    //?}
}
