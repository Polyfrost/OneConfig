package org.polyfrost.oneconfig.internal.mixin.events;

//#if MC <= 1.12.2
//$ import net.minecraft.client.gui.GuiScreen;
//$ import org.lwjgl.input.Keyboard;
//$ import org.polyfrost.oneconfig.api.event.v1.EventManager;
//$ import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
//$ import org.spongepowered.asm.mixin.Mixin;
//$ import org.spongepowered.asm.mixin.injection.At;
//$ import org.spongepowered.asm.mixin.injection.Inject;
//$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$
//$ @Mixin(GuiScreen.class)
//$ public class Mixin_KeyInputEvent_Screen {
//$     @Inject(
//$             method = "handleInput",
//$             at = @At(
//$                     value = "INVOKE",
//#if FORGE
//$                     target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", ordinal = 2, remap = false
//#else
//$                     //$$ target = "Lnet/minecraft/client/gui/GuiScreen;handleKeyboardInput()V"
//#endif
//$             )
//$     )
//$     private void keyCallback(CallbackInfo ci) {
//$         int state = 0;
//$         if (Keyboard.getEventKeyState()) {
//$             if (Keyboard.isRepeatEvent()) {
//$                 state = 2;
//$             } else {
//$                 state = 1;
//$             }
//$         }
//$         EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
//$     }
//$ }
//#endif
