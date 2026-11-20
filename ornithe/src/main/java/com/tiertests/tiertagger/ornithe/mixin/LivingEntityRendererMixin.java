package com.tiertests.tiertagger.ornithe.mixin;

import com.tiertests.tiertagger.core.TierTagger;
import com.tiertests.tiertagger.core.text.Tag;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Tags the nametag text right where it is drawn rather than in getDisplayName, because performance mods
 * in clients like OneClient (Sarcio) build the nametag without calling getDisplayName. Mods that restyle
 * nametags (PolyNametag, Argentum) draw this same string, so they keep working.
 */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @ModifyVariable(method = "renderNameTag(Lnet/minecraft/entity/living/LivingEntity;DDD)V", at = @At("STORE"), ordinal = 0)
    private String tiertag$decorateNameTag(String name, LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return name;
        TierTagger tagger = TierTagger.get();
        if (tagger == null) return name;
        PlayerEntity player = (PlayerEntity) entity;
        Tag tag = tagger.tag(player.getUuid(), player.getName(), false);
        if (tag == null) return name;
        return tag.prefix().toLegacyString() + name + tag.suffix().toLegacyString();
    }
}
