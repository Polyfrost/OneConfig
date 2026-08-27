package org.polyfrost.oneconfig.internal.mixin.events;

//? if >= 26.3
//import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
//? >= 1.21.10
import net.minecraft.client.input.MouseButtonInfo;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class Mixin_MouseInputEvent {
    //? >= 1.21.10 {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void mouseCallback(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        int button = buttonInfo.button();
        //? if >= 26.3 {
        /*button = switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> MouseInputEvent.BUTTON_LEFT;
            case InputConstants.MOUSE_BUTTON_RIGHT -> MouseInputEvent.BUTTON_RIGHT;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> MouseInputEvent.BUTTON_MIDDLE;
            default -> button - 1;
        };
        *///?}
        EventManager.INSTANCE.post(new MouseInputEvent(button, action));
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        //? if >= 26.2 {
        if (Minecraft.getInstance().gui.screen() != null) {
        //?} else {
        /*if (Minecraft.getInstance().screen != null) {
        *///?}
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
        if (Minecraft.getInstance().screen != null) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }
    *///? }
}
