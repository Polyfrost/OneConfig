package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.toggle.v1.ModToggle;
import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "tomeko.hychatter.waypoints.WaypointRenderer", remap = false)
public class Mixin_Toggle_HyChatterWaypoints {
    @Unique
    private static final ModToggle oneconfig$toggle = ModToggles.handle("hychatter");

    @Inject(method = {"onWorldRender"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$skip1(CallbackInfo ci) {
        if (!oneconfig$toggle.isEnabled()) ci.cancel();
    }
}
