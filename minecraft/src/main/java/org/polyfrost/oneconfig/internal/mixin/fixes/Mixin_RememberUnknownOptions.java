package org.polyfrost.oneconfig.internal.mixin.fixes;

//? if = 1.8.9 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.polyfrost.oneconfig.internal.legacy.LegacyOptionsFormat;
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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(Options.class)
public abstract class Mixin_RememberUnknownOptions {
    @Unique
    private static final String ocfg$KEY_PREFIX = "key_";

    @Unique
    private static final String ocfg$SKIPPED_PREFIX = "Skipping bad option:";

    @Unique
    private static final String ocfg$LEGACY_PACKS_PREFIX = "oneconfig_legacy_";

    @Shadow
    private File file;

    @Shadow
    public KeyMapping[] keyMappings;

    @Shadow
    public abstract void save();

    @Unique
    private final List<String> ocfg$loadedLines = new ArrayList<>();

    @Unique
    private final Set<String> ocfg$rejectedKeys = new HashSet<>();

    @Unique
    private final Set<String> ocfg$legacyPackKeys = new HashSet<>();

    @Unique
    private int ocfg$dataVersion;

    @Inject(method = "load", at = @At("HEAD"))
    private void ocfg$rememberLines(CallbackInfo ci) {
        ocfg$loadedLines.clear();
        ocfg$rejectedKeys.clear();
        ocfg$legacyPackKeys.clear();
        ocfg$dataVersion = 0;
        if (file == null || !file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ocfg$loadedLines.add(line);
                if (line.startsWith(ocfg$LEGACY_PACKS_PREFIX)) {
                    ocfg$legacyPackKeys.add(ocfg$keyOf(line.substring(ocfg$LEGACY_PACKS_PREFIX.length())));
                }
                if (line.startsWith("version:")) {
                    try {
                        ocfg$dataVersion = Integer.parseInt(line.substring("version:".length()).trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void ocfg$rewriteStaleFormats(CallbackInfo ci) {
        for (String line : ocfg$loadedLines) {
            if (!LegacyOptionsFormat.toModernLine(line, ocfg$dataVersion).equals(line)) {
                save();
                return;
            }
        }
    }

    @ModifyExpressionValue(
        method = "load",
        at = @At(value = "INVOKE", target = "Ljava/io/BufferedReader;readLine()Ljava/lang/String;", remap = false)
    )
    private String ocfg$readModernFormats(String line) {
        if (line == null) return null;
        if (line.startsWith(ocfg$LEGACY_PACKS_PREFIX)) return line.substring(ocfg$LEGACY_PACKS_PREFIX.length());
        if (ocfg$legacyPackKeys.contains(ocfg$keyOf(line))) return "";
        return LegacyOptionsFormat.toLegacyLine(line);
    }

    @WrapWithCondition(
        method = "load",
        at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V", remap = false)
    )
    private boolean ocfg$noteRejectedOption(Logger logger, String message) {
        if (message.startsWith(ocfg$SKIPPED_PREFIX)) {
            String key = ocfg$keyOf(message.substring(ocfg$SKIPPED_PREFIX.length()).trim());
            if (key != null) ocfg$rejectedKeys.add(key);
        }
        return false;
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void ocfg$writeRememberedLines(CallbackInfo ci) {
        if (ocfg$loadedLines.isEmpty() || file == null || !file.exists()) return;

        Map<String, String> saved = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String key = ocfg$keyOf(line);
                if ("resourcePacks".equals(key) || "incompatibleResourcePacks".equals(key)) {
                    key = ocfg$LEGACY_PACKS_PREFIX + key;
                    line = ocfg$LEGACY_PACKS_PREFIX + line;
                }
                if (key != null) saved.put(key, LegacyOptionsFormat.toModernLine(line, ocfg$dataVersion));
            }
        } catch (IOException e) {
            return;
        }
        if (saved.isEmpty()) return;

        Map<String, KeyMapping> mappings = ocfg$mappingsByKey();
        Set<String> mirrors = ocfg$mirrorKeys(mappings);
        List<String> merged = new ArrayList<>(ocfg$loadedLines.size() + saved.size());
        Set<String> emitted = new HashSet<>();

        for (String line : ocfg$loadedLines) {
            String key = ocfg$keyOf(line);
            if (key == null) {
                merged.add(line);
                continue;
            }
            if (!emitted.add(key)) continue;

            String fresh = saved.remove(key);
            if (fresh == null) {
                if (!mirrors.contains(key)) merged.add(LegacyOptionsFormat.toModernLine(line, ocfg$dataVersion));
            } else if (ocfg$rejectedKeys.contains(key) && !ocfg$changedThisSession(key, mappings)) {
                merged.add(line);
            } else {
                merged.add(fresh);
            }
        }

        merged.addAll(saved.values());

        File temp = new File(file.getParentFile(), file.getName() + ".ocfg-tmp");
        try (PrintWriter writer = new PrintWriter(new FileWriter(temp))) {
            for (String line : merged) {
                writer.println(line);
            }
        } catch (IOException e) {
            temp.delete();
            return;
        }
        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temp.delete();
        }
    }

    @Unique
    private boolean ocfg$changedThisSession(String key, Map<String, KeyMapping> mappings) {
        KeyMapping mapping = mappings.get(key);
        return mapping != null && mapping.getKeyCode() != mapping.getDefaultKeyCode();
    }

    @Unique
    private Map<String, KeyMapping> ocfg$mappingsByKey() {
        Map<String, KeyMapping> byKey = new HashMap<>();
        if (keyMappings == null) return byKey;
        for (KeyMapping mapping : keyMappings) {
            byKey.put(ocfg$KEY_PREFIX + mapping.getName(), mapping);
        }
        return byKey;
    }

    @Unique
    private Set<String> ocfg$mirrorKeys(Map<String, KeyMapping> mappings) {
        Set<String> keys = new HashSet<>();
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null) return keys;
        for (Map.Entry<String, KeyMapping> entry : mappings.entrySet()) {
            if (bridge.bindFor(entry.getValue()) != null) keys.add(entry.getKey());
        }
        return keys;
    }

    @Unique
    private static String ocfg$keyOf(String line) {
        int colon = line.indexOf(':');
        return colon > 0 ? line.substring(0, colon) : null;
    }
}
*///?}
