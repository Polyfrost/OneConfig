package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.common.OmniProfiler;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.21.2
//$$ import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//$$ import net.minecraft.client.Minecraft;
//#endif

//#if MC >= 1.16.5
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.renderer.MultiBufferSource;
//#endif

@Mixin(RendererLivingEntity.class)
public class Mixin_RenderLivingEntityEvent<
        T extends EntityLivingBase
        //#if MC >= 1.21.2
        //$$ , S extends LivingEntityRenderState
        //#endif
> {

    @Inject(
            //#if MC >= 1.21.2
            //$$ method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            //#elseif MC >= 1.16.5
            //$$ method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //#else
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            //#endif
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPreEntityRenderCallback(
            //#if MC >= 1.21.2
            //$$ S entity,
            //#else
            T entity,
            //#endif
            //#if MC <= 1.12.2
            double x,
            double y,
            double z,
            //#endif
            //#if MC <= 1.21.1
            float entityYaw,
            float partialTicks,
            //#endif
            //#if MC >= 1.16.5
            //$$ PoseStack matrixStack,
            //$$ MultiBufferSource buffer,
            //$$ int packedLight,
            //#endif
            CallbackInfo ci
    ) {
        OmniProfiler.withProfiler("oneconfig_renderlivingentity_event_pre", () -> {
            //#if MC >= 1.21.2
            //$$ double x = entity.x;
            //$$ double y = entity.y;
            //$$ double z = entity.z;
            //$$ float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //#elseif MC >= 1.16.5
            //$$ double x = entity.getX();
            //$$ double y = entity.getY();
            //$$ double z = entity.getZ();
            //#endif
            RenderLivingEvent event = new RenderLivingEvent.Pre(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            if (event.cancelled) {
                ci.cancel();
            }
        });
    }

    @Inject(
            //#if MC >= 1.21.2
            //$$ method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            //#elseif MC >= 1.16.5
            //$$ method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //#else
            method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
            //#endif
            at = @At("TAIL")
    )
    private void onPostEntityRenderCallback(
            //#if MC >= 1.21.2
            //$$ S entity,
            //#else
            T entity,
            //#endif
            //#if MC <= 1.12.2
            double x,
            double y,
            double z,
            //#endif
            //#if MC <= 1.21.1
            float entityYaw,
            float partialTicks,
            //#endif
            //#if MC >= 1.16.5
            //$$ PoseStack matrixStack,
            //$$ MultiBufferSource buffer,
            //$$ int packedLight,
            //#endif
            CallbackInfo ci
    ) {
        OmniProfiler.withProfiler("oneconfig_renderlivingentity_event_post", () -> {
            //#if MC >= 1.21.2
            //$$ double x = entity.x;
            //$$ double y = entity.y;
            //$$ double z = entity.z;
            //$$ float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //#elseif MC >= 1.16.5
            //$$ double x = entity.getX();
            //$$ double y = entity.getY();
            //$$ double z = entity.getZ();
            //#endif
            RenderLivingEvent event = new RenderLivingEvent.Post(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            // Can't cancel when the method has already returned lol
        });
    }

}
