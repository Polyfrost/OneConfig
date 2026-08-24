package org.polyfrost.oneconfig.internal.mixin.events;

//~ gui_graphics
//? if > 1.8.9 {
import net.minecraft.client.DeltaTracker;
//? >= 26.2 {
import net.minecraft.client.gui.Hud;
//? } else {
/*import net.minecraft.client.gui.Gui;
*///? }
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GameGui;
import org.polyfrost.oneconfig.internal.OneConfig;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
@Mixin(Hud.class)
//?} elif > 1.8.9 {
/*@Mixin(Gui.class)
*///?} else
//@Mixin(GameGui.class)
public class Mixin_HudRenderEvent {

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    //~ if = 1.8.9 '(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker' -> '(float partialTicks'
    private void renderHudCallback(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
        //~ if = 1.8.9 '(ctx, deltaTracker.getRealtimeDeltaTicks())' -> '(partialTicks)'
        OneConfig.render(ctx, deltaTracker.getRealtimeDeltaTicks());
        //~ if < 1.21.8 '.suppressInGameHudRender' -> '.shouldSuppressInGameHudRender()'
        if (!SkiaCtx.INSTANCE.suppressInGameHudRender) {
            //~ if = 1.8.9 '(ctx)' -> '()'
            SkiaCtx.INSTANCE.blitHud(ctx);
        }
    }

}
