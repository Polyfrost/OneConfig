package org.polyfrost.oneconfig.internal.mixin.compat.modmenu;

import com.terraformersmc.modmenu.ModMenu;
import org.polyfrost.oneconfig.internal.compat.ModMenuCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModMenu.class)
public class ModMenuMixin {

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void init(CallbackInfo ci) {
        ModMenuCompat.enable();
    }

}
