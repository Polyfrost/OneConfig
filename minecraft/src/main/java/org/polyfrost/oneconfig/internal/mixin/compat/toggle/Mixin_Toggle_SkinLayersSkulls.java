package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

//? if >= 1.21.1 {
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkullBlockEntity.class)
public class Mixin_Toggle_SkinLayersSkulls {

    @Inject(method = "getHeadLayers", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void oneconfig$noMesh(CallbackInfoReturnable<Object> cir) {
        if (!ModToggles.isEnabled("skinlayers3d")) cir.setReturnValue(null);
    }

    @Inject(method = "setInitialized", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void oneconfig$retryLater(boolean initialized, CallbackInfo ci) {
        if (initialized && !ModToggles.isEnabled("skinlayers3d")) ci.cancel();
    }
}
//?}
