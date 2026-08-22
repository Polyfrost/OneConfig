package org.polyfrost.oneconfig.internal.mixin.events;

//? fabric || ornithe {
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
//? if = 1.8.9
//import org.polyfrost.oneconfig.internal.legacy.LegacyPanoramaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_ScreenOpenEvent_Fabric {
    //? if > 1.8.9 {
    //~ if >= 26.2 'setScreen' -> 'setScreenAndShow'
    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    //?} else
    //@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void screenOpenCallback(Screen screen, CallbackInfo ci) {
        //? if = 1.8.9
        //LegacyPanoramaTracker.capture(((Minecraft) (Object) this).screen);
        ScreenOpenEvent event = new ScreenOpenEvent(screen);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
//? }
