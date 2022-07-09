package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.KeyInputEvent;
import net.minecraft.client.KeyboardListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardListener.class)
public class KeyboardMixin {
    @Inject(method = "onKeyEvent", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;fireKeyInput(IIII)V"))
    private void onKeyEvent(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        EventManager.INSTANCE.post(new KeyInputEvent());
    }
}
