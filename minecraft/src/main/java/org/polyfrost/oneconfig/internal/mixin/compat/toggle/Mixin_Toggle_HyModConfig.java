package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tomeko.hymod.config.HyModConfig", remap = false)
public class Mixin_Toggle_HyModConfig {

    @Inject(method = {"getCoordsWaypointsEnabled", "getHideGuildMOTDEnabled", "getMvpEmojisEnabled"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("hymod")) cir.setReturnValue(false);
    }
}
