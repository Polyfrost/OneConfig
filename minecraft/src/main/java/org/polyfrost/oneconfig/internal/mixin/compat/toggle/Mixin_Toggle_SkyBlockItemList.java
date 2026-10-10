package org.polyfrost.oneconfig.internal.mixin.compat.toggle;

import org.polyfrost.oneconfig.api.ui.v1.ModToggles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.operationpotato.itemlist.SkyBlockItemList", remap = false)
public class Mixin_Toggle_SkyBlockItemList {

    @Inject(method = {"addItemListWidget"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$skip1(CallbackInfo ci) {
        if (!ModToggles.isEnabled("skyblock-item-list")) ci.cancel();
    }

    @Inject(method = {"handleScreenRecipeLookup"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void oneconfig$force2(CallbackInfoReturnable<Boolean> cir) {
        if (!ModToggles.isEnabled("skyblock-item-list")) cir.setReturnValue(false);
    }
}
