package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.ui.compose.UiBlur;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class Mixin_UiBlur {
    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void oneconfig$uiBlur(CallbackInfo ci) {
        if (UiBlur.process()) ci.cancel();
    }
}
//?}
