package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.microcontrollers.betterscreens.hook.ContainerScaleHook", remap = false)
public class Mixin_Toggle_BetterScreensScale {

    @Inject(method = "shouldScale", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$vanillaScale(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("betterscreens")) cir.setReturnValue(false);
    }
}
