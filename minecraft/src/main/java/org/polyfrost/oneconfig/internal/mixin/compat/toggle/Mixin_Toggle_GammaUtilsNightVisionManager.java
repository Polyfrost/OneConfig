package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.sjouwer.gammautils.NightVisionManager", remap = false)
public class Mixin_Toggle_GammaUtilsNightVisionManager {

    @Inject(method = {"setNightVision(DZZZ)V", "setNightVisionStatus(Z)V"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("gammautils")) ci.cancel();
    }
}
