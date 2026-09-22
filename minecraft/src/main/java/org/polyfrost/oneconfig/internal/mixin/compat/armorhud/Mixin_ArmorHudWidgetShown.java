package org.polyfrost.oneconfig.internal.mixin.compat.armorhud;

import org.polyfrost.oneconfig.internal.compat.ArmorHudCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings({"rawtypes", "unchecked"})
@Pseudo
@Mixin(targets = "ru.berdinskiybear.armorhud.config.ArmorHudConfig", remap = false)
public class Mixin_ArmorHudWidgetShown {

    @Inject(method = "getWidgetShown", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$exampleWhileEditing(CallbackInfoReturnable cir) {
        try {
            Object forced = ArmorHudCompat.forcedWidgetShown(this);
            if (forced != null) cir.setReturnValue(forced);
        } catch (Throwable ignored) {
        }
    }
}
