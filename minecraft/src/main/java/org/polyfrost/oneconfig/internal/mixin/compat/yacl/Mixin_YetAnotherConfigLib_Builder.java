package org.polyfrost.oneconfig.internal.mixin.compat.yacl;

//? yacl_compat {
import org.polyfrost.oneconfig.internal.compat.YACLCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.isxander.yacl3.api.YetAnotherConfigLib", remap = false)
public class Mixin_YetAnotherConfigLib_Builder {

    @Inject(method = "generateScreen", at = @At("HEAD"))
    private void oneconfig$onGenerateScreen(Object parent, CallbackInfoReturnable<?> cir) {
        try {
            YACLCompat.parseYACL(this);
        } catch (Throwable ignored) {
        }
    }
}
//? }

