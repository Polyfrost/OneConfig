package org.polyfrost.oneconfig.internal.mixin.skia;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaCtx;
import org.polyfrost.oneconfig.internal.ui.compose.SkiaFontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_InitSkiaFontRenderer {
    @Inject(method = "<init>", at = @At("TAIL"))
    void impl$__init__(CallbackInfo ci) {
        SkiaFontRenderer.INSTANCE.init();
    }
}
