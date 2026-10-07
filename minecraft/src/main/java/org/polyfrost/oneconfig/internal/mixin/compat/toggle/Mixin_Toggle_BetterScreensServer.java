package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.microcontrollers.betterscreens.ServerManager", remap = false)
public class Mixin_Toggle_BetterScreensServer {

    @Inject(method = "saveLastServerIp", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$keepMaskOffDisk(CallbackInfo ci) {
        if (!ModToggles.isEnabled("betterscreens")) ci.cancel();
    }
}
