package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.isxander.controlify.Controlify", remap = false)
public class Mixin_Toggle_Controlify {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$noController(CallbackInfo ci) {
        if (ModToggles.isEnabled("controlify")) return;
        ModGates.dropController(this);
        ci.cancel();
    }
}
