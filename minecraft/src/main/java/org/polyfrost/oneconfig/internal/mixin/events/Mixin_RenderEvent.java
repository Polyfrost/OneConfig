package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import dev.deftu.omnicore.api.client.render.stack.OmniMatrixStacks;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_RenderEvent {

    //@formatter:off
    @Unique
    private static final String UPDATE_CAMERA_AND_RENDER =
            //#if MC >= 1.16
            //$$ "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V";
            //#elseif MC >= 1.13
            //$$ "Lnet/minecraft/client/renderer/GameRenderer;updateCameraAndRender(FJZ)V";
            //#else
            "Lnet/minecraft/client/renderer/EntityRenderer;updateCameraAndRender(FJ)V";
            //#endif
    //@formatter:on

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER))
    private void renderTickStartCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Pre.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = new OmniRenderingContext(
                //#if MC >= 1.20.1
                //$$ null,
                //#endif
                OmniMatrixStacks.create()
        );

        EventManager.INSTANCE.post(e);
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = UPDATE_CAMERA_AND_RENDER, shift = At.Shift.AFTER))
    private void renderTickEndCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Post.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = new OmniRenderingContext(
                //#if MC >= 1.20.1
                //$$ null,
                //#endif
                OmniMatrixStacks.create()
        );

        EventManager.INSTANCE.post(e);
    }

}
