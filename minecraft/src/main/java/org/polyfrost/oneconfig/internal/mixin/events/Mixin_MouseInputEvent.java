package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class Mixin_MouseInputEvent {
    //? >= 1.21.10 {
    @ModifyVariable(method = "onButton", at = @At("STORE"), ordinal = 0)
    private int mouseCallback(int button, long handle, MouseButtonInfo event, int action) {
        EventManager.INSTANCE.post(new MouseInputEvent(button, action));
        return button;
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }

    //? } else {
    /*@Inject(method = "onPress", at = @At("HEAD"))
    private void mouseCallback(long handle, int button, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(button, action));
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        if (OmniScreens.isInScreen()) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }
    *///? }
}
