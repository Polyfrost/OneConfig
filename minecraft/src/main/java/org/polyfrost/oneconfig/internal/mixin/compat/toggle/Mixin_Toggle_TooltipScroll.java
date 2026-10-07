package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.provismet.tooltipscroll.ScrollTracker", remap = false)
public class Mixin_Toggle_TooltipScroll {

    @Inject(method = {"update", "alignToTop"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("tooltipscroll")) ci.cancel();
    }

    @Inject(method = {"getXOffset", "getYOffset"}, at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$noOffset(CallbackInfoReturnable<Integer> cir) {
        if (!ModToggles.isEnabled("tooltipscroll")) cir.setReturnValue(0);
    }
}
