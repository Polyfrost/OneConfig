package org.polyfrost.oneconfig.internal.mixin.compat.skyhanni;

//? if > 1.8.9 {
import org.polyfrost.oneconfig.internal.compat.SkyHanniHudCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.data.RenderData", remap = false)
public class Mixin_SkyHanniRenderData {

    @Inject(method = "postRenderOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void oneconfig$hudCompat(CallbackInfo ci) {
        SkyHanniHudCompat.ensureRegistered();
        if (CompatOverlayRenderer.oneConfigScreenOpen() && !SkyHanniHudCompat.isRedrawing()) {
            ci.cancel();
        }
    }
}
//?}
