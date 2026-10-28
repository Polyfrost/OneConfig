package org.polyfrost.oneconfig.internal.mixin.skia;

//? if > 1.8.9 && < 1.21.11 {
/*import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.ui.compose.GlHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_GlHostPresent {
    //~ if < 1.21.5 'blitToScreen()V' -> 'blitToScreen(II)V'
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen()V"), require = 0)
    private void oneconfig$beforeBlit(boolean tick, CallbackInfo ci) {
        GlHost.INSTANCE.drawBeforeBlit();
    }
}
*///?}
