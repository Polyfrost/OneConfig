package org.polyfrost.oneconfig.internal.mixin.compat.apec;

//? apec_compat {
/*import org.polyfrost.oneconfig.internal.compat.ApecCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.hexeption.apec.hud.ApecMenu;

@Pseudo
@Mixin(ApecMenu.class)
public class Mixin_ApecMenu {

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void oneconfig$registerHudCompat(CallbackInfo ci) {
        ApecCompat.initialize();
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$suppressWhileEditing(CallbackInfo ci) {
        if (CompatOverlayRenderer.oneConfigScreenOpen()) {
            ci.cancel();
        }
    }
}
*///? }
