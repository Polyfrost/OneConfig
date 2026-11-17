package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.a5b84.statuseffectbars.StatusEffectBarRenderer", remap = false)
public class Mixin_Toggle_StatusEffectBars {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("status-effect-bars");

    @Inject(method = {"render"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$skip1(CallbackInfo ci) {
        if (!oneconfig$toggle.isEnabled()) ci.cancel();
    }
}
