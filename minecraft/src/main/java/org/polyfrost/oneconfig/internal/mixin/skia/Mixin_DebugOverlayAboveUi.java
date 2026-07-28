package org.polyfrost.oneconfig.internal.mixin.skia;

//~ gui_graphics
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.polyfrost.oneconfig.internal.ui.hud.DebugOverlayOffscreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public class Mixin_DebugOverlayAboveUi {
    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void oneconfig$deferDebugOverlay(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        if (DebugOverlayOffscreen.INSTANCE.shouldSuppressVanilla()) {
            ci.cancel();
        }
    }
}
