package org.polyfrost.oneconfig.internal.mixin;

//? if >= 1.21.4
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.MainMenuFpsSampler;
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen;
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
    private static final int MAIN_MENU_FPS_HEADROOM = 60;
    private static final int FALLBACK_MONITOR_REFRESH_RATE = 60;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void oneconfig$uncapWhileSampling(CallbackInfoReturnable<Integer> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        //? if >= 26.2 {
        Object screen = minecraft.gui.screen();
        //?} else
        //Object screen = minecraft.screen;
        if (minecraft.level == null && screen instanceof ComposeScreen) {
            int fallback = FALLBACK_MONITOR_REFRESH_RATE + MAIN_MENU_FPS_HEADROOM;
            int refreshRate = minecraft.getWindow().getRefreshRate();
            if (refreshRate > 0) {
                cir.setReturnValue(refreshRate + MAIN_MENU_FPS_HEADROOM);
            } else {
                cir.setReturnValue(fallback);
            }
            return;
        }

        if (MainMenuFpsSampler.isSampling()) {
            cir.setReturnValue(260);
        }
    }
}
