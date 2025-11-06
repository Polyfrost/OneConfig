package org.polyfrost.oneconfig.internal.mixin.events;

//#if MC >= 1.21.1
//$$ import net.minecraft.client.DeltaTracker;
//#endif

//#if MC >= 1.20.1
//$$ import net.minecraft.client.gui.GuiGraphics;
//#elseif MC >= 1.16.5
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC >= 1.16.5
//$$ import net.minecraft.client.gui.Gui;
//#else
//#if FORGE
import net.minecraftforge.client.GuiIngameForge;
//#else
//$$ import net.minecraft.client.gui.GuiIngame;
//#endif
//#endif

import dev.deftu.omnicore.api.client.render.OmniRenderTicks;
import dev.deftu.omnicore.api.client.render.OmniRenderingContext;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HudRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.16.5
//$$ @Mixin(Gui.class)
//#else
//#if FORGE
@Mixin(GuiIngameForge.class)
//#else
//$$ @Mixin(GuiIngame.class)
//#endif
//#endif
public class Mixin_HudRenderEvent {

    @Inject(
            //#if MC >= 1.16.5
            //$$ method = "render",
            //#else
            method = "renderGameOverlay",
            //#endif
            at = @At("TAIL")
    )
    private void renderHudCallback(
            //#if MC >= 1.20.1
            //$$ GuiGraphics ctx,
            //#elseif MC >= 1.16.5
            //$$ PoseStack ctx,
            //#endif
            //#if MC >= 1.21.1
            //$$ DeltaTracker deltaTracker,
            //#else
            float passedPartialTicks,
            //#endif
            CallbackInfo ci
    ) {
        OmniRenderingContext context = OmniRenderingContext.from(
                //#if MC >= 1.16.5
                //$$ ctx
                //#endif
        );

        float partialTicks = OmniRenderTicks.get();
        EventManager.INSTANCE.post(new HudRenderEvent(context, partialTicks));
    }

}
