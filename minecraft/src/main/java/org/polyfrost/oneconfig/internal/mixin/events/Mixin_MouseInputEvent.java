package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_MouseInputEvent {
    //#if FORGE
    @Inject(
            //#if MC >= 1.12.2
            //$$ method = "runTickMouse",
            //#else
            method = "runTick",
            //#endif
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z",
                    remap = false
            )
    )
    //#else
    //#if MC==10809
    //$$ @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false))
    //#else
    //$$ @Inject(method = "tickMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;setKeyPressed(IZ)V"))
    //#endif
    //#endif
    private void mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(org.lwjgl.input.Mouse.getEventButton(), org.lwjgl.input.Mouse.getEventButtonState() ? 1 : 0));
    }
}

