package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.screen.OmniScreens;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if FABRIC
import org.spongepowered.asm.mixin.injection.ModifyVariable;
//#endif

@Mixin(Mouse.class)
public class Mixin_MouseInputEvent {
    //#if FORGE-LIKE
    //$$ @Inject(
    //$$         method = "onPress",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/neoforged/neoforge/client/ClientHooks;onMouseButtonPre(III)Z",
    //$$                 remap = false
    //$$         ),
    //$$         remap = true
    //$$ )
    //$$ private void mouseCallback(long handle, int button, int action, int mods, CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(button, action));
    //$$ }
    //#else
    @ModifyVariable(method = "onMouseButton", at = @At("STORE"), ordinal = 0)
    private int mouseCallback(int button, long handle, MouseInput event, int action) {
        EventManager.INSTANCE.post(new MouseInputEvent(button, action));
        return button;
    }
    //#endif

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
        if (OmniScreens.isInScreen()) {
            MouseInputEvent.Moved.post((float) x, (float) y);
        }
    }
}
