package org.polyfrost.oneconfig.internal.mixin.skia;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.polyfrost.oneconfig.internal.ui.compose.ComposePreloader;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.11 {
import net.minecraft.util.Util;
//?}

//? if < 1.21.11 {
/*import net.minecraft.Util;
*///?}

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
        if (!SkiaCtx.INSTANCE.isReady()) ComposePreloader.INSTANCE.fail("Skia context unavailable", null);
        boolean hold = !ComposePreloader.INSTANCE.getStopped();
        if (hold) {
            long now = System.nanoTime();
            if (!this.oneconfig$holdingFade) this.oneconfig$holdStartedNanos = now;
            if (now - this.oneconfig$holdStartedNanos >= ONECONFIG_HOLD_TIMEOUT_NANOS) {
                ComposePreloader.INSTANCE.fail("startup held for more than 15 seconds", null);
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
