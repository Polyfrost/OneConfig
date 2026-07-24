package org.polyfrost.oneconfig.internal.mixin.compat.walksylib;

//? walksylib_compat {
import org.polyfrost.oneconfig.internal.compat.WalksyLibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "main.walksy.lib.core.mods.ModEntryPointList", remap = false)
public class Mixin_WalksyLib_ModEntryPointList {

    @Inject(method = "loadModConfigs", at = @At("TAIL"), remap = false)
    private void oneconfig$onLoadModConfigs(CallbackInfo ci) {
        try {
            WalksyLibCompat.onLoad(this);
        } catch (Throwable ignored) {
        }
    }
}
//? }
