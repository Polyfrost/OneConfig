package org.polyfrost.oneconfig.internal.mixin.compat.axolotlclient;

//? axolotlclient_config_compat {
import org.polyfrost.oneconfig.internal.compat.AxolotlClientConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "io.github.axolotlclient.AxolotlClientConfig.impl.AxolotlClientConfigImpl", remap = false)
public class Mixin_AxolotlClientConfigImpl {

    @Inject(method = "register", at = @At("HEAD"), remap = false, require = 0)
    private void oneconfig$register(@Coerce Object manager, CallbackInfo ci) {
        try {
            AxolotlClientConfigCompat.addManager(manager);
        } catch (Throwable ignored) {
        }
    }
}
//? }
