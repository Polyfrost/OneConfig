package org.polyfrost.oneconfig.internal.mixin.events;

//#if MC <= 1.12.2
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class Mixin_MouseInputEvent_Screen {
    @Unique private int ocfg$mouseX, ocfg$mouseY;

    @Inject(
            method = "handleInput",
            at = @At(
                    value = "INVOKE",
                    //#if FORGE
                    target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", ordinal = 0, remap = false
                    //#else
                    //$$ target = "Lnet/minecraft/client/gui/GuiScreen;handleMouseInput()V"
                    //#endif
            )
    )
    private void mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if(mouseX != ocfg$mouseX || mouseY != ocfg$mouseY) {
            MouseInputEvent.Moved.post(mouseX, mouseY);
        }
        ocfg$mouseX = mouseX;
        ocfg$mouseY = mouseY;
    }
}
//#endif
