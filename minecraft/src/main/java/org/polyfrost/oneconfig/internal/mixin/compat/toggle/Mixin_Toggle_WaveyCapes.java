package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

//? if >= 1.21.10 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} elif >= 1.21.2 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?} elif >= 1.21.1 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
*///?}
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// the layer's own method is not in any mappings, so before 26.1 it is spelt both ways it can turn up
@Pseudo
@Mixin(targets = "dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer", remap = false)
public class Mixin_Toggle_WaveyCapes {

    //? if >= 1.21.10 {
    @Inject(
        method = {
            "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            "submit(Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;ILnet/minecraft/class_10055;FF)V"
        },
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void oneconfig$vanillaCape(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (ModToggles.isEnabled("waveycapes")) return;
        Object vanilla = ModGates.waveyCapesVanillaLayer();
        if (vanilla instanceof CapeLayer) ((CapeLayer) vanilla).submit(pose, collector, light, state, yRot, xRot);
        ci.cancel();
    }
    //?} elif >= 1.21.2 {
    /*@Inject(
        method = {
            "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            "render(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;ILnet/minecraft/class_10055;FF)V"
        },
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void oneconfig$vanillaCape(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (ModToggles.isEnabled("waveycapes")) return;
        Object vanilla = ModGates.waveyCapesVanillaLayer();
        if (vanilla instanceof CapeLayer) ((CapeLayer) vanilla).render(pose, buffers, light, state, yRot, xRot);
        ci.cancel();
    }
    *///?} elif >= 1.21.1 {
    /*@Inject(
        method = {
            "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/player/AbstractClientPlayer;FFFFFF)V",
            "render(Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;ILnet/minecraft/class_742;FFFFFF)V"
        },
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void oneconfig$vanillaCape(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (ModToggles.isEnabled("waveycapes")) return;
        Object vanilla = ModGates.waveyCapesVanillaLayer();
        if (vanilla instanceof CapeLayer) ((CapeLayer) vanilla).render(pose, buffers, light, player, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        ci.cancel();
    }
    *///?}
}
