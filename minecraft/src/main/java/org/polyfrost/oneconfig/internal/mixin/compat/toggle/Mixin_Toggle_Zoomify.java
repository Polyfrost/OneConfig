package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import com.llamalad7.mixinextras.sugar.Local;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.isxander.zoomify.Zoomify", remap = false)
public class Mixin_Toggle_Zoomify {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$stopZooming(CallbackInfo ci) {
        if (ModToggles.isEnabled("zoomify")) return;
        ModGates.resetZoomify();
        ci.cancel();
    }

    @Inject(method = "getZoomDivisor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$noZoom(CallbackInfoReturnable<Float> cir) {
        if (!ModToggles.isEnabled("zoomify")) cir.setReturnValue(1.0F);
    }

    @Inject(method = "shouldRenderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$vanillaSpyglass(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) boolean scoping) {
        if (!ModToggles.isEnabled("zoomify")) cir.setReturnValue(scoping);
    }
}
