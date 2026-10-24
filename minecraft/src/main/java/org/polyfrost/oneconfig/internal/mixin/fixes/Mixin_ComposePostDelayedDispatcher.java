package org.polyfrost.oneconfig.internal.mixin.fixes;

import kotlin.coroutines.CoroutineContext;
import org.polyfrost.oneconfig.internal.ui.compose.RenderThreadDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "androidx.compose.ui.Actuals_desktopKt", remap = false)
public class Mixin_ComposePostDelayedDispatcher {
    @Inject(method = "getPostDelayedDispatcher", at = @At("HEAD"), cancellable = true)
    private static void oneconfig$runOnClientThread(CallbackInfoReturnable<CoroutineContext> cir) {
        cir.setReturnValue(RenderThreadDispatcher.INSTANCE);
    }
}
