package org.polyfrost.oneconfig.internal.mixin.events;

//~ gui_graphics
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.polyfrost.oneconfig.internal.OneConfig;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class Mixin_HudRenderEvent {

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderHudCallback(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
        OneConfig.render(ctx, deltaTracker.getRealtimeDeltaTicks());
        SkiaCtx.INSTANCE.blitHud(ctx);
    }

}
