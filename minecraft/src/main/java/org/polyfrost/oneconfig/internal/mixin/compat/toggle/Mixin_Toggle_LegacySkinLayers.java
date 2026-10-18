package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.polyfrost.skinlayers.skin.SkinMeshCache", remap = false)
public class Mixin_Toggle_LegacySkinLayers {

    @Inject(method = {"forPlayer", "forSkull"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$noMesh(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("legacy-skin-layers")) cir.setReturnValue(null);
    }
}
