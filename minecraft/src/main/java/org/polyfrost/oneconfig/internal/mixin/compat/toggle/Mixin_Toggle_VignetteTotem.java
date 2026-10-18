package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.codeberg.chromatic.vignettetweaks.config.VignetteConfig$Totem", remap = false)
public class Mixin_Toggle_VignetteTotem {

    @Inject(method = "evaluate", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$noTint(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("vignettetweaks")) cir.setReturnValue(null);
    }
}
