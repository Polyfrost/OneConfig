package org.polyfrost.oneconfig.internal.mixin.events;

//? fabric {
import net.minecraft.client.gui.screens.Screen;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if >= 26.2 'Minecraft' -> 'gui.Gui'
import net.minecraft.client.gui.Gui;

// 26.2 moved Minecraft#setScreen to Gui
// Minecraft#setScreenAndShow is only a wrapper most callers bypass
//~ if >= 26.2 'Minecraft' -> 'Gui'
@Mixin(Gui.class)
public class Mixin_ScreenOpenEvent_Fabric {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void screenOpenCallback(Screen screen, CallbackInfo ci) {
        ScreenOpenEvent event = new ScreenOpenEvent(screen);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
//?}
