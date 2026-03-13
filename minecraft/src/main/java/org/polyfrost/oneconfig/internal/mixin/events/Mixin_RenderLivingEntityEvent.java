package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import dev.deftu.omnicore.api.client.OmniClientProfiler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.21.9
//$$ import net.minecraft.client.renderer.SubmitNodeCollector;
//$$ import net.minecraft.client.renderer.state.CameraRenderState;
//#endif

//#if MC >= 1.21.2
//$$ import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//$$ import net.minecraft.client.Minecraft;
//#endif

@Mixin(LivingEntityRenderer.class)
public class Mixin_RenderLivingEntityEvent<
        T extends LivingEntity
        //#if MC >= 1.21.2
        //$$ , S extends LivingEntityRenderState
        //#endif
> {
    @Inject(
            //#if MC >= 1.21.9
            //$$ method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            //#elseif MC >= 1.21.2
            //$$ method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //#else
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
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
            //#if MC <= 1.21.1
            float entityYaw,
            float partialTicks,
            //#endif
            //#if MC >= 1.21.9
            //$$ PoseStack matrixStack,
            //$$ SubmitNodeCollector renderQueue,
            //$$ CameraRenderState cameraState,
            //#else
            PoseStack matrixStack,
            MultiBufferSource buffer,
            int packedLight,
            //#endif
            CallbackInfo ci
    ) {
        OmniClientProfiler.withProfiler(OmniClient.get(), "oneconfig_renderlivingentity_event_pre", () -> {
            //#if MC >= 1.21.2
            //$$ double x = entity.x;
            //$$ double y = entity.y;
            //$$ double z = entity.z;
            //$$ float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //#else
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            //#endif
            RenderLivingEvent event = new RenderLivingEvent.Pre(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            if (event.cancelled) {
                ci.cancel();
            }
        });
    }

    @Inject(
            //#if MC >= 1.21.9
            //$$ method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            //#elseif MC >= 1.21.2
            //$$ method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //#else
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //#endif
            at = @At("TAIL")
    )
    private void onPostEntityRenderCallback(
            //#if MC >= 1.21.2
            //$$ S entity,
            //#else
            T entity,
            //#endif
            //#if MC <= 1.21.1
            float entityYaw,
            float partialTicks,
            //#endif
            //#if MC >= 1.21.9
            //$$ PoseStack matrixStack,
            //$$ SubmitNodeCollector renderQueue,
            //$$ CameraRenderState cameraState,
            //#else
            PoseStack matrixStack,
            MultiBufferSource buffer,
            int packedLight,
            //#endif
            CallbackInfo ci
    ) {
        OmniClientProfiler.withProfiler(OmniClient.get(), "oneconfig_renderlivingentity_event_post", () -> {
            //#if MC >= 1.21.2
            //$$ double x = entity.x;
            //$$ double y = entity.y;
            //$$ double z = entity.z;
            //$$ float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //#else
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            //#endif
            RenderLivingEvent event = new RenderLivingEvent.Post(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            // Can't cancel when the method has already returned lol
        });
    }
}
