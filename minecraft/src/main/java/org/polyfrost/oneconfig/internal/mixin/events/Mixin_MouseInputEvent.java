package org.polyfrost.oneconfig.internal.mixin.events;

//#if MC >= 1.16.5
//$$ import net.minecraft.client.MouseHandler;
//$$ import dev.deftu.omnicore.api.client.screen.OmniScreens;
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//#else
import net.minecraft.client.Minecraft;
//#endif

import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.16.5
//$$ @Mixin(MouseHandler.class)
//#else
@Mixin(Minecraft.class)
//#endif
public class Mixin_MouseInputEvent {

    //#if MC >= 1.16.5
    //#if FORGE
    //$$ @Inject(
    //$$         method = "onPress",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //#if MC >= 1.20.4
    //$$                 target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onMouseButtonPre(III)Z",
    //#else
    //$$                 target = "Lnet/minecraftforge/client/ForgeHooksClient;onRawMouseClicked(III)Z",
    //#endif
    //$$                 remap = false
    //$$         ),
    //$$         remap = true
    //$$ )
    //$$ private void mouseCallback(long handle, int button, int action, int mods, CallbackInfo ci) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(button, action));
    //$$ }
    //#else
    //$$ @ModifyVariable(method = "onMouseButton", at = @At("STORE"), ordinal = 0)
    //$$ private int mouseCallback(int button, long handle, int b, int action, int mods) {
    //$$     EventManager.INSTANCE.post(new MouseInputEvent(button, action));
    //$$     return button;
    //$$ }
    //#endif
    //#else

    //@formatter:off
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
    //@formatter:on
    private void mouseCallback(CallbackInfo ci) {
        EventManager.INSTANCE.post(new MouseInputEvent(org.lwjgl.input.Mouse.getEventButton(), org.lwjgl.input.Mouse.getEventButtonState() ? 1 : 0));
    }

    //#endif

    //#if MC >= 1.16.5
    //$$ @Inject(method = "onMove", at = @At("HEAD"))
    //$$ private void mouseMoveCallback(long handle, double x, double y, CallbackInfo ci) {
    //$$     if(OmniScreens.isInScreen()) MouseInputEvent.Moved.post((float) x, (float) y);
    //$$ }
    //#endif

}

