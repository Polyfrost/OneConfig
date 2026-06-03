package org.polyfrost.oneconfig.internal.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading;
import org.polyfrost.oneconfig.internal.OneConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;

@Mixin(SimpleReloadInstance.class)
public class Mixin_SimpleReloadInstance<S> {



    @WrapMethod(method = "prepareTasks")
    @Coerce
    protected CompletableFuture<List<S>> test(
        Executor taskExecutor,
        Executor mainThreadExecutor,
        ResourceManager resourceManager,
        List<PreparableReloadListener> listeners,
        @Coerce Object stateFactory,
        CompletableFuture<?> initialTask,
        Operation<CompletableFuture<List<S>>> original) {

        return original.call(taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask).whenComplete((s, throwable) -> {
            EventManager.INSTANCE.post(ResourceFinishedLoading.INSTANCE);
        });
    }

}
