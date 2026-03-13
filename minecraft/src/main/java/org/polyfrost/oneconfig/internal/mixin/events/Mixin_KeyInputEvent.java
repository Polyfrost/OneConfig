package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import net.minecraft.client.KeyboardHandler;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class Mixin_KeyInputEvent {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void keyCallback(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent(key, (char) 0, action));
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void charCallback(long window, int codepoint, int mods, CallbackInfo ci) {
        if (window != OmniClient.getWindowHandle()) {
            return;
        }
        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) codepoint, 1));
    }
}
