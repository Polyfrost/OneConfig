package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.polyfrost.oneconfig.internal.compat.toggle.ModGates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "tomeko.legacyskyblock.utils.HypixelPackets", remap = false)
public class Mixin_Toggle_LegacySkyblockLocation {

    @Inject(method = "onLocationPacket", at = @At("TAIL"), require = 0)
    private static void oneconfig$forgetLocation(CallbackInfo ci) {
        if (!ModToggles.isEnabled("legacyskyblock")) ModGates.forgetLegacySkyblockLocation();
    }
}
