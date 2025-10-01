package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.KeyInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_KeyInputEvent {
    //#if MC <= 1.8.9
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V", ordinal = 1))
    //#else
    //$$ @Inject(method = "tickKeyboard", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;f3CTime:J", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    //#endif
    private void keyCallback(CallbackInfo ci) {
        int state = 0;
        if (Keyboard.getEventKeyState()) {
            if (Keyboard.isRepeatEvent()) {
                state = 2;
            } else {
                state = 1;
            }
        }

        EventManager.INSTANCE.post(new KeyInputEvent(Keyboard.getEventKey(), Keyboard.getEventCharacter(), state));
    }
}
