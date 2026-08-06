package org.polyfrost.oneconfig.internal.mixin.skia;

//? if >= 1.21.10 {
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiRenderer.class)
public class Mixin_VulkanModBlurSnapshot {

    @Redirect(
            method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V")
    )
    private void oneconfig$snapshotWorldAndHud(GameRenderer instance) {
        if (SkiaCtx.INSTANCE.consumeBlurSnapshotRequest()) {
            SkiaCtx.INSTANCE.takeWorldSnapshotIfNeeded();
        } else {
            instance.processBlurEffect();
        }
    }
}
//? }
