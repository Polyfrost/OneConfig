package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "eu.midnightdust.lib.config.MidnightConfig", remap = false)
public class Mixin_Toggle_MidnightSave {

    @Inject(method = "write", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$keepMaskOffDisk(String modid, CallbackInfo ci) {
        if (ModGates.blocksSave(modid)) ci.cancel();
    }

    @Inject(method = "loadValuesFromJson", at = @At("TAIL"), require = 0)
    private void oneconfig$maskAgain(CallbackInfo ci) {
        ModGates.remask(((Object) this).getClass().getName());
    }
}
