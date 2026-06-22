package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//~ if = 1.8.9 'SimpleReloadInstance' -> 'SimpleReloadableResourceManager'
@Mixin(SimpleReloadInstance.class)
public class Mixin_SimpleReloadInstance {

    //~ if = 1.8.9 'create' -> 'startReload'
    @Inject(method = "create", at = @At("RETURN"))
    //~ if = 1.8.9 'ReloadInstance' -> 'ResourceReload'
    private static void oneconfig$onResourceFinishedLoading(CallbackInfoReturnable<ReloadInstance> cir) {
        //~ if = 1.8.9 'done()' -> 'result()'
        cir.getReturnValue().done().whenComplete((result, throwable) -> {
            EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE);
        });
    }

}
