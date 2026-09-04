package org.polyfrost.oneconfig.internal.mixin.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
//? if < 1.21.9
//import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
//? if < 1.21.2
//import net.minecraft.world.entity.LivingEntity;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? >= 1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
//~ if >= 26.1 'state.' -> 'state.level.'
import net.minecraft.client.renderer.state.level.CameraRenderState;
//? }
//? if >= 1.21.2
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Mixin(LivingEntityRenderer.class)
public class Mixin_RenderLivingEntityEvent {
    @Inject(
            //? >= 1.21.9 {
            //~ if >= 26.1 'state/Camera' -> 'state/level/Camera'
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            //? } >= 1.21.2 {
            /*method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            *///? } else
            //method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    //? if >= 1.21.9 {
    private void onPreEntityRenderCallback(LivingEntityRenderState entity, PoseStack matrixStack, SubmitNodeCollector renderQueue, CameraRenderState cameraState, CallbackInfo ci) {
    //?} elif >= 1.21.2 {
    /*private void onPreEntityRenderCallback(LivingEntityRenderState entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
    *///?} else {
    /*private void onPreEntityRenderCallback(LivingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
    *///?}
        // runs for every living entity on every frame, so build nothing when nothing is listening
        if (!EventManager.INSTANCE.hasListeners(RenderLivingEvent.Pre.class)) return;

        //? >= 1.21.2 {
        double x = entity.x;
        double y = entity.y;
        double z = entity.z;
        float partialTicks = Minecraft.getInstance().getFrameTimeNs();
        //? } else {
        /*double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        *///? }
        RenderLivingEvent event = new RenderLivingEvent.Pre(entity, partialTicks, x, y, z);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }

    @Inject(
            //? >= 1.21.9 {
            //~ if >= 26.1 'state/Camera' -> 'state/level/Camera'
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            //? } >= 1.21.2 {
             /*method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            *///?} else
            //method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL")
    )
    //? if >= 1.21.9 {
    private void onPostEntityRenderCallback(LivingEntityRenderState entity, PoseStack matrixStack, SubmitNodeCollector renderQueue, CameraRenderState cameraState, CallbackInfo ci) {
    //?} elif >= 1.21.2 {
    /*private void onPostEntityRenderCallback(LivingEntityRenderState entity, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
    *///?} else {
    /*private void onPostEntityRenderCallback(LivingEntity entity, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
    *///?}
        if (!EventManager.INSTANCE.hasListeners(RenderLivingEvent.Post.class)) return;
        //? >= 1.21.2 {
        double x = entity.x;
        double y = entity.y;
        double z = entity.z;
        float partialTicks = Minecraft.getInstance().getFrameTimeNs();
        //? } else {
        /*double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        *///? }
        RenderLivingEvent event = new RenderLivingEvent.Post(entity, partialTicks, x, y, z);
        EventManager.INSTANCE.post(event);
        // cannot cancel when the method has already returned
    }
}
