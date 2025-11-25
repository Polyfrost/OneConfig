package org.polyfrost.oneconfig.internal.mixin.events;

import com.llamalad7.mixinextras.sugar.Local;
import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class Mixin_RenderEvent {
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
}
