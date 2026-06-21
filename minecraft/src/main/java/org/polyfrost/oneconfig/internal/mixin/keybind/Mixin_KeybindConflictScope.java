package org.polyfrost.oneconfig.internal.mixin.keybind;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.KeyMapping;
import org.objectweb.asm.Opcodes;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
public class Mixin_KeybindConflictScope {
    @ModifyExpressionValue(
        method = "refreshEntry",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;",
            opcode = Opcodes.GETFIELD
        )
    )
    private KeyMapping[] oneconfig$includeOurMappings(KeyMapping[] original) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return original;
        KeyMapping[] ours = bridge.entries();
        if (ours.length == 0) return original;
        KeyMapping[] combined = Arrays.copyOf(original, original.length + ours.length);
        System.arraycopy(ours, 0, combined, original.length, ours.length);
        return combined;
    }
}
