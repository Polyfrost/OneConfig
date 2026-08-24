package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if > 1.8.9 {
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
*///?}

//? if > 1.8.9 {
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
//?} else {
/*@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class Mixin_KeybindConflictDetect {
    @Final
    @Shadow
    private KeyMapping keyBinding;

    @Definition(id = "getKeyCode", method = "Lnet/minecraft/client/KeyMapping;getKeyCode()I")
    @Definition(
            id = "keyBinding",
            field = "Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList$KeyEntry;keyBinding:Lnet/minecraft/client/KeyMapping;"
    )
    @Expression("?.getKeyCode() == this.keyBinding.getKeyCode()")
    @ModifyExpressionValue(method = "render", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean oneconfig$conflict(boolean original, @Local KeyMapping other) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || (bridge.bindFor(this.keyBinding) == null && bridge.bindFor(other) == null)) {
            return original;
        }
        return bridge.menuConflict(this.keyBinding, other);
    }
}
*///?}
