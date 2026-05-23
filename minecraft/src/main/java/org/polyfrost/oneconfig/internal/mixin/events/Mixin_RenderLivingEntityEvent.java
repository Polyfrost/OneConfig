package org.polyfrost.oneconfig.internal.mixin.events;

import dev.deftu.omnicore.api.client.OmniClient;
import dev.deftu.omnicore.api.client.OmniClientProfiler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
//? } else {
//import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//import net.minecraft.client.Minecraft;
//? }

@Mixin(LivingEntityRenderer.class)
public class Mixin_RenderLivingEntityEvent<
        T extends LivingEntity
        //? >= 1.21.2
        , S extends LivingEntityRenderState
> {
    @Inject(
            //? >= 1.21.9 {
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            //? } >= 1.21.2 {
            //method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //? } else
            //method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onPreEntityRenderCallback(
            //? >= 1.21.2 {
            S entity,
            //? } else
            //T entity,
            //? <= 1.21.1 {
            //float entityYaw,
            //float partialTicks,
            //? }
            //? >= 1.21.9 {
            //PoseStack matrixStack,
            //SubmitNodeCollector renderQueue,
            //CameraRenderState cameraState,
            //? } else {
            PoseStack matrixStack,
            MultiBufferSource buffer,
            int packedLight,
            //? }
            CallbackInfo ci
    ) {
        OmniClientProfiler.withProfiler(OmniClient.get(), "oneconfig_renderlivingentity_event_pre", () -> {
            //? >= 1.21.2 {
            double x = entity.x;
            double y = entity.y;
            double z = entity.z;
            float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //? } else {
            //double x = entity.getX();
            //double y = entity.getY();
            //double z = entity.getZ();
            //? }
            RenderLivingEvent event = new RenderLivingEvent.Pre(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            if (event.cancelled) {
                ci.cancel();
            }
        });
    }

    @Inject(
            //? >= 1.21.9 {
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            //? } >= 1.21.2 {
            // method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //?} else
            //method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    private void onPostEntityRenderCallback(
            //? >= 1.21.2 {
            S entity,
            //? } else
            //T entity,
            //? <= 1.21.1 {
            //float entityYaw,
            //float partialTicks,
            //? }
            //? >= 1.21.9 {
            PoseStack matrixStack,
            SubmitNodeCollector renderQueue,
            CameraRenderState cameraState,
            //? } else {
            /*PoseStack matrixStack,
            MultiBufferSource buffer,
            int packedLight,
            *///? }
            CallbackInfo ci
    ) {
        OmniClientProfiler.withProfiler(OmniClient.get(), "oneconfig_renderlivingentity_event_post", () -> {
            //? >= 1.21.2 {
            double x = entity.x;
            double y = entity.y;
            double z = entity.z;
            float partialTicks = Minecraft.getInstance().getFrameTimeNs();
            //? } else {
            //double x = entity.getX();
            //double y = entity.getY();
            //double z = entity.getZ();
            //? }
            RenderLivingEvent event = new RenderLivingEvent.Post(entity, partialTicks, x, y, z);
            EventManager.INSTANCE.post(event);
            // Can't cancel when the method has already returned lol
        });
    }
}
