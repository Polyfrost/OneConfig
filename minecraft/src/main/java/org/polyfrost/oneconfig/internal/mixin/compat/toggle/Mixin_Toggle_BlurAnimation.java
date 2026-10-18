package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "eu.midnightdust.blur.animations.AbstractAnimationHandler", remap = false)
public class Mixin_Toggle_BlurAnimation {

    @Inject(method = "getCurrentValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$fullRadius(CallbackInfoReturnable<Float> cir) {
        if (ModToggles.isEnabled("blur")) return;
        if (((Object) this).getClass().getName().endsWith("BlurRadiusAnimationHandler")) cir.setReturnValue(1.0F);
    }
}
