package cc.polyfrost.oneconfig.mixin;

import cc.polyfrost.oneconfig.OneConfig;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.libs.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private Timer timer;

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;beginMinecraftLoading(Lnet/minecraft/client/Minecraft;Ljava/util/List;Lnet/minecraft/client/resources/IReloadableResourceManager;)V", remap = false), remap = true)
    private void onPreLaunch(CallbackInfo ci) {
        OneConfig.preLaunch();
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;onInitializationComplete()V", shift = At.Shift.AFTER, remap = false), remap = true)
    private void onInit(CallbackInfo ci) {
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

    @ModifyExpressionValue(method = "displayGuiScreen", at = @At(value = "NEW", target = "Lnet/minecraftforge/client/event/GuiOpenEvent;<init>(Lnet/minecraft/client/gui/GuiScreen;)V", remap = false), remap = true)
    private GuiOpenEvent onGuiOpenEvent(GuiOpenEvent screen) {
        ScreenOpenEvent event = new ScreenOpenEvent(screen.gui);
        EventManager.INSTANCE.post(event);
        if (event.isCancelled) {
            screen.setCanceled(true);
        }
        return screen;
    }

    @Inject(method = "runGameLoop", at = @At(value = "FIELD", target = "Lnet/minecraft/util/Timer;renderPartialTicks:F", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void onNonDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(timer, false));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Timer;updateTimer()V", shift = At.Shift.AFTER, ordinal = 1))
    private void onDeltaTickTimerUpdate(CallbackInfo ci) {
        EventManager.INSTANCE.post(new TimerUpdateEvent(timer, true));
    }
}
