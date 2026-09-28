package org.polyfrost.oneconfig.internal.mixin.fixes;

import kotlin.coroutines.CoroutineContext;
import org.polyfrost.oneconfig.internal.ui.compose.RenderThreadDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Compose runs RectManager's debounced dispatch on the AWT event thread, which races the client thread
 * that owns our scenes and corrupts RectList ("LayoutNode X not found in RectList", CMP-7153/CMP-10678)
 */
@Mixin(targets = "androidx.compose.ui.Actuals_desktopKt", remap = false)
public class Mixin_ComposePostDelayedDispatcher {
    @Inject(method = "getPostDelayedDispatcher", at = @At("HEAD"), cancellable = true)
    private static void oneconfig$runOnClientThread(CallbackInfoReturnable<CoroutineContext> cir) {
        cir.setReturnValue(RenderThreadDispatcher.INSTANCE);
    }
}
