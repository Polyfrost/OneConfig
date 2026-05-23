package org.polyfrost.oneconfig.internal.mixin.events;

import com.llamalad7.mixinextras.sugar.Local;
import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_RenderEvent {

    //? >= 1.21.8 {
    @Unique private GuiGraphics oneconfig$lastGraphics;

    @Inject(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 3,
                    shift = At.Shift.BEFORE
            )
    )
    private void renderTickStartCallback(
            DeltaTracker arg,
            boolean bl,
            CallbackInfo ci,
            @Local GuiGraphics guigraphics
    ) {
        this.oneconfig$lastGraphics = guigraphics;

        RenderEvent e = RenderEvent.Pre.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.from(guigraphics);
        EventManager.INSTANCE.post(e);
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void renderTickEndCallback(
            DeltaTracker arg,
            boolean bl,
            CallbackInfo ci
    ) {
        GuiGraphics guigraphics = this.oneconfig$lastGraphics;
        if (guigraphics == null) {
            return;
        }

        RenderEvent e = RenderEvent.Post.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.from(guigraphics);
        EventManager.INSTANCE.post(e);

        this.oneconfig$lastGraphics = null;
    }
    //? } else {
    /*
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void renderTickStartCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Pre.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.create();
        EventManager.INSTANCE.post(e);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            //? >= 1.21.4 {
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V"
            //? } else
            //target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V"
    ))
    private void renderTickEndCallback(CallbackInfo ci) {
        RenderEvent e = RenderEvent.Post.INSTANCE;
        e.deltaTicks = OmniRenderTicks.get();
        e.ctx = OmniRenderingContext.create();
        EventManager.INSTANCE.post(e);
    }
    *///? }

}
