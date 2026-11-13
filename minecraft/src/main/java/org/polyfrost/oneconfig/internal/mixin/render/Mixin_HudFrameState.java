package org.polyfrost.oneconfig.internal.mixin.render;

import org.polyfrost.oneconfig.api.ui.v1.platform.HudFrameHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import net.minecraft.client.DeltaTracker;
//?}

//? if >= 26.2 {
import net.minecraft.client.gui.Gui;
//?} elif > 1.8.9 {
/*import net.minecraft.client.renderer.GameRenderer;
*///?} else
//import net.minecraft.client.gui.GameGui;

//? if >= 26.2 {
@Mixin(Gui.class)
//?} elif > 1.8.9 {
/*@Mixin(GameRenderer.class)
*///?} else
//@Mixin(GameGui.class)
public class Mixin_HudFrameState {
    //? if >= 26.1 {
    //~ if >= 26.2 'extractGui' -> 'extractRenderState'
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void oneconfig$beginHudFrame(DeltaTracker deltaTracker, boolean renderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        HudFrameHooks.beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
    //?} elif > 1.8.9 {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void oneconfig$beginHudFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        HudFrameHooks.beginFrame(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
    *///?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void oneconfig$beginHudFrame(float partialTicks, CallbackInfo ci) {
        HudFrameHooks.beginFrame(partialTicks);
    }
    *///?}
}
