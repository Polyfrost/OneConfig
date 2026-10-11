package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.misterpemodder.shulkerboxtooltip.impl.config.ConfigurationHandler", remap = false)
public class Mixin_Toggle_ShulkerBoxTooltipSave {

    @Inject(method = "saveToFile", at = @At("HEAD"), require = 0)
    private static void oneconfig$keepMaskOffDisk(CallbackInfo ci) {
        ModGates.unmaskForSave("com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip");
    }

    @Inject(method = "saveToFile", at = @At("RETURN"), require = 0)
    private static void oneconfig$maskAgain(CallbackInfo ci) {
        ModGates.maskAfterSave("com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip");
    }
}
