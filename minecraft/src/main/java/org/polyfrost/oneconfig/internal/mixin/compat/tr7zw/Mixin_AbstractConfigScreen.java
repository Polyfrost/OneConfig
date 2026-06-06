package org.polyfrost.oneconfig.internal.mixin.compat.tr7zw;

//? tr7zw_compat {
import org.polyfrost.oneconfig.internal.compat.Tr7zwConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.tr7zw.trender.gui.client.AbstractConfigScreen", remap = false)
public class Mixin_AbstractConfigScreen {

    @Inject(method = "createOptionList", at = @At("HEAD"))
    private void oneconfig$onCreateOptionList(List<?> options, CallbackInfoReturnable<?> cir) {
        try {
            Tr7zwConfigCompat.parseOptionList(this, options);
        } catch (Throwable ignored) {
        }
    }
}
//? }
