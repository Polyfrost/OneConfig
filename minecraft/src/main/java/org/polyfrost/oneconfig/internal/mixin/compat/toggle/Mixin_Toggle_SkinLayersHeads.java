package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.tr7zw.skinlayers.SkullRendererCache$ItemSettings", remap = false)
public class Mixin_Toggle_SkinLayersHeads {

    @Inject(method = "getHeadLayers", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$noMesh(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("skinlayers3d")) cir.setReturnValue(null);
    }
}
