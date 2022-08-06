package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {
    @Final
    @Shadow
    private RenderTickCounter renderTickCounter;

    @Inject(method = "stop", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        EventManager.INSTANCE.post(new PreShutdownEvent());
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", ordinal = 0))
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.START, renderTickCounter.tickDelta));
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V", shift = At.Shift.AFTER, ordinal = 4))
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.END, renderTickCounter.tickDelta));
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V", ordinal = 0))
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.START));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.END));
    }

    @Inject(method = "openScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;requestRespawn()V", shift = At.Shift.BY, by = 2), cancellable = true)
    private void onGuiOpenEvent(Screen screen, CallbackInfo ci) {
        ScreenOpenEvent event = new ScreenOpenEvent(screen);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderTickCounter;beginRenderTick(J)I", shift = At.Shift.AFTER))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(renderTickCounter, true));
    }
}