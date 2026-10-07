package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

//? if >= 26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer", remap = false)
public class Mixin_Toggle_WaveyCapes {

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
        at = @At("HEAD"), cancellable = true, require = 0
    )
    private void oneconfig$vanillaCape(PoseStack pose, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (ModToggles.isEnabled("waveycapes")) return;
        Object vanilla = ModGates.waveyCapesVanillaLayer();
        if (vanilla instanceof CapeLayer) ((CapeLayer) vanilla).submit(pose, collector, light, state, yRot, xRot);
        ci.cancel();
    }
}
//?}
