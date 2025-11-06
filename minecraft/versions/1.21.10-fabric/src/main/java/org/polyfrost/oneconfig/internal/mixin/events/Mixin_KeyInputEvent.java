package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class Mixin_KeyInputEvent {
    @ModifyVariable(method = "keyPress", at = @At(value = "STORE"), ordinal = 0)
    private boolean keyCallback(boolean original, long handle, int action, KeyEvent event) {
        EventManager.INSTANCE.post(new KeyInputEvent(event.key(), (char) 0, action));
        return original;
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void charCallback(long handle, CharacterEvent event, CallbackInfo ci) {
        if (handle != OmniClient.getWindowHandle()) {
            return;
        }

        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) event.codepoint(), 1));
    }
}
