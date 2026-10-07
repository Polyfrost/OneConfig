package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
    targets = {
        "me.owdding.customscoreboard.config.Config", "me.owdding.customscoreboard.config.MainConfig",
        "gay.j10a1n15.customscoreboard.config.MainConfig",
    },
    remap = false
)
public class Mixin_Toggle_CustomScoreboard {

    @Inject(method = {"getEnabled"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$force1(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("customscoreboard")) cir.setReturnValue(false);
    }
}
