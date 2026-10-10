package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.codeberg.chromatic.fovchanger.option.FOVChangerConfig", remap = false)
public class Mixin_Toggle_FovChanger {

    @Inject(
        method = {"getSpeed", "getFreezing", "getSprint", "getAiming", "getFlying", "getSubmerged"},
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private static void oneconfig$vanillaFov(CallbackInfoReturnable<Float> cir) {
        if (!ModToggles.isEnabled("fovchanger")) cir.setReturnValue(1.0F);
    }
}
