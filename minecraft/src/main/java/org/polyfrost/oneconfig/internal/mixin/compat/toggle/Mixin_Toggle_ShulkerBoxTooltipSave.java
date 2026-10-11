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

    @Inject(method = "saveToFile", at = @At("TAIL"), require = 0)
    private static void oneconfig$maskAgain(CallbackInfo ci) {
        ModGates.remaskIfOverwritten("com.misterpemodder.shulkerboxtooltip.ShulkerBoxTooltip");
    }
}
