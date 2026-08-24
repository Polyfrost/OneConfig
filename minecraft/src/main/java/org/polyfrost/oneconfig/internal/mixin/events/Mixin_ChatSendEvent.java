package org.polyfrost.oneconfig.internal.mixin.events;

//? if > 1.8.9 {
import net.minecraft.client.multiplayer.ClientPacketListener;
//?} else
//import net.minecraft.client.player.LocalPlayer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
//? if = 1.8.9
//import org.polyfrost.oneconfig.internal.legacy.command.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if = 1.8.9 'ClientPacketListener' -> 'LocalPlayer'
@Mixin(ClientPacketListener.class)
public abstract class Mixin_ChatSendEvent {
    @Unique private ChatEvent.Send ocfg$chatEvent;

    @Inject(
            method = "sendChat",
            at = @At("HEAD"),
            cancellable = true
    )
    public void chatCallback(String message, CallbackInfo ci) {
        ocfg$chatEvent = new ChatEvent.Send(message);
        EventManager.INSTANCE.post(ocfg$chatEvent);

        if (ocfg$chatEvent.cancelled) {
            ci.cancel();
            return;
        }

        //? if = 1.8.9 {
        /*String commandMessage = ocfg$chatEvent.message;
        if (commandMessage.startsWith("/") && ClientCommandInternals.executeCommand(commandMessage.substring(1))) {
            ci.cancel();
        }
        *///?}
    }

    @ModifyVariable(
            method = "sendChat",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    public String modifyMessage(String message) {
        return ocfg$chatEvent != null ? ocfg$chatEvent.message : message;
    }
}
