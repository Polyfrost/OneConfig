package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
//? if > 1.8.9 {
//? < 1.21.10 {
/*import net.minecraft.client.gui.components.DebugScreenOverlay;
*///? } else {
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
//? }
//?} else
//import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? < 1.21.10 {
/*//~ if = 1.8.9 'DebugScreenOverlay' -> 'Minecraft'
@Mixin(DebugScreenOverlay.class)
public abstract class Mixin_DebugOpenEvent {
    //? if > 1.8.9 {
    @Inject(method = "toggleOverlay", at = @At("TAIL"))
    //?} else
    //@Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;debugEnabled:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
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
