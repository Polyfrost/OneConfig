package org.polyfrost.oneconfig.internal.mixin.skia;

//? if >= 1.21.10 {
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
public class Mixin_VulkanModBlurSnapshot {

    @WrapOperation(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V")
    )
    private void oneconfig$snapshotWorldAndHud(GameRenderer instance, Operation<Void> original) {
        if (SkiaCtx.INSTANCE.consumeBlurSnapshotRequest()) {
            SkiaCtx.INSTANCE.takeWorldSnapshotIfNeeded();
        } else {
            original.call();
        }
    }
}
//? }
