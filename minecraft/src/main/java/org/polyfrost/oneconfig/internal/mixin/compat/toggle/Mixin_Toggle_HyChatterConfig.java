package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tomeko.hychatter.config.HyChatterConfig", remap = false)
public class Mixin_Toggle_HyChatterConfig {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("hychatter");

    @Inject(method = {"getCoordsWaypointsEnabled", "getDebugModeEnabled"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$toggle.isEnabled()) cir.setReturnValue(false);
    }
}
