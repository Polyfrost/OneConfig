package org.polyfrost.oneconfig.internal.mixin.fixes;

//? if >= 26.3 {
import com.mojang.blaze3d.platform.VideoMode;
import org.polyfrost.oneconfig.internal.utils.LegacyVideoModeFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VideoMode.class)
public class Mixin_LegacyVideoModeFormat {
    @ModifyVariable(method = "read", at = @At("HEAD"), argsOnly = true)
    private static String oneconfig$readLegacyFormat(String mode) {
        return LegacyVideoModeFormat.toJson(mode);
    }
}
//?}
