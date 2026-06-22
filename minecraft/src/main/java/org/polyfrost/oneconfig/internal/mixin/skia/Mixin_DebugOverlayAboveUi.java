package org.polyfrost.oneconfig.internal.mixin.skia;

//~ gui_graphics
//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.render.Window;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.polyfrost.oneconfig.internal.ui.hud.DebugOverlayOffscreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if = 1.8.9 'DebugScreenOverlay' -> 'DebugOverlay'
@Mixin(DebugScreenOverlay.class)
public class Mixin_DebugOverlayAboveUi {
    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    //~ if = 1.8.9 'GuiGraphicsExtractor ctx' -> 'Window window'
    private void oneconfig$deferDebugOverlay(GuiGraphicsExtractor ctx, CallbackInfo ci) {
        //~ if = 1.8.9 'shouldSuppressVanilla()' -> 'shouldSuppressVanilla((DebugOverlay) (Object) this, window)'
        if (DebugOverlayOffscreen.INSTANCE.shouldSuppressVanilla()) {
            ci.cancel();
        }
    }
}
