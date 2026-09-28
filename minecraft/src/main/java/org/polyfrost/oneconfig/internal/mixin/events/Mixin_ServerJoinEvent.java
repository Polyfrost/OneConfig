package org.polyfrost.oneconfig.internal.mixin.events;

import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ServerJoinEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
import net.minecraft.client.multiplayer.ClientPacketListener;
//?}

//? if = 1.8.9 {
/*import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
*///?}

//~ if = 1.8.9 'ClientPacketListener' -> 'ClientPlayNetworkHandler'
@Mixin(ClientPacketListener.class)
public class Mixin_ServerJoinEvent {

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void onLoginSuccess(CallbackInfo ci) {
        EventManager.INSTANCE.post(ServerJoinEvent.INSTANCE);
    }

}
