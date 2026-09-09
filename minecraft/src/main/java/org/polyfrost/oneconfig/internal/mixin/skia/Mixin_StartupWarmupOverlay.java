package org.polyfrost.oneconfig.internal.mixin.skia;

import net.minecraft.client.gui.screens.LoadingOverlay;
//? if >= 1.21.11 {
import net.minecraft.util.Util;
//?} else
//import net.minecraft.Util;
import org.polyfrost.oneconfig.internal.ui.compose.ComposePreloader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public class Mixin_StartupWarmupOverlay {
    @Shadow
    @Final
    private boolean fadeIn;

    @Shadow
    private long fadeOutStart;

    @Unique
    private boolean oneconfig$holdingFade;

    @Unique
    private long oneconfig$holdStartedNanos;

    @Unique
    private static final long ONECONFIG_HOLD_TIMEOUT_NANOS = 15_000_000_000L;

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void oneconfig$holdStartupReveal(CallbackInfo ci) {
        // Only hold the initial startup overlay once resource loading has finished
        if (this.fadeIn || this.fadeOutStart < 0L) return;
        boolean hold = !ComposePreloader.INSTANCE.getStopped();
        if (hold) {
            long now = System.nanoTime();
            if (!this.oneconfig$holdingFade) this.oneconfig$holdStartedNanos = now;
            if (now - this.oneconfig$holdStartedNanos >= ONECONFIG_HOLD_TIMEOUT_NANOS) {
                ComposePreloader.INSTANCE.failStartup("startup held for more than 15 seconds", null);
                hold = false;
            }
        }
        if (hold || this.oneconfig$holdingFade) {
            // Restart normal fade when warm up finishes
            this.fadeOutStart = Util.getMillis();
        }
        this.oneconfig$holdingFade = hold;
    }
}
