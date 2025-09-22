package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.render.stack.OmniMatrixStack;
import dev.deftu.omnicore.api.client.render.stack.OmniMatrixStacks;
import net.minecraft.client.renderer.EntityRenderer;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PostWorldRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class Mixin_PostWorldRenderEvent {

    @Inject(
            //#if MC >= 1.16.5
            //$$ method = "renderLevel",
            //#else
            method = "renderWorldPass",
            //#endif
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V"
                    //#if FABRIC
                    //#if MC <= 1.12.2
                    //$$ , ordinal = 1
                    //#endif
                    //#else
                    //#if MC >= 1.16.5
                    //$$ , ordinal = 1
                    //#else
                    , ordinal = 2
                    //#endif
                    //#endif
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            //#if MC >= 1.16.5
                            //$$ target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V"
                            //#else
                            target = "Lnet/minecraft/client/renderer/GlStateManager;disableFog()V"
                            //#endif
                    ),
                    to = @At(
                            value = "INVOKE",
                            //#if MC >= 1.17.1
                            //$$ target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V"
                            //#else
                            target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand(FI)V"
                            //#endif
                    )
            )
    )
    private void onRenderWorldPass(
            //#if MC <= 1.12.2
            int pass,
            //#endif
            float partialTicks,
            long finishTimeNano,
            //#if MC >= 1.16.5
            //$$ com.mojang.blaze3d.vertex.PoseStack matrixStack,
            //#endif
            CallbackInfo ci
    ) {
        OmniMatrixStack stack = OmniMatrixStacks.vanilla(
            //#if MC >= 1.16.5
            //$$ matrixStack
            //#endif
        );

        EventManager.INSTANCE.post(new PostWorldRenderEvent(stack, partialTicks));
    }

}
