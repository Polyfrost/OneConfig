package org.polyfrost.oneconfig.internal.mixin.fixes;

//? if = 1.8.9 {
/*import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Mixin(Options.class)
public abstract class Mixin_RememberUnknownOptions {
    @Shadow
    private File file;

    @Shadow
    public KeyMapping[] keyMappings;

    @Unique
    private final Map<String, String> ocfg$loadedLines = new LinkedHashMap<>();

    @Inject(method = "load", at = @At("HEAD"))
    private void ocfg$rememberLines(CallbackInfo ci) {
        ocfg$loadedLines.clear();
        if (file == null || !file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String key = ocfg$keyOf(line);
                if (key != null) ocfg$loadedLines.put(key, line);
            }
        } catch (IOException ignored) {
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void ocfg$writeRememberedLines(CallbackInfo ci) {
        if (ocfg$loadedLines.isEmpty() || file == null || !file.exists()) return;
        Set<String> written = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String key = ocfg$keyOf(line);
                if (key != null) written.add(key);
            }
        } catch (IOException e) {
            return;
        }
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge != null && keyMappings != null) {
            for (KeyMapping mapping : keyMappings) {
                if (bridge.bindFor(mapping) != null) written.add(ocfg$keyOf("key_" + mapping.getName() + ":"));
            }
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            for (Map.Entry<String, String> entry : ocfg$loadedLines.entrySet()) {
                if (!written.contains(entry.getKey())) writer.println(entry.getValue());
            }
        } catch (IOException ignored) {
        }
    }

    @Unique
    private static String ocfg$keyOf(String line) {
        int colon = line.indexOf(':');
        return colon > 0 ? line.substring(0, colon) : null;
    }
}
*///?}
