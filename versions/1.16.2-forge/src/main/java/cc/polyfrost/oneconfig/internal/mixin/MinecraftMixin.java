package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Final
    @Shadow
    private Timer timer;

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        EventManager.INSTANCE.post(new PreShutdownEvent());
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/hooks/BasicEventHooks;onRenderTickStart(F)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.START, timer.renderPartialTicks));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/hooks/BasicEventHooks;onRenderTickEnd(F)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.END, timer.renderPartialTicks));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/hooks/BasicEventHooks;onPreClientTick()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.START));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/hooks/BasicEventHooks;onPostClientTick()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.END));
    }

    @ModifyArg(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", remap = false), remap = true)
    private Event onGuiOpenEvent(Event a) {
        if (a instanceof GuiOpenEvent) {
            GuiOpenEvent forgeEvent = (GuiOpenEvent) a;
            ScreenOpenEvent event = new ScreenOpenEvent(forgeEvent.getGui());
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                forgeEvent.setCanceled(true);
            }
            return forgeEvent;
        }
        return a;
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;getPartialTicks(J)I", shift = At.Shift.AFTER))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(timer, true));
    }
}