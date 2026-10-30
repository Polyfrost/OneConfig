package org.polyfrost.oneconfig.internal.mixin.skia;

import org.polyfrost.oneconfig.internal.ui.compose.GlHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import com.mojang.blaze3d.platform.Window;
//?}

//? if = 1.8.9 {
/*import net.minecraft.client.Minecraft;
*///?}

//~ if = 1.8.9 'Window' -> 'Minecraft'
@Mixin(Window.class)
public class Mixin_SkiaFrame {
    //? if > 1.8.9 {
    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Inject(method = "refreshFramebufferSize", at = @At("TAIL"))
    void impl$onResize(CallbackInfo ci) {
        GlHost.INSTANCE.recreateSurface(this.framebufferWidth, this.framebufferHeight);
    }
    //?} else {
    /*@Inject(method = "resize", at = @At("TAIL"))
    void impl$onResize(int width, int height, CallbackInfo ci) {
        GlHost.INSTANCE.recreateSurface(width, height);
    }
    *///?}

    //? if < 26.1 {
    /*//? if >= 1.21.4 {
    @Inject(method = "updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V", at = @At("HEAD"))
    //?} elif > 1.8.9 {
    /^@Inject(method = "updateDisplay()V", at = @At("HEAD"))
    ^///?} else
    //@Inject(method = "runGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/pipeline/RenderTarget;draw(II)V"))
    void impl$onDraw(CallbackInfo ci) {
        //? if > 1.8.9 {
        GlHost.INSTANCE.drawAtPresent();
        //?} else
        //GlHost.INSTANCE.drawBeforeBlit();
    }
    *///? }
}
