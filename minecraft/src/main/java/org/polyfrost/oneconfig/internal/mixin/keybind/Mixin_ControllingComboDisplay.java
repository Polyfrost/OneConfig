package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if > 1.8.9 {
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.blamejared.controlling.client.NewKeyBindsList$KeyEntry")
public class Mixin_ControllingComboDisplay {
    @Shadow(remap = false) @Final private KeyMapping key;
    @Shadow(remap = false) @Final private Button btnChangeKeyBinding;

    @Inject(method = "extractContent", at = @At("HEAD"), remap = false)
    private void oneconfig$showCombo(CallbackInfo ci) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || key == null || btnChangeKeyBinding == null || bridge.bindFor(key) == null) return;

        if (bridge.getActiveRebind() == key) {
            Component preview = bridge.previewFor(key);
            Component inner = preview != null ? preview : bridge.comboTextFor(key);
            if (inner == null) return;
            Component prompt = Component.literal("> ")
                .append(inner.copy().withStyle(net.minecraft.ChatFormatting.YELLOW))
                .append(Component.literal(" <"));
            if (!prompt.equals(btnChangeKeyBinding.getMessage())) btnChangeKeyBinding.setMessage(prompt);
            return;
        }

        Component combo = bridge.comboTextFor(key);
        if (combo != null && !combo.equals(btnChangeKeyBinding.getMessage())) btnChangeKeyBinding.setMessage(combo);
    }
}
//?} else {
/*import net.minecraft.SharedConstants;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SharedConstants.class)
public class Mixin_ControllingComboDisplay {}
*///?}
