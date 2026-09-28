package org.polyfrost.oneconfig.internal.mixin.compat.mlib;

//? mlib_compat {
/*import me.owdding.lib.overlays.Overlay;
import me.owdding.lib.overlays.Overlays;
import org.polyfrost.oneconfig.internal.compat.MlibCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(Overlays.class)
public class Mixin_MlibOverlays {

    @Inject(method = "register", at = @At("TAIL"), require = 0)
    private void registerHudCompat(Overlay overlay, CallbackInfo ci) {
        MlibCompat.onRegister(overlay);
    }

    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void suppressWhileEditing(CallbackInfo ci) {
        if (CompatOverlayRenderer.oneConfigScreenOpen()) {
            ci.cancel();
        }
    }
}
*///? }
