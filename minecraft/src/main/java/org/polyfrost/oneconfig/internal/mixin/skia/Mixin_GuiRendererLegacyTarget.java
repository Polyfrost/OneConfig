package org.polyfrost.oneconfig.internal.mixin.skia;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//? if >= 1.21.8 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.render.GuiRenderer;
//? if >= 26.2 {
/*import net.minecraft.client.renderer.GameRenderer;
*///? } else {
import net.minecraft.client.Minecraft;
//? }
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public class Mixin_GuiRendererLegacyTarget {
    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I")
    )
    private int oneconfig$redirectGuiTargetWidth(Window instance) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.width : instance.getWidth();
    }

    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")
    )
    private int oneconfig$redirectGuiTargetHeight(Window instance) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.height : instance.getHeight();
    }

    //? if >= 26.2 {
    /*@WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(GameRenderer instance, Operation<RenderTarget> original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override : original.call();
    }
    *///? } else {
    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(Minecraft instance, Operation<RenderTarget> original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override : original.call();
    }
    //? }
}
//? }
