package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.microcontrollers.mountopacity.hook.EntityHook", remap = false)
public class Mixin_Toggle_MountOpacity {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("mountopacity");

    @Inject(method = "setOpacity(I)I", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$keepColor(int color, CallbackInfoReturnable<Integer> cir) {
        if (!oneconfig$toggle.isEnabled()) cir.setReturnValue(color);
    }
}
