package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import net.minecraftforge.fml.client.ClientModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientModLoader.class, remap = false)
public class ClientModLoaderMixin { //todo
    @Inject(method = "begin", at = @At("HEAD"))
    private static void onBegin(CallbackInfo ci) {
        OneConfig.preLaunch();
    }

    @Inject(method = "finishModLoading", at = @At("HEAD"))
    private static void onFinishModLoading(CallbackInfo ci) {
        EventManager.INSTANCE.post(new InitializationEvent());
        OneConfig.init();
    }
}
