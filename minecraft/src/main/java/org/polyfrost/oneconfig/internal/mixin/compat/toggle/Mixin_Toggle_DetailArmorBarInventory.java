package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.redlimerl.detailab.render.InventoryArmorOverlayRenderer", remap = false)
public class Mixin_Toggle_DetailArmorBarInventory {

    @Inject(method = {"renderOverlay"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("detailabreconst")) ci.cancel();
    }
}
