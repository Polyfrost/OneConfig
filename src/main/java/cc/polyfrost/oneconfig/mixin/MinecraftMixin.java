package cc.polyfrost.oneconfig.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.RenderEvent;
import cc.polyfrost.oneconfig.events.event.ScreenOpenEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import net.minecraftforge.client.event.GuiOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private Timer timer;

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickStart(F)V", shift = At.Shift.AFTER))
    private void onRenderTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.getEventBus().post(new RenderEvent(Stage.START, timer.renderPartialTicks));
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onRenderTickEnd(F)V", shift = At.Shift.AFTER))
    private void onRenderTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.getEventBus().post(new RenderEvent(Stage.END, timer.renderPartialTicks));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPreClientTick()V", shift = At.Shift.AFTER))
    private void onClientTickStart(CallbackInfo ci) {
        EventManager.INSTANCE.getEventBus().post(new TickEvent(Stage.START));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;onPostClientTick()V", shift = At.Shift.AFTER))
    private void onClientTickEnd(CallbackInfo ci) {
        EventManager.INSTANCE.getEventBus().post(new TickEvent(Stage.END));
    }

    @ModifyExpressionValue(method = "displayGuiScreen", at = @At(value = "NEW", target = "Lnet/minecraftforge/client/event/GuiOpenEvent;<init>(Lnet/minecraft/client/gui/GuiScreen;)V"))
    private GuiOpenEvent onGuiOpenEvent(GuiOpenEvent screen) {
        ScreenOpenEvent event = new ScreenOpenEvent(screen.gui);
        EventManager.INSTANCE.getEventBus().post(event);
        if (event.isCancelled) {
            screen.setCanceled(true);
        }
        return screen;
    }
}
