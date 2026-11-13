package org.polyfrost.oneconfig.internal.mixin.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsRenderer;
import org.polyfrost.oneconfig.api.ui.v1.platform.ComposeHudInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 1.21.10 {
import net.minecraft.client.input.MouseButtonInfo;
//?}

//? if > 1.8.9 {
import net.minecraft.client.MouseHandler;
import org.polyfrost.oneconfig.internal.ui.compose.RawScroll;
//?}

//? if = 1.8.9 {
/*import org.lwjgl.input.Mouse;
import org.polyfrost.oneconfig.internal.legacy.KeyCodes;
import org.polyfrost.oneconfig.internal.legacy.LegacyInputEvents;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

//? if > 1.8.9 {
@Mixin(MouseHandler.class)
//?} else
//@Mixin(value = Mouse.class, remap = false)
public class Mixin_MouseInputEvent {
    //? if >= 1.21.10 {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void mouseCallback(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (oneconfig$toastConsumes(buttonInfo.button(), action) || oneconfig$hudConsumes(buttonInfo.button(), action)) {
            ci.cancel();
            return;
        }
        EventManager.INSTANCE.post(new MouseInputEvent(buttonInfo.button(), oneconfig$state(action)));
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    //~ if sdl 'double y,' -> 'double y, double xrel, double yrel,'
    private void mouseMoveCallback(long handle, double x, double y, double xrel, double yrel, CallbackInfo ci) {
        //? if >= 26.2 {
        if (Minecraft.getInstance().gui.screen() != null) {
        //?} else {
        /*if (Minecraft.getInstance().screen != null) {
        *///?}
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }

    //?} elif > 1.8.9 {
    /*@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void mouseCallback(long handle, int button, int action, int mods, CallbackInfo ci) {
        if (oneconfig$toastConsumes(button, action) || oneconfig$hudConsumes(button, action)) {
            ci.cancel();
            return;
        }
        EventManager.INSTANCE.post(new MouseInputEvent(button, oneconfig$state(action)));
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }
    *///?} else {
    /*@Inject(method = "next", at = @At("RETURN"), remap = false, cancellable = true)
    private static void mouseCallback(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !LegacyInputEvents.deferred && (oneconfig$hudConsumesLegacy() || LegacyInputEvents.postMouse())) cir.setReturnValue(Mouse.next());
    }

    @Unique
    private static boolean oneconfig$hudConsumesLegacy() {
        int button = Mouse.getEventButton();
        if (button >= 0) {
            boolean pressed = Mouse.getEventButtonState();
            if (pressed && NotificationsRenderer.isPointerOverToast()) return false;
            return ComposeHudInput.consumeButton(KeyCodes.mouseFromLegacy(button), pressed);
        }
        double wheel = Mouse.getEventDWheelPrecise();
        if (wheel == 0.0 || NotificationsRenderer.isPointerOverToast()) return false;
        return ComposeHudInput.consumeScroll(0.0, wheel);
    }
    *///? }

    //? if > 1.8.9 {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void oneconfig$captureRawScroll(long window, double x, double y, CallbackInfo ci) {
        if (NotificationsRenderer.isPointerOverToast() || ComposeHudInput.consumeScroll(x, y)) {
            ci.cancel();
            return;
        }
        RawScroll.x = x;
        RawScroll.y = y;
    }
    //?}

    @Unique
    private static boolean oneconfig$toastConsumes(int button, int action) {
        if (action != InputConstants.PRESS && action != InputConstants.RELEASE) return false;
        return NotificationsRenderer.consumeMouseButton(button, action == InputConstants.PRESS);
    }

    @Unique
    private static boolean oneconfig$hudConsumes(int button, int action) {
        if (action != InputConstants.PRESS && action != InputConstants.RELEASE) return false;
        return ComposeHudInput.consumeButton(button, action == InputConstants.PRESS);
    }

    @Unique
    private static int oneconfig$state(int action) {
        if (action == InputConstants.PRESS) return MouseInputEvent.PRESSED;
        if (action == InputConstants.RELEASE) return MouseInputEvent.RELEASED;
        return action;
    }
}
