package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.KeyboardHandler;
//? >= 1.21.10 {
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
//? }
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class Mixin_KeyInputEvent {

    //? >= 1.21.10 {
    @ModifyVariable(method = "keyPress", at = @At(value = "STORE"), ordinal = 0)
    private boolean keyCallback(boolean original, long handle, int action, KeyEvent event) {
        EventManager.INSTANCE.post(new KeyInputEvent(event.key(), (char) 0, action));
        return original;
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void charCallback(long handle, CharacterEvent event, CallbackInfo ci) {
        if (handle != Platform.compatibility().windowHandle()) {
            return;
        }

        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) event.codepoint(), 1));
    }
    //? } else {
    /*
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void keyCallback(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent(key, (char) 0, action));
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void charCallback(long window, int codepoint, int mods, CallbackInfo ci) {
        if (window != Platform.compatibility().windowHandle()) {
            return;
        }
        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) codepoint, 1));
    }
    *///? }
}
