package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleReloadInstance.class)
public class Mixin_SimpleReloadInstance {

    @Inject(method = "create", at = @At("RETURN"))
    private static void oneconfig$onResourceFinishedLoading(CallbackInfoReturnable<ReloadInstance> cir) {
        cir.getReturnValue().done().whenComplete((result, throwable) -> {
            EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE);
        });
    }

}
