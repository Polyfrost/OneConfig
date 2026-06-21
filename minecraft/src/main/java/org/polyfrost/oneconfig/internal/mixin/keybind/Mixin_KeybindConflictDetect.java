package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class Mixin_KeybindConflictDetect {
    @Inject(method = "same", at = @At("HEAD"), cancellable = true)
    private void oneconfig$comboAwareConflict(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return;
        KeyMapping self = (KeyMapping) (Object) this;
        if (bridge.bindFor(self) == null && bridge.bindFor(other) == null) return;
        cir.setReturnValue(bridge.menuConflict(self, other));
    }
}
