package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if > 1.8.9 {
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
*///?}

//? if > 1.8.9 {
@Mixin(KeyMapping.class)
public class Mixin_KeybindComboDisplay {
    @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
    private void oneconfig$showCombo(CallbackInfoReturnable<Component> cir) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return;
        KeyMapping self = (KeyMapping) (Object) this;
        OneConfigKeybind bind = bridge.bindFor(self);
        if (bind == null) return;
        // while recording show the live combo as keys are added
        Component preview = bridge.previewFor(self);
        if (preview != null) {
            cir.setReturnValue(preview);
            return;
        }
        Component decorated = MinecraftKeybindBridgeImpl.comboLabel(bind, cir.getReturnValue());
        if (decorated != null) cir.setReturnValue(decorated);
    }
}
//?} else {
/*@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class Mixin_KeybindComboDisplay {
    @Shadow
    private KeyMapping keyBinding;

    @ModifyExpressionValue(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getKeyName(I)Ljava/lang/String;"
            )
    )
    private String oneconfig$showCombo(String original) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return original;
        KeyMapping self = this.keyBinding;
        OneConfigKeybind bind = bridge.bindFor(self);
        if (bind == null) return original;
        // while recording show the live combo as keys are added
        Component preview = bridge.previewFor(self);
        if (preview != null) return preview.getFormattedString();
        Component decorated = MinecraftKeybindBridgeImpl.comboLabel(bind, Component.literal(original));
        return decorated != null ? decorated.getFormattedString() : original;
    }
}
*///?}
