package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.lambdaurora.lambdabettergrass.LBGConfig", remap = false)
public class Mixin_Toggle_LambdaBetterGrassMode {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("lambdabettergrass");

    @Inject(method = "getMode", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$off(CallbackInfoReturnable<Object> cir) {
        if (oneconfig$toggle.isEnabled()) return;
        Object off = ModGates.enumConstant("dev.lambdaurora.lambdabettergrass.LBGMode", "OFF");
        if (off != null) cir.setReturnValue(off);
    }
}
