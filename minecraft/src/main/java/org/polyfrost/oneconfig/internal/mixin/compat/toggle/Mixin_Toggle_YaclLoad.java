package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.isxander.yacl3.config.v2.impl.ConfigClassHandlerImpl", remap = false)
public class Mixin_Toggle_YaclLoad {

    @Inject(method = "load", at = @At("RETURN"), require = 0)
    private void oneconfig$maskAgain(CallbackInfoReturnable<Boolean> cir) {
        ModGates.remaskYacl(this);
    }
}
