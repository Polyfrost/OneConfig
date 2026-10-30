package org.polyfrost.oneconfig.internal.mixin.skia;

//? if < 1.21.11 {
/*import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.polyfrost.oneconfig.internal.ui.compose.GlHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import net.minecraft.client.gui.GuiGraphics;
//?} else
//import net.minecraft.client.render.Window;

//~ if = 1.8.9 'DebugScreenOverlay' -> 'DebugOverlay'
@Mixin(DebugScreenOverlay.class)
public class Mixin_DebugOverlayAboveUi {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    //~ if = 1.8.9 'GuiGraphics graphics' -> 'Window window'
    private void oneconfig$deferAboveUi(GuiGraphics graphics, CallbackInfo ci) {
        //~ if = 1.8.9 'DebugScreenOverlay' -> 'DebugOverlay'
        if (GlHost.INSTANCE.deferDebugScreen((DebugScreenOverlay) (Object) this)) ci.cancel();
    }
}
*///?}
