package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.polyfrost.polyzoom.PolyZoom", remap = false)
public class Mixin_Toggle_PolyZoom {

    @Inject(method = {"onZoomKey", "onSecondaryZoomKey", "addScrollStep"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("polyzoom")) ci.cancel();
    }

    // left to tick so a zoom in progress eases back out
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void oneconfig$releaseZoom(CallbackInfo ci) {
        if (!ModToggles.isEnabled("polyzoom")) ModGates.releasePolyZoom(this);
    }
}
