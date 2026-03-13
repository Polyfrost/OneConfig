package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_TickEvent {

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", ordinal = 0))
    private void tickStartCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(TickEvent.Start.INSTANCE);
    }

    @Inject(method = "runTick", at = @At("TAIL"))
    private void tickEndCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(TickEvent.End.INSTANCE);
    }

}
