package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import dev.deftu.omnicore.api.client.render.stack.OmniMatrixStacks;
import net.minecraft.client.MinecraftClient;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class Mixin_RenderEvent {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V"))
    private void renderTickStartCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Pre.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = new OmniRenderingContext(
                //#if MC >= 1.20.1
                null,
                //#endif
                OmniMatrixStacks.create()
        );

        EventManager.INSTANCE.post(e);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    //#if MC >= 1.21.4
                    //$$ target = "Lnet/minecraft/client/util/Window;swapBuffers(Lnet/minecraft/client/util/tracy/TracyFrameCapturer;)V"
                    //#else
                    target = "Lnet/minecraft/client/util/Window;swapBuffers()V"
                    //#endif
            )
    )
    private void renderTickEndCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Post.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = new OmniRenderingContext(
                null,
                OmniMatrixStacks.create()
        );

        EventManager.INSTANCE.post(e);
    }
}
