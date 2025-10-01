package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.KeyboardHandler;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class Mixin_KeyInputEvent {
    @ModifyVariable(method = "keyPress", at = @At(value = "STORE"), ordinal = 0)
    private boolean keyCallback(boolean original, long handle, int key, int scancode, int action, int modifiers) {
        EventManager.INSTANCE.post(new KeyInputEvent(key, (char) 0, action));
        return original;
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    private void charCallback(long handle, int codepoint, int modifiers, CallbackInfo ci) {
        if (handle != OmniClient.getWindowHandle()) {
            return;
        }

        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) codepoint, 1));
    }
}
