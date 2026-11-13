package org.polyfrost.oneconfig.internal.mixin.render;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.polyfrost.oneconfig.internal.ui.PreviewNameTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class Mixin_EntityPreviewNameTag {

    @Inject(
            //? if >= 1.21.4 {
            method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            //?} elif > 1.8.9 {
            /*method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z",
            *///?} else
            //method = "shouldRenderNameTag(Lnet/minecraft/entity/living/LivingEntity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void oneconfig$entityPreviewNameTag(
            LivingEntity entity,
            //? if >= 1.21.4
            double distanceToCameraSq,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (PreviewNameTag.rendering) {
            cir.setReturnValue(PreviewNameTag.show);
        }
    }

}
