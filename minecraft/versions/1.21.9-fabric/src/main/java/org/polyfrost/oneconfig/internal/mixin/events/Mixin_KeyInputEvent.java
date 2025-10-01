package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.client.Keyboard;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class Mixin_KeyInputEvent {
    @ModifyVariable(method = "onKey", at = @At(value = "STORE"), ordinal = 0)
    private boolean keyCallback(boolean original, long handle, int action, KeyInput event) {
        EventManager.INSTANCE.post(new KeyInputEvent(event.getKeycode(), (char) 0, action));
        return original;
    }

    @Inject(method = "onChar", at = @At("HEAD"))
    private void charCallback(long handle, CharInput event, CallbackInfo ci) {
        if (handle != OmniClient.getWindowHandle()) {
            return;
        }

        EventManager.INSTANCE.post(new KeyInputEvent(0, (char) event.comp_4793(), 1));
    }
}
