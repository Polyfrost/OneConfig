package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.options.OmniVideoSettings;
import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.20.4
//$$ @Mixin(net.minecraft.client.gui.components.DebugScreenOverlay.class)
//$$ public abstract class Mixin_DebugOpenEvent {
//$$     @Inject(method = "toggleOverlay", at = @At("TAIL"))
//$$     private void onDebugOpen(CallbackInfo ci) {
//$$         if (OmniVideoSettings.isDebugRendering()) {
//$$             EventManager.INSTANCE.post(HudEvent.Debug.OPENED);
//$$         } else EventManager.INSTANCE.post(HudEvent.Debug.CLOSED);
//$$     }
//$$ }
//#else
//#if MC <= 1.13
@Mixin(net.minecraft.client.Minecraft.class)
//#else
//$$ @Mixin(net.minecraft.client.KeyboardHandler.class)
//#endif
public abstract class Mixin_DebugOpenEvent {
    //#if MC <= 1.13
    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;showDebugInfo:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "keyPress", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;renderDebug:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    //#endif
    private void onDebugOpen(CallbackInfo ci) {
        if (OmniVideoSettings.isDebugRendering()) {
            EventManager.INSTANCE.post(HudEvent.Debug.OPENED);
        } else {
            EventManager.INSTANCE.post(HudEvent.Debug.CLOSED);
        }
    }
}
//#endif

