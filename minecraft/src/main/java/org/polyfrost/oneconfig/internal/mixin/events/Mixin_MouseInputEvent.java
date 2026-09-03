package org.polyfrost.oneconfig.internal.mixin.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
//? if > 1.8.9 {
import net.minecraft.client.MouseHandler;
//? >= 1.21.10
import net.minecraft.client.input.MouseButtonInfo;
//?} else {
/*import org.lwjgl.input.Mouse;
import pl.tomgirl.lenis.window.DisplaySdl;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if > 1.8.9 {
@Mixin(MouseHandler.class)
//?} else
//@Mixin(value = Mouse.class, remap = false)
public class Mixin_MouseInputEvent {
    //? >= 1.21.10 {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void mouseCallback(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(buttonInfo.button(), oneconfig$state(action)));
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

    //?} elif > 1.8.9 {
    /*@Inject(method = "onPress", at = @At("HEAD"))
    private void mouseCallback(long handle, int button, int action, int mods, CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(button, oneconfig$state(action)));
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }
    *///?} else {
    /*@Inject(method = "next", at = @At("RETURN"), remap = false)
    private static void mouseCallback(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;

        int button = Mouse.getEventButton();
        if (button >= 0) {
            EventManager.INSTANCE.post(new MouseInputEvent(button, Mouse.getEventButtonState() ? 1 : 0));
            return;
        }

        if (Mouse.getEventDWheel() != 0 || Minecraft.getInstance().screen == null || Mouse.getEventDX() == 0 && Mouse.getEventDY() == 0) return;

        DisplaySdl display = DisplaySdl.instance();
        float scaleX = (float) display.getWidth() / display.getWindowWidth();
        float scaleY = (float) display.getHeight() / display.getWindowHeight();
        float x = Mouse.getEventX() / scaleX;
        float y = (display.getHeight() - 1 - Mouse.getEventY()) / scaleY;
        MouseInputEvent.Moved.post(x, y);
    }
    *///? }

    @Unique
    private static int oneconfig$state(int action) {
        if (action == InputConstants.PRESS) return MouseInputEvent.PRESSED;
        if (action == InputConstants.RELEASE) return MouseInputEvent.RELEASED;
        return action;
    }
}
