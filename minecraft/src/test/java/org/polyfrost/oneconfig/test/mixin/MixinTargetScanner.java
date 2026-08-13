/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2026 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.test.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.service.IClassProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class MixinTargetScanner {

    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    private MixinTargetScanner() {
    }

    record Result(
            List<String> configNames,
            List<String> mixinClasses,
            List<String> missingMixinClasses,
            List<String> untargetedMixins,
            List<String> targets
    ) {
    }

    static Result scan(String modId, ClassLoader loader, IClassProvider classProvider) throws IOException {
        JsonObject metadata = findModMetadata(modId, loader);
        List<String> configNames = new ArrayList<>();
        for (JsonElement entry : array(metadata, "mixins")) {
            if (entry.isJsonObject()) {
                configNames.add(entry.getAsJsonObject().get("config").getAsString());
            } else {
                configNames.add(entry.getAsString());
            }
        }
        if (configNames.isEmpty()) {
            throw new IOException("Mod '" + modId + "' declares no mixin configs");
        }

        Set<String> mixinClasses = new LinkedHashSet<>();
        for (String configName : configNames) {
            collectMixinClasses(configName, loader, classProvider, mixinClasses);
        }

        List<String> missing = new ArrayList<>();
        List<String> untargeted = new ArrayList<>();
        Set<String> targets = new LinkedHashSet<>();
        for (String mixinClass : mixinClasses) {
            ClassNode node = readClass(loader, mixinClass);
            if (node == null) {
                missing.add(mixinClass);
                continue;
            }
            List<String> mixinTargets = targetsOf(node);
            if (mixinTargets.isEmpty()) {
                untargeted.add(mixinClass);
            } else {
                targets.addAll(mixinTargets);
            }
        }

        return new Result(configNames, List.copyOf(mixinClasses), missing, untargeted, List.copyOf(targets));
    }

    private static void collectMixinClasses(
            String configName,
            ClassLoader loader,
            IClassProvider classProvider,
            Set<String> out
    ) throws IOException {
        JsonObject config = readJson(loader, configName);
        if (config == null) {
            throw new IOException("Mixin config '" + configName + "' is declared but not on the classpath");
        }

        String mixinPackage = config.has("package") ? config.get("package").getAsString() : null;
        if (mixinPackage == null) {
            throw new IOException("Mixin config '" + configName + "' declares no package");
        }

        List<String> names = new ArrayList<>();
        for (String section : List.of("mixins", "client", "server")) {
            for (JsonElement entry : array(config, section)) {
                names.add(entry.getAsString());
            }
        }
        if (config.has("plugin")) {
            names.addAll(pluginMixins(config.get("plugin").getAsString(), classProvider));
        }

        for (String name : names) {
            out.add(mixinPackage + "." + name);
        }
    }

    private static List<String> pluginMixins(String pluginClassName, IClassProvider classProvider) {
        try {
            Class<?> pluginClass = classProvider.findClass(pluginClassName, true);
            IMixinConfigPlugin plugin = (IMixinConfigPlugin) pluginClass.getDeclaredConstructor().newInstance();
            plugin.onLoad("");
            List<String> mixins = plugin.getMixins();
            return mixins == null ? List.of() : mixins;
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("Could not query mixin config plugin " + pluginClassName, e);
        }
    }

    private static List<String> targetsOf(ClassNode node) {
        List<String> targets = new ArrayList<>();
        List<AnnotationNode> annotations = new ArrayList<>();
        // @Mixin has CLASS retention so it lands in invisibleAnnotations
        if (node.invisibleAnnotations != null) {
            annotations.addAll(node.invisibleAnnotations);
        }
        if (node.visibleAnnotations != null) {
            annotations.addAll(node.visibleAnnotations);
        }
        for (AnnotationNode annotation : annotations) {
            if (!MIXIN_ANNOTATION.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            List<Object> values = annotation.values;
            for (int i = 0; i + 1 < values.size(); i += 2) {
                Object value = values.get(i + 1);
                if (!(value instanceof List<?> entries)) {
                    continue;
                }
                switch ((String) values.get(i)) {
                    case "value" -> entries.forEach(entry -> targets.add(((Type) entry).getClassName()));
                    case "targets" -> entries.forEach(entry -> targets.add(((String) entry).replace('/', '.')));
                    default -> { /* priority / remap are not interesting here */ }
                }
            }
        }
        return targets;
    }

    private static JsonObject findModMetadata(String modId, ClassLoader loader) throws IOException {
        Enumeration<URL> candidates = loader.getResources("fabric.mod.json");
        while (candidates.hasMoreElements()) {
            URL url = candidates.nextElement();
            try (Reader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                if (metadata.has("id") && modId.equals(metadata.get("id").getAsString())) {
                    return metadata;
                }
            } catch (RuntimeException e) {
                // another mod's metadata that we cannot parse is not our problem
            }
        }
        throw new IOException("No fabric.mod.json for mod '" + modId + "' on the classpath");
    }

    private static JsonObject readJson(ClassLoader loader, String resource) throws IOException {
        try (InputStream in = loader.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    private static ClassNode readClass(ClassLoader loader, String binaryName) throws IOException {
        try (InputStream in = loader.getResourceAsStream(binaryName.replace('.', '/') + ".class")) {
            if (in == null) {
                return null;
            }
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }
}
