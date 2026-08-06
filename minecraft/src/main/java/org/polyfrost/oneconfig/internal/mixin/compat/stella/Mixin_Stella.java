package org.polyfrost.oneconfig.internal.mixin.compat.stella;

//? stella_compat {
import co.stellarskys.stella.Stella;
import org.polyfrost.oneconfig.internal.compat.StellaCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(Stella.class)
public class Mixin_Stella {
    @Inject(method = "onInitializeClient", at = @At("TAIL"), require = 0)
    private void oneconfig$registerStella(CallbackInfo ci) {
        StellaCompat.initialize();
    }
}
//? }