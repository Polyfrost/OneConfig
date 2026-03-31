package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class Mixin_HudRenderEvent {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderHudCallback(GuiGraphics ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
        //#if MC < 1.21.8
        ctx.flush();
        //#endif
        OmniRenderingContext context = OmniRenderingContext.from(ctx);
        float partialTicks = OmniRenderTicks.get();
        EventManager.INSTANCE.post(new HudRenderEvent(context, partialTicks));

        SkiaCtx.INSTANCE.blitHud(ctx);
    }

}
