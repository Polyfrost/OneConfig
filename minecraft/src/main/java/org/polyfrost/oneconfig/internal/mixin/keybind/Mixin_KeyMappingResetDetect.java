package org.polyfrost.oneconfig.internal.mixin.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class Mixin_KeyMappingResetDetect {
    @Inject(method = "setKey", at = @At("HEAD"), cancellable = true)
    private void oneconfig$restoreFullDefault(InputConstants.Key key, CallbackInfo ci) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || bridge.isInternalSetKey()) return;
        KeyMapping self = (KeyMapping) (Object) this;
        if (!key.equals(self.getDefaultKey())) return;
        if (bridge.menuReset(self)) ci.cancel();
    }
}
