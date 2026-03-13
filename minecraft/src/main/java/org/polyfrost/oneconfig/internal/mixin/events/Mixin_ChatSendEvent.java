package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.player.LocalPlayer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class Mixin_ChatSendEvent {
//    @Unique private ChatEvent.Send ocfg$chatEvent;
//
//    @Inject(
//            method = "sendChat",
//            at = @At("HEAD"),
//            cancellable = true
//    )
//    public void chatCallback(String message, CallbackInfo ci) {
//        // if (org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals.executeCommand(message)) {
//        //     ci.cancel();
//        // }
//
//        ocfg$chatEvent = new ChatEvent.Send(message);
//        EventManager.INSTANCE.post(ocfg$chatEvent);
//
//        if (ocfg$chatEvent.cancelled) {
//            ci.cancel();
//        }
//    }
//
//    @ModifyVariable(
//            method = "sendChat",
//            at = @At("HEAD"),
//            ordinal = 0,
//            argsOnly = true
//    )
//    public String modifyMessage(String message) {
//        return ocfg$chatEvent != null ? ocfg$chatEvent.message : message;
//    }
}
