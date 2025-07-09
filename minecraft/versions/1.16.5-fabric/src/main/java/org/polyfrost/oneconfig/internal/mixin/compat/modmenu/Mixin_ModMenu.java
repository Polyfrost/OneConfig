package org.polyfrost.oneconfig.internal.mixin.compat.modmenu;

import com.terraformersmc.modmenu.ModMenu;
import org.polyfrost.oneconfig.internal.compat.ModMenuCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ModMenu.class)
public class Mixin_ModMenu {

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        ModMenuCompat.enable();
    }

}
