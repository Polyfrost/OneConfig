package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import cc.polyfrost.oneconfig.events.event.ShutdownEvent;
import cc.polyfrost.oneconfig.events.event.StartEvent;
import cc.polyfrost.oneconfig.internal.OneConfig;
import net.minecraft.client.render.ClientTickTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientTickTracker.class)
public class TickTimeTrackerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(float f, CallbackInfo ci) {
        EventManager.INSTANCE.post(new StartEvent());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.INSTANCE.post(new ShutdownEvent())));
        EventManager.INSTANCE.post(new InitializationEvent());
        OneConfig.init();
    }
}
