package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

//? moul_compat {
/*import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Pseudo
@MoulConfig
@RelocatedMixin
@Mixin(targets = "io.github.notenoughupdates.moulconfig.observer.PropertyImpl", remap = false)
public class Mixin_PropertyImpl {

    // PropertyImpl<T>.value - erased to Object
    @Shadow
    Object value;

    @Inject(method = "set", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void oneconfig$skipRedundantSet(Object newValue, CallbackInfo ci) {
        if (Objects.equals(this.value, newValue)) {
            ci.cancel();
        }
    }
}
*///? }
