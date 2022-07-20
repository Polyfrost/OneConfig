//#if MC==10809
package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.internal.OneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private Timer timer;

    @Inject(method = "shutdownMinecraftApplet", at = @At("HEAD"))
    private void onShutdown(CallbackInfo ci) {
        EventManager.INSTANCE.post(new PreShutdownEvent());
    }

    @Inject(method = "startGame", at = @At("HEAD"))
    private void onStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new StartEvent());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> EventManager.INSTANCE.post(new ShutdownEvent())));
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;onInitializationComplete()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onInit(CallbackInfo ci) {
        EventManager.INSTANCE.post(new InitializationEvent());
        OneConfig.init();
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickStart(F)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.START, timer.renderPartialTicks));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickEnd(F)V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new RenderEvent(Stage.END, timer.renderPartialTicks));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPreClientTick()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.START));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPostClientTick()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TickEvent(Stage.END));
    }

    @ModifyArg(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false), remap = true)
    private Event onGuiOpenEvent(Event a) {
        if (a instanceof GuiOpenEvent) {
            GuiOpenEvent forgeEvent = (GuiOpenEvent) a;
            ScreenOpenEvent event = new ScreenOpenEvent(forgeEvent.gui);
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                forgeEvent.setCanceled(true);
            }
            return forgeEvent;
        }
        return a;
    }

    @Inject(method = "runGameLoop", at = @At(value = "FIELD", target = "Lnet/minecraft/util/Timer;renderPartialTicks:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void onNonDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(timer, false));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.AFTER, ordinal = 1))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(timer, true));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireKeyInput()V"))
    private void onKeyEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;fireMouseInput()V"))
    private void onMouseEvent(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent());
    }
}
//#endif