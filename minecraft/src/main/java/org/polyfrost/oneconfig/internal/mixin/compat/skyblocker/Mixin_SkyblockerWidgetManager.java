package org.polyfrost.oneconfig.internal.mixin.compat.skyblocker;

//? skyblocker_compat {
import de.hysky.skyblocker.skyblock.tabhud.screenbuilder.WidgetManager;
import org.polyfrost.oneconfig.internal.compat.SkyblockerWidgetCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(WidgetManager.class)
public class Mixin_SkyblockerWidgetManager {

    @Inject(method = "loadConfig", at = @At("TAIL"), require = 0)
    private static void oneconfig$registerHudCompat(CallbackInfo ci) {
        SkyblockerWidgetCompat.initialize();
    }
}
//? }
