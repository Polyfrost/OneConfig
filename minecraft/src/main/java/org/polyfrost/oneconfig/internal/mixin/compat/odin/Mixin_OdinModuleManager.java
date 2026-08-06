package org.polyfrost.oneconfig.internal.mixin.compat.odin;

//? odin_compat {
/*import com.odtheking.odin.features.ModuleManager;
import org.polyfrost.oneconfig.internal.compat.OdinCompat;
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(ModuleManager.class)
public class Mixin_OdinModuleManager {

    @Inject(method = "render", at = @At("HEAD"), require = 0, cancellable = true)
    private void oneconfig$registerOdinHuds(CallbackInfo ci) {
        OdinCompat.ensureRegistered();
        if (CompatOverlayRenderer.oneConfigScreenOpen()) {
            ci.cancel();
        }
    }
}
*///? }
