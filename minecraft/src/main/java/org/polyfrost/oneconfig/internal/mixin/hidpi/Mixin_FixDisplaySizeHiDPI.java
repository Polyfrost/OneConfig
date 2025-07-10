package org.polyfrost.oneconfig.internal.mixin.hidpi;

//#if MC < 1.13
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class Mixin_FixDisplaySizeHiDPI {

    //@formatter:off
    @Shadow public int displayWidth;
    @Shadow public int displayHeight;
    @Shadow private int tempDisplayWidth;
    @Shadow private int tempDisplayHeight;
    //@formatter:on

    @Shadow public abstract void resize(int par1, int par2);

    @Unique
    private float oneconfig$lastScaleFactor = -1F;
    @Unique
    private static final Logger oneconfig$HIDPI_LOGGER = LogManager.getLogger("OneConfig/HiDPI");

    @WrapOperation(method = "checkWindowResize", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;wasResized()Z", remap = false), remap = true)
    private boolean hiDpiFixWasResized(Operation<Boolean> original) {
        // If the display scale factor is not 1, we assume that the window has been resized
        // to account for HiDPI scaling.
        boolean wasResized = original.call();
        if (oneconfig$lastScaleFactor != -1F && oneconfig$lastScaleFactor != Display.getPixelScaleFactor()) {
            oneconfig$HIDPI_LOGGER.info("Detected HiDPI scaling change: {} -> {}", oneconfig$lastScaleFactor, Display.getPixelScaleFactor());
            this.displayWidth = Display.getWidth();
            this.displayHeight = Display.getHeight();
            if (this.displayWidth <= 0) {
                this.displayWidth = 1;
            }

            if (this.displayHeight <= 0) {
                this.displayHeight = 1;
            }
            resize(this.displayWidth, this.displayHeight);
        }
        return wasResized;
    }

    @ModifyVariable(method = "resize", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private int hiDpiFixResizeW(int value) {
        return (int) (value * (oneconfig$lastScaleFactor = Display.getPixelScaleFactor()));
    }

    @ModifyVariable(method = "resize", at = @At(value = "HEAD"), ordinal = 1, argsOnly = true)
    private int hiDpiFixResizeH(int value) {
        return (int) (value * (oneconfig$lastScaleFactor = Display.getPixelScaleFactor()));
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createDisplay()V", shift = At.Shift.AFTER))
    private void hiDpiFixDisplaySizes(CallbackInfo ci) {
        float scale = (oneconfig$lastScaleFactor = Display.getPixelScaleFactor());
        oneconfig$HIDPI_LOGGER.info("Got window scale factor of {}", scale);
        this.displayWidth = (int) (this.displayWidth * scale);
        this.displayHeight = (int) (this.displayHeight * scale);
        this.tempDisplayWidth = (int) (this.tempDisplayWidth * scale);
        this.tempDisplayHeight = (int) (this.tempDisplayHeight * scale);
    }

}
//#endif
