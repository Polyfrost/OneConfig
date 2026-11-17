//~ gui_graphics
package org.polyfrost.oneconfig.internal.mixin.events;

import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.internal.OneConfig;
//? if = 1.8.9
//import org.polyfrost.oneconfig.internal.LegacyVanillaHudSampler;
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Hud;
//?}

//? if > 1.8.9 {
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}

//? if < 26.2 && > 1.8.9 {
/*import net.minecraft.client.gui.Gui;
*///?}

//? if = 1.8.9 {
/*import net.minecraft.client.gui.GameGui;
*///?}

//? if >= 26.2 {
@Mixin(Hud.class)
//?} elif > 1.8.9 {
/*@Mixin(Gui.class)
*///?} else
//@Mixin(GameGui.class)
public class Mixin_HudRenderEvent {

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    //~ if = 1.8.9 '(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker' -> '(float partialTicks'
    private void beginVanillaFootprint(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudFootprint.begin();
    }

    //~ if >= 26.1 'render' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    //~ if = 1.8.9 '(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker' -> '(float partialTicks'
    private void renderHudCallback(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
        VanillaHudFootprint.end(HudManager.guiScreenWidth, HudManager.guiScreenHeight);
        //? if = 1.8.9
        //LegacyVanillaHudSampler.sample(this);


        //~ if = 1.8.9 'submitHud(ctx)' -> 'render()'
        OneConfig.submitHud(ctx);
    }

}
