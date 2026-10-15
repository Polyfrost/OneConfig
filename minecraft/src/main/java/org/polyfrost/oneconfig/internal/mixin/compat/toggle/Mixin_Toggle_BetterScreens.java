package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.microcontrollers.betterscreens.hook.ScreenMouseHook", remap = false)
public class Mixin_Toggle_BetterScreens {

    @Inject(method = {"saveCursorMiddle"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("betterscreens")) ci.cancel();
    }

    @Inject(method = "loadCursor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$keepCursor(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("betterscreens")) cir.setReturnValue(null);
    }

    @Inject(method = "getLastCloseTime", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$neverClosed(CallbackInfoReturnable<Long> cir) {
        if (!ModToggles.isEnabled("betterscreens")) cir.setReturnValue(-1L);
    }
}
