package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_MouseInputEvent {
    @Inject(
            //#if MC >= 1.12.2
            //$$ method = "runTickMouse",
            //#else
            method = "runTick",
            //#endif
            at = @At(
                    value = "INVOKE",
                    //#if FABRIC
                    //#if MC >= 1.12.2
                    //$$ target = "Lnet/minecraft/client/settings/KeyBinding;setKeyBindState(IZ)V",
                    //#else
                    //$$ target = "Lorg/lwjgl/input/Mouse;getEventButton()I",
                    //#endif
                    //#else
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;postMouseEvent()Z",
                    //#endif
                    remap = false
            )
    )
    private void mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(Mouse.getEventButton(), Mouse.getEventButtonState() ? 1 : 0));
    }
}
