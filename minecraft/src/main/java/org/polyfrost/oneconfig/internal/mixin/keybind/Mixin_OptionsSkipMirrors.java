package org.polyfrost.oneconfig.internal.mixin.keybind;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps OneConfig's Controls menu mirror mappings out of options.txt.
 * <p>
 * Mirror values are persisted through the mod's own config, and mirror names are plain display
 * labels (e.g. {@code key_Slot 1}) that are not namespaced and could clash with real mappings.
 */
@Mixin(Options.class)
public class Mixin_OptionsSkipMirrors {
    @ModifyExpressionValue(method = "processOptions", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;"))
    private KeyMapping[] oneconfig$skipMirrors(KeyMapping[] mappings) {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || mappings == null) return mappings;
        List<KeyMapping> kept = new ArrayList<>(mappings.length);
        for (KeyMapping mapping : mappings) {
            if (bridge.bindFor(mapping) == null) kept.add(mapping);
        }
        if (kept.size() == mappings.length) return mappings;
        return kept.toArray(new KeyMapping[0]);
    }
}
