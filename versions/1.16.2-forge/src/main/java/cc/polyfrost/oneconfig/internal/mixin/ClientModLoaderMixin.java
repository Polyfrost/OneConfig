//#if FORGE==1
package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import cc.polyfrost.oneconfig.internal.OneConfig;
import cc.polyfrost.oneconfig.internal.init.OneConfigInit;
import net.minecraftforge.fml.client.ClientModLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientModLoader.class, remap = false)
public class ClientModLoaderMixin {
    @Inject(method = "begin", at = @At("HEAD"))
    private static void onBegin(CallbackInfo ci) {
        OneConfigInit.initialize(new String[]{});
    }

    @Inject(method = "lambda$finishModLoading$9", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameSettings;loadOptions()V", remap = true), remap = false)
    private static void onFinishModLoading(CallbackInfo ci) {
        EventManager.INSTANCE.post(new InitializationEvent());
        OneConfig.init();
    }
}
//#endif