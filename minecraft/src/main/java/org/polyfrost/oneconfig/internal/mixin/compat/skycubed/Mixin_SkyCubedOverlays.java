package org.polyfrost.oneconfig.internal.mixin.compat.skycubed;

//? skycubed_compat {
import me.owdding.lib.overlays.Overlays;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(Overlays.class)
public class Mixin_SkyCubedOverlays {

    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$suppressWhileEditing(CallbackInfo ci) {
        if (CompatOverlayRenderer.oneConfigScreenOpen()) {
            ci.cancel();
        }
    }
}
//? }
