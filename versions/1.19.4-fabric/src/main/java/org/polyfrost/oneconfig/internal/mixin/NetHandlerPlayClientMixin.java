package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.ChatReceiveEvent;
import org.polyfrost.oneconfig.api.events.event.ChatSendEvent;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class NetHandlerPlayClientMixin {
    @Unique
    private ChatSendEvent ocfg$sendchatevent;

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfoReturnable<Boolean> cir) {
        if (ClientCommandInternals.executeCommand(command)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(String command, CallbackInfo info) {
        if (ClientCommandInternals.executeCommand(command)) {
            info.cancel();
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        ocfg$sendchatevent = new ChatSendEvent(message);
        EventManager.INSTANCE.post(ocfg$sendchatevent);
        if (ocfg$sendchatevent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String modifyMessage(String message) {
        return ocfg$sendchatevent.message;
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ChatReceiveEvent ev = new ChatReceiveEvent(packet.unsignedContent());
        EventManager.INSTANCE.post(ev);
        if(ev.cancelled) {
            ci.cancel();
        }
    }

}
