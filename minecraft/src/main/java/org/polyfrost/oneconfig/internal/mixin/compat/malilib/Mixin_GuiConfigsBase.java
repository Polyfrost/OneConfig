package org.polyfrost.oneconfig.internal.mixin.compat.malilib;

//? malilib_compat {
import org.polyfrost.oneconfig.internal.compat.MaLiLibCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.gui.GuiConfigsBase", remap = false)
public class Mixin_GuiConfigsBase {

    @Inject(method = "initGui", at = @At("TAIL"), remap = false)
    private void oneconfig$onInitGui(CallbackInfo ci) {
        try {
            MaLiLibCompat.onScreen(this);
        } catch (Throwable ignored) {
        }
    }
}
//? }
