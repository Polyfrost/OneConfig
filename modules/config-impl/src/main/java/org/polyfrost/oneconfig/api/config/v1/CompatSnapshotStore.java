/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
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

package org.polyfrost.oneconfig.api.config.v1;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.json.JsonParser;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@org.jetbrains.annotations.ApiStatus.Internal
public final class CompatSnapshotStore {
    static final String FILE_NAME = "compat-snapshots.json";

    private final String fileName;
    private final ConfigWriter writer = JsonFormat.fancyInstance().createWriter();
    private final ConfigParser<?> parser = createBackedParser();

    public CompatSnapshotStore() {
        this(FILE_NAME);
    }

    public CompatSnapshotStore(String fileName) {
        this.fileName = fileName;
    }

    private final Map<String, Map<String, Map<String, Object>>> cache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OneConfig-CompatSnapshots");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, ScheduledFuture<?>> pendingFlush = new ConcurrentHashMap<>();

    public Map<String, Map<String, Object>> load(String profile) {
        return cache.computeIfAbsent(profile, this::readFromDisk);
    }

    public Object getValue(String profile, String treeId, String key) {
        Map<String, Object> tree = load(profile).get(treeId);
        return tree == null ? null : tree.get(key);
    }

    public void putValue(String profile, String treeId, String key, Object serialized) {
        load(profile).computeIfAbsent(treeId, k -> new ConcurrentHashMap<>()).put(key, serialized);
        scheduleFlush(profile);
    }

    private void scheduleFlush(String profile) {
        ScheduledFuture<?> prev = pendingFlush.put(profile, flusher.schedule(() -> {
            pendingFlush.remove(profile);
            flush(profile);
        }, 200, TimeUnit.MILLISECONDS));
        if (prev != null) prev.cancel(false);
    }

    public synchronized void flush(String profile) {
        Map<String, Map<String, Object>> snapshot = cache.get(profile);
        if (snapshot == null) return;
        Path file = ConfigManager.profileDir(profile).resolve(fileName);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, writer.writeToString(toConfig(snapshot)).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            ConfigManager.LOGGER.error("Failed to write compat snapshot for profile '{}'", profile, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> readFromDisk(String profile) {
        Map<String, Map<String, Object>> out = new ConcurrentHashMap<>();
        Path file = ConfigManager.profileDir(profile).resolve(fileName);
        if (!Files.isRegularFile(file)) return out;
        try {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (text.isEmpty()) return out;
            BackedConfig cfg = new BackedConfig(new HashMap<>());
            parser.parse(text, cfg, ParsingMode.MERGE);
            Map<String, Object> root = fromConfig(cfg);
            for (Map.Entry<String, Object> e : root.entrySet()) {
                if (e.getValue() instanceof Map) {
                    Map<String, Object> tree = new ConcurrentHashMap<>();
                    tree.putAll((Map<String, Object>) e.getValue());
                    out.put(e.getKey(), tree);
                }
            }
        } catch (Exception e) {
            ConfigManager.LOGGER.error("Failed to read compat snapshot for profile '{}'", profile, e);
        }
        return out;
    }

    private static Config toConfig(Map<String, ?> map) {
        Map<String, Object> backing = new HashMap<>(map.size(), 1f);
        for (Map.Entry<String, ?> e : map.entrySet()) {
            backing.put(e.getKey(), toStorable(e.getValue()));
        }
        return new BackedConfig(backing);
    }

    @SuppressWarnings("unchecked")
    private static Object toStorable(Object value) {
        if (value instanceof Map) return toConfig((Map<String, Object>) value);
        if (value instanceof List) {
            List<Object> in = (List<Object>) value;
            java.util.ArrayList<Object> out = new java.util.ArrayList<>(in.size());
            for (Object o : in) out.add(toStorable(o));
            return out;
        }
        return value;
    }

    private static Map<String, Object> fromConfig(Config cfg) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : cfg.valueMap().entrySet()) {
            out.put(e.getKey(), fromStorable(e.getValue()));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object fromStorable(Object value) {
        if (value instanceof Config) return fromConfig((Config) value);
        if (value instanceof List) {
            List<Object> in = (List<Object>) value;
            java.util.ArrayList<Object> out = new java.util.ArrayList<>(in.size());
            for (Object o : in) out.add(fromStorable(o));
            return out;
        }
        return value;
    }

    private static ConfigParser<?> createBackedParser() {
        try {
            Constructor<JsonParser> ctor = JsonParser.class.getDeclaredConstructor(ConfigFormat.class);
            ctor.setAccessible(true);
            return ctor.newInstance(BackedFormat.INSTANCE);
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("Could not build dot-safe compat-snapshot parser; falling back to default", t);
            return JsonFormat.fancyInstance().createParser();
        }
    }

    private static final class BackedConfig extends AbstractConfig {
        BackedConfig(Map<String, Object> map) {
            super(map);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T set(List<String> path, Object value) {
            String key = path.size() == 1 ? path.get(0) : String.join(".", path);
            return (T) valueMap().put(key, value);
        }

        @Override
        public BackedConfig clone() {
            return new BackedConfig(new HashMap<>(valueMap()));
        }

        @Override
        public Config createSubConfig() {
            return new BackedConfig(new HashMap<>());
        }

        @Override
        public ConfigFormat<?> configFormat() {
            return InMemoryFormat.withUniversalSupport();
        }
    }

    private static final class BackedFormat implements ConfigFormat<Config> {
        static final BackedFormat INSTANCE = new BackedFormat();

        @Override
        public ConfigWriter createWriter() {
            return JsonFormat.fancyInstance().createWriter();
        }

        @Override
        public ConfigParser<Config> createParser() {
            return JsonFormat.fancyInstance().createParser();
        }

        @Override
        public Config createConfig(java.util.function.Supplier<Map<String, Object>> mapCreator) {
            return new BackedConfig(mapCreator.get());
        }

        @Override
        public boolean supportsComments() {
            return false;
        }

        @Override
        public boolean supportsType(Class<?> type) {
            return true;
        }
    }
}
