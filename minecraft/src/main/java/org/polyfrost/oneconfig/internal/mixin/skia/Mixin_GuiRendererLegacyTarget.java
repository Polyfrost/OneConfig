package org.polyfrost.oneconfig.internal.mixin.skia;

//? if >= 26.1 {
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
//? if >= 26.2 {
import net.minecraft.client.renderer.GameRenderer;
//? } else {
/*import net.minecraft.client.Minecraft;
*///? }
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudOffscreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderer.class)
public class Mixin_GuiRendererLegacyTarget {
    //? if >= 26.2 {
    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(GameRenderer instance) {
        RenderTarget override = LegacyHudOffscreen.INSTANCE.redirectTarget;
        return override != null ? override : instance.mainRenderTarget();
    }
    //? } else {
    /*@Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(Minecraft instance) {
        RenderTarget override = LegacyHudOffscreen.INSTANCE.redirectTarget;
        return override != null ? override : instance.getMainRenderTarget();
    }
    *///? }
}
//? }
