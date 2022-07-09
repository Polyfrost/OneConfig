package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ShutdownEvent;
import cc.polyfrost.oneconfig.events.event.StartEvent;
import net.minecraft.util.profiler.TickTimeTracker;
import cc.polyfrost.oneconfig.internal.OneConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

@Mixin(TickTimeTracker.class)
public class TickTimeTrackerMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onStart(LongSupplier longSupplier, IntSupplier intSupplier, CallbackInfo ci) {
        OneConfig.init();
        EventManager.INSTANCE.post(new StartEvent());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.INSTANCE.post(new ShutdownEvent())));
    }
}
