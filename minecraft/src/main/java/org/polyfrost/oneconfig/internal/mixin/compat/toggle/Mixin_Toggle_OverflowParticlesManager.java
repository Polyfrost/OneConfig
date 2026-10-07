package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.polyfrost.overflowparticles.client.config.PerParticleConfigManager", remap = false)
public class Mixin_Toggle_OverflowParticlesManager {

    @Inject(method = "getConfig", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$noParticleConfig(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("overflowparticles")) cir.setReturnValue(null);
    }
}
