package org.polyfrost.oneconfig.internal.mixin.compat.midnightlib;

//? midnightlib_compat {
import org.polyfrost.oneconfig.internal.compat.MidnightLibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "eu.midnightdust.lib.config.MidnightConfig", remap = false)
public class Mixin_MidnightConfig {

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private static void oneconfig$onInit(String modid, Class<?> config, CallbackInfo ci) {
        try {
            MidnightLibCompat.onInit(modid, config);
        } catch (Throwable ignored) {
        }
    }
}
//? }
