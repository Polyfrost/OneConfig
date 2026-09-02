package org.polyfrost.oneconfig.internal.mixin.compat.skyblocker;

//? skyblocker_compat {
import de.hysky.skyblocker.skyblock.fancybars.FancyStatusBars;
import org.polyfrost.oneconfig.internal.compat.SkyblockerCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(FancyStatusBars.class)
public class Mixin_SkyblockerFancyStatusBarsV2 {

    @Inject(method = "initStatic", at = @At("TAIL"), require = 0)
    private static void oneconfig$registerHudCompat(CallbackInfo ci) {
        SkyblockerCompat.initialize();
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$suppressWhileEditing(CallbackInfoReturnable<Boolean> cir) {
        if (CompatOverlayRenderer.oneConfigScreenOpen() && !SkyblockerCompat.isRedrawing()) {
            cir.setReturnValue(false);
        }
    }
}
//? }
