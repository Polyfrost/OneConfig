package org.polyfrost.oneconfig.internal.mixin.skia;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_InitSkia {
    @Inject(method = "<init>", at = @At("TAIL"))
    void impl$__init__(CallbackInfo ci) {
        SkiaCtx.INSTANCE.init();
    }
}
