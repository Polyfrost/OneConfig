package org.polyfrost.oneconfig.internal.mixin.skia;

//? if >= 1.21.8 {
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.polyfrost.oneconfig.internal.ui.hud.GuiTargetRedirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.2 {
import net.minecraft.client.renderer.GameRenderer;
//?}

//? if >= 26.1 {
import org.objectweb.asm.Opcodes;
//?}

//? if < 26.2 {
/*import net.minecraft.client.Minecraft;
*///?}

@Mixin(GuiRenderer.class)
public class Mixin_GuiRendererLegacyTarget {
    @ModifyExpressionValue(
            method = "draw",
            //? if >= 26.1 {
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/WindowRenderState;guiScale:I", opcode = Opcodes.GETFIELD)
            //?} else
            //at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I")
    )
    private int oneconfig$itemAtlasProjectionScale(int original) {
        return GuiTargetRedirect.itemRenderSizePx > 0 ? 1 : original;
    }

    //? if >= 26.1 {
    @ModifyExpressionValue(
            method = "draw",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/WindowRenderState;width:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int oneconfig$modifyGuiTargetWidth(int original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.width : original;
    }

    @ModifyExpressionValue(
            method = "draw",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/WindowRenderState;height:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int oneconfig$modifyGuiTargetHeight(int original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.height : original;
    }
    //? } else {
    /*@ModifyExpressionValue(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I")
    )
    private int oneconfig$modifyGuiTargetWidth(int original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.width : original;
    }

    @ModifyExpressionValue(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I")
    )
    private int oneconfig$modifyGuiTargetHeight(int original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override.height : original;
    }
    *///? }

    //? if >= 26.2 {
    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(GameRenderer instance, Operation<RenderTarget> original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override : original.call(instance);
    }
    //? } else {
    /*@WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget oneconfig$redirectGuiTarget(Minecraft instance, Operation<RenderTarget> original) {
        RenderTarget override = GuiTargetRedirect.INSTANCE.target;
        return override != null ? override : original.call(instance);
    }
    *///? }
}
//? }
