package org.polyfrost.oneconfig.internal.mixin.compat.ukulib;

//? ukulib_compat {
import net.minecraft.client.gui.screens.Screen;
import org.polyfrost.oneconfig.internal.compat.UkulibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.uku3lig.ukulib.config.screen.BaseConfigScreen", remap = false)
public class Mixin_BaseConfigScreen {

    // hooked on construction rather than init so a screen that is only built and thrown away, which is what the
    // Mod Menu warmup does, is still picked up
    // the config manager is coerced because ukulib is not on the compile classpath
    @Inject(method = "<init>", at = @At("TAIL"))
    private void oneconfig$onCreated(String key, Screen parent, @Coerce Object manager, CallbackInfo ci) {
        try {
            UkulibCompat.onScreenCreated(this);
        } catch (Throwable ignored) {
        }
    }
}
//? }
