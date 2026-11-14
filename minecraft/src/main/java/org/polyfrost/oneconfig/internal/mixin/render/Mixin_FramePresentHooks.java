package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.10 && < 26.2
//import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.ui.FramePresentHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
@Mixin(value = Minecraft.class, priority = 1500)
//? } else if >= 1.21.10 {
/*@Mixin(RenderTarget.class)
*///? } else {
/*@Mixin(Minecraft.class)
*///? }
public class Mixin_FramePresentHooks {

    //? if >= 26.2 {
    @Inject(
            method = "renderFrame",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"
            )
    )
    private void oneconfig$beforePresent(CallbackInfo ci) {
        FramePresentHooks.run();
    }
    //? } else if >= 1.21.10 {
    /*@Inject(method = "blitToScreen", at = @At("HEAD"))
    private void oneconfig$beforePresent(CallbackInfo ci) {
        RenderTarget self = (RenderTarget) (Object) this;
        Minecraft client = Minecraft.getInstance();
        if (client == null || self != client.getMainRenderTarget()) return;
        FramePresentHooks.run();
    }
    *///? }

}
