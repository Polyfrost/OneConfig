package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
//? < 1.21.10 {
/*import net.minecraft.client.gui.components.DebugScreenOverlay;
*///? } else {
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
//? }
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? < 1.21.10 {
/*@Mixin(DebugScreenOverlay.class)
public abstract class Mixin_DebugOpenEvent {
    @Inject(method = "toggleOverlay", at = @At("TAIL"))
    private void onDebugOpen(CallbackInfo ci) {
        if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            EventManager.INSTANCE.post(HudEvent.Debug.OPENED);
        } else EventManager.INSTANCE.post(HudEvent.Debug.CLOSED);
    }
}
*///? } else {
@Mixin(DebugScreenEntryList.class)
public abstract class Mixin_DebugOpenEvent {
    //? < 1.21.11 {
    /*@Inject(method = "toggleF3Visible", at = @At("TAIL"))
    *///? } else {
    @Inject(method = "toggleDebugOverlay", at = @At("TAIL"))
    //? }
    private void onDebugOpen(CallbackInfo ci) {
        if (Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            EventManager.INSTANCE.post(HudEvent.Debug.OPENED);
        } else EventManager.INSTANCE.post(HudEvent.Debug.CLOSED);
    }
}
//? }
