package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "tektonikal.customblockhighlight.Renderer", remap = false)
public class Mixin_Toggle_BlockHighlightLegacy {

    @Inject(
        method = "mainLoop(Lnet/fabricmc/fabric/api/client/rendering/v1/level/LevelRenderContext;)V",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private static void oneconfig$skipRender(CallbackInfo ci) {
        if (!ModToggles.isEnabled("custom-block-highlight")) ci.cancel();
    }

    @Inject(
        method = "mainLoop(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;Lnet/minecraft/class_239;)Z",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private static void oneconfig$vanillaOutline(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("custom-block-highlight")) cir.setReturnValue(true);
    }
}
