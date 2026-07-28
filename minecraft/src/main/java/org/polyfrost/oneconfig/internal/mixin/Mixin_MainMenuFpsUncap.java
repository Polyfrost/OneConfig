package org.polyfrost.oneconfig.internal.mixin;

//? if >=1.21.4 {
import com.mojang.blaze3d.platform.FramerateLimitTracker;
//?} else {
/*import net.minecraft.client.Minecraft;
*///?}
import org.polyfrost.oneconfig.internal.MainMenuFpsSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21.4 {
@Mixin(value = FramerateLimitTracker.class, priority = Integer.MAX_VALUE)
//?} else {
/*@Mixin(value = Minecraft.class, priority = Integer.MAX_VALUE)
*///?}
public class Mixin_MainMenuFpsUncap {
    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void oneconfig$uncapWhileSampling(CallbackInfoReturnable<Integer> cir) {
        if (MainMenuFpsSampler.isSampling()) {
            cir.setReturnValue(260);
        }
    }
}
