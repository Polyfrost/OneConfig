package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.wwaypoints.client.ServerFeaturePolicy", remap = false)
public class Mixin_Toggle_WWaypoints {

    @ModifyReturnValue(method = "allows", at = @At("RETURN"), require = 0)
    private static boolean oneconfig$disallowWhileOff(boolean original) {
        return original && ModToggles.isEnabled("wwaypoints");
    }
}
