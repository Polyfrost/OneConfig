package org.polyfrost.oneconfig.internal.mixin.hidpi;

//#if MC < 1.13
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class Mixin_EnableHiDPI {

    @Inject(method = "startGame", at = @At("HEAD"))
    private void hiDpiFixInit(CallbackInfo ci) {
        System.setProperty("org.lwjgl.opengl.Display.enableHighDPI", "true");
    }

}
//#endif
