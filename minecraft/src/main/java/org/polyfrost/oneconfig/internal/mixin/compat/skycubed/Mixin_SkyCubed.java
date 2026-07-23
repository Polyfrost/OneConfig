package org.polyfrost.oneconfig.internal.mixin.compat.skycubed;

//? skycubed_compat {
import org.polyfrost.oneconfig.internal.compat.SkyCubedCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.thatgravyboat.skycubed.SkyCubed;

@Pseudo
@Mixin(SkyCubed.class)
public class Mixin_SkyCubed {

    @Inject(method = "onInitialize", at = @At("TAIL"), require = 0)
    private void oneconfig$registerHudCompat(CallbackInfo ci) {
        SkyCubedCompat.initialize();
    }
}
//? }
