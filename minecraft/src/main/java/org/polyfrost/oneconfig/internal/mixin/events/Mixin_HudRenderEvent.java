package org.polyfrost.oneconfig.internal.mixin.events;

//#if FORGE && MC <= 1.12.2
import net.minecraftforge.client.GuiIngameForge;
//#else
//#if FORGE-LIKE
//$$ import net.minecraft.client.gui.Gui;
//#else
//$$ import net.minecraft.client.gui.hud.InGameHud;
//#endif
//$$
//#if MC >= 1.20
//#if FORGE-LIKE
//$$ import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import net.minecraft.client.gui.DrawContext;
//#endif
//#elseif MC >= 1.13
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
//#endif

//#if MC >= 1.21.1
//$$ import net.minecraft.client.render.RenderTickCounter;
//#endif

import dev.deftu.omnicore.client.render.OmniGameRendering;
import dev.deftu.omnicore.client.render.OmniMatrixStack;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if FORGE && MC <= 1.12.2
@Mixin(GuiIngameForge.class)
//#elseif FORGE-LIKE
//$$ @Mixin(Gui.class)
//#else
//$$ @Mixin(InGameHud.class)
//#endif
public class Mixin_HudRenderEvent {

    @Inject(
            //#if FORGE && MC <= 1.12.2
            method = "renderGameOverlay",
            //#else
            //$$ method = "render",
            //#endif
            at = @At("TAIL")
    )
    private void renderHudCallback(
            //#if MC >= 1.20
            //#if FORGE-LIKE
            //$$ GuiGraphics ctx,
            //#else
            //$$ DrawContext ctx,
            //#endif
            //#elseif MC >= 1.13
            //$$ PoseStack ctx,
            //#endif
            //#if MC >= 1.21.1
            //$$ RenderTickCounter renderTickCounter,
            //#else
            float passedPartialTicks,
            //#endif
            CallbackInfo ci
    ) {
        OmniMatrixStack stack = OmniMatrixStack.vanilla(
                //#if MC >= 1.16.5
                //$$ ctx
                //#endif
        );

        //#if MC >= 1.20.1
        //$$ Platform.screen().setSmuggledDrawContext(ctx);
        //#elseif MC >= 1.16.5
        //$$ Platform.screen().setSmuggledDrawContext(stack);
        //#else
        Platform.screen().setSmuggledDrawContext(stack);
        //#endif

        float partialTicks = OmniGameRendering.getTickDelta(false);
        EventManager.INSTANCE.post(new HudRenderEvent(stack, partialTicks));
    }

}
