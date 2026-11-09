package org.polyfrost.oneconfig.internal.mixin.fixes;

//? if >= 26.3 {
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import java.util.List;
import java.util.Optional;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monitor.class)
public class Mixin_MonitorNearestRefresh {
    @Shadow @Final private List<VideoMode> videoModes;

    @Inject(method = "getPreferredVideoMode", at = @At("HEAD"), cancellable = true)
    private void oneconfig$nearestRefresh(Optional<VideoMode> expectedMode, CallbackInfoReturnable<VideoMode> cir) {
        if (expectedMode.isEmpty()) return;
        int index = oneconfig$nearest(expectedMode.get());
        if (index >= 0) cir.setReturnValue(this.videoModes.get(index));
    }

    @Inject(method = "indexOfMode", at = @At("RETURN"), cancellable = true)
    private void oneconfig$nearestIndex(VideoMode expectedMode, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() < 0) cir.setReturnValue(oneconfig$nearest(expectedMode));
    }

    @Unique
    private int oneconfig$nearest(VideoMode want) {
        int exact = this.videoModes.indexOf(want);
        if (exact >= 0) return exact;
        int best = -1;
        float bestDiff = 1.0F;
        for (int i = 0; i < this.videoModes.size(); i++) {
            VideoMode mode = this.videoModes.get(i);
            float diff = Math.abs(mode.getRefreshRate() - want.getRefreshRate());
            if (diff < bestDiff && mode.getWidth() == want.getWidth() && mode.getHeight() == want.getHeight()
                && mode.getRedBits() == want.getRedBits() && mode.getGreenBits() == want.getGreenBits() && mode.getBlueBits() == want.getBlueBits()) {
                best = i;
                bestDiff = diff;
            }
        }
        return best;
    }
}
//?}
