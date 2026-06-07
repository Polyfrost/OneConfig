package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ServerJoinEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientHandshakePacketListenerImpl.class)
public class Mixin_ServerJoinEvent {

    //? < 1.21.4 {
    /*@Inject(method = "handleGameProfile", at = @At("RETURN"))
    *///? } else {
    @Inject(method = "handleLoginFinished", at = @At("RETURN"))
    //? }
    private void onLoginSuccess(CallbackInfo ci) {
        EventManager.INSTANCE.post(ServerJoinEvent.INSTANCE);
    }

}
