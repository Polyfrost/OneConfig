package org.polyfrost.oneconfig.internal.mixin.compat.rconfig;

import com.llamalad7.mixinextras.sugar.Local;
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
import com.teamresourceful.resourcefulconfig.common.config.Configurations;
//#if MC<1.20.4
import com.teamresourceful.resourcefulconfig.common.config.ResourcefulConfig;
//#else
//$$import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig;
//#endif
import org.polyfrost.oneconfig.internal.compat.RConfigCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = Configurations.class, remap = false)
public class Mixin_Configurations {

    @Inject(at = @At("HEAD"), method = "<clinit>")
    private static void clinit(CallbackInfo ci) {
        RConfigCompat.enable();
    }

    @Inject(at = @At("HEAD"), method = "addConfig")
    private void addConfig(CallbackInfo ci, @Local(argsOnly = true) ResourcefulConfig config) {
        RConfigCompat.addConfig(config);
    }

}
