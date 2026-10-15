package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.wwaypoints.client.ServerFeaturePolicy", remap = false)
public class Mixin_Toggle_WWaypoints {

    @Inject(method = {"allows"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("wwaypoints")) cir.setReturnValue(false);
    }
}
