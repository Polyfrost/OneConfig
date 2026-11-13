package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.server.packs.resources.ReloadInstance;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if > 1.8.9 {
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@Mixin(ReloadableResourceManager.class)
public class Mixin_SimpleReloadInstance {

    @Inject(method = "createReload", at = @At("RETURN"))
    private void oneconfig$onResourceFinishedLoading(CallbackInfoReturnable<ReloadInstance> cir) {
        cir.getReturnValue().done().whenComplete((result, throwable) -> {
            if (throwable != null) return;
            EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE);
        });
    }

}
//?} else {
/*import net.minecraft.server.packs.resources.SimpleReloadInstance;

@Mixin(SimpleReloadableResourceManager.class)
public class Mixin_SimpleReloadInstance {

    @Inject(
            method = "startReload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/ornithemc/osl/resource/loader/api/resource/reload/ResourceReload;",
            at = @At("RETURN")
    )
    private void oneconfig$onResourceFinishedLoading(CallbackInfoReturnable<ResourceReload> cir) {
        if ((Object) this != SimpleReloadableResourceManager.client()) return;
        cir.getReturnValue().result().whenComplete((result, throwable) -> {
            if (throwable != null) return;
            EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE);
        });
    }

}
*///?}
