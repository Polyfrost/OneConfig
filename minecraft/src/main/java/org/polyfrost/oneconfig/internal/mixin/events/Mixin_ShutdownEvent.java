package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ShutdownEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_ShutdownEvent {
    @Unique
    private static boolean oneconfig$shutdownPosted;

    @Inject(method = "stop", at = @At("HEAD"))
    private void shutdownCallback(CallbackInfo ci) {
        if (oneconfig$shutdownPosted) return;
        oneconfig$shutdownPosted = true;
        EventManager.INSTANCE.post(ShutdownEvent.INSTANCE);
    }

}
