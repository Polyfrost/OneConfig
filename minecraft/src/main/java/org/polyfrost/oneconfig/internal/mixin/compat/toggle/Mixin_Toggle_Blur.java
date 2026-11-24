package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "eu.midnightdust.blur.Blur", remap = false)
public class Mixin_Toggle_Blur {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("blur");

    @Inject(method = {"canBlur"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$toggle.isEnabled()) cir.setReturnValue(false);
    }

    @Inject(method = {"renderBackground"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$skip2(CallbackInfo ci) {
        if (!oneconfig$toggle.isEnabled()) ci.cancel();
    }
}
