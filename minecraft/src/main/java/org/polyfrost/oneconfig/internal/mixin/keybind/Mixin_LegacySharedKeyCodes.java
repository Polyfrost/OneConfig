package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if = 1.8.9 {
/*import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Int2ObjectHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class Mixin_LegacySharedKeyCodes {
    @Shadow @Final private static List<KeyMapping> ALL;
    @Shadow @Final private static Int2ObjectHashMap<KeyMapping> BY_KEY_CODE;
    @Shadow private int keyCode;
    @Shadow private boolean pressed;
    @Shadow private int clickCount;

    @Inject(method = "click", at = @At("TAIL"))
    private static void oneconfig$clickShared(int keyCode, CallbackInfo ci) {
        if (keyCode == 0) return;
        KeyMapping hashed = BY_KEY_CODE.get(keyCode);
        for (KeyMapping mapping : ALL) {
            Mixin_LegacySharedKeyCodes other = (Mixin_LegacySharedKeyCodes) (Object) mapping;
            if (mapping != hashed && other.keyCode == keyCode) other.clickCount++;
        }
    }

    @Inject(method = "set", at = @At("TAIL"))
    private static void oneconfig$setShared(int keyCode, boolean pressed, CallbackInfo ci) {
        if (keyCode == 0) return;
        KeyMapping hashed = BY_KEY_CODE.get(keyCode);
        for (KeyMapping mapping : ALL) {
            Mixin_LegacySharedKeyCodes other = (Mixin_LegacySharedKeyCodes) (Object) mapping;
            if (mapping != hashed && other.keyCode == keyCode) other.pressed = pressed;
        }
    }
}
*///?}
