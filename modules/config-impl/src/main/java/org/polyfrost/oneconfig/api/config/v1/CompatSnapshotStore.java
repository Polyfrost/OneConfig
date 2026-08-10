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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    private final Map<String, IllegalStateException> loadFailures = new ConcurrentHashMap<>();

    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OneConfig-CompatSnapshots");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, ScheduledFuture<?>> pendingFlush = new ConcurrentHashMap<>();

    public synchronized Map<String, Map<String, Object>> load(String profile) {
        IllegalStateException previousFailure = loadFailures.get(profile);
        if (previousFailure != null) throw previousFailure;
        Map<String, Map<String, Object>> snapshot = cache.get(profile);
        if (snapshot != null) return snapshot;
        try {
            snapshot = readFromDisk(profile);
            cache.put(profile, snapshot);
            return snapshot;
        } catch (IllegalStateException failure) {
            // A malformed or temporarily unreadable snapshot must not become an empty, writable cache:
            // doing so would replace the only on-disk copy the next time a value is captured.
            loadFailures.put(profile, failure);
            throw failure;
        }
    }

    public synchronized Object getValue(String profile, String treeId, String key) {
        Map<String, Object> tree = load(profile).get(treeId);
        return tree == null ? null : tree.get(key);
    }

    public synchronized boolean hasLoadFailure(String profile) {
        if (loadFailures.containsKey(profile)) return true;
        if (cache.containsKey(profile)) return false;
        try {
            readFromDisk(profile);
            return false;
        } catch (IllegalStateException failure) {
            loadFailures.put(profile, failure);
            return true;
        }
    }

    public synchronized void putValue(String profile, String treeId, String key, Object serialized) {
        putValue(profile, treeId, key, serialized, true);
    }

    synchronized void putValueWithoutScheduling(String profile, String treeId, String key, Object serialized) {
        putValue(profile, treeId, key, serialized, false);
    }

    private void putValue(String profile, String treeId, String key, Object serialized, boolean schedule) {
        load(profile).computeIfAbsent(treeId, k -> new ConcurrentHashMap<>()).put(key, serialized);
        if (schedule) scheduleFlush(profile);
    }

    private synchronized void scheduleFlush(String profile) {
        AtomicReference<ScheduledFuture<?>> scheduled = new AtomicReference<>();
        ScheduledFuture<?> next = flusher.schedule(
                () -> flushScheduled(profile, scheduled.get()), 200, TimeUnit.MILLISECONDS
        );
        scheduled.set(next);
        ScheduledFuture<?> prev = pendingFlush.put(profile, next);
        if (prev != null) prev.cancel(false);
    }

    private synchronized void flushScheduled(String profile, ScheduledFuture<?> expected) {
        if (expected != null && pendingFlush.remove(profile, expected)) flush(profile);
    }

    public synchronized void flush(String profile) {
        try {
            flushInternal(profile, false);
        } catch (Exception e) {
            ConfigManager.LOGGER.error("Failed to write compat snapshot for profile '{}'", profile, e);
        }
    }

    /**
     * Flushes a snapshot for a profile lifecycle operation, propagating any failure to the caller.
     * Unlike {@link #flush(String)}, a missing named profile directory is an error rather than a
     * silently ignored delayed write.
     */
    public synchronized void flushOrThrow(String profile) throws IOException {
        flushInternal(profile, true);
    }

    private void flushInternal(String profile, boolean requireProfileDirectory) throws IOException {
        cancelPending(profile);
        IllegalStateException loadFailure = loadFailures.get(profile);
        if (loadFailure != null) throw loadFailure;
        Map<String, Map<String, Object>> snapshot = cache.get(profile);
        if (snapshot == null) return;
        Path profileDir = ConfigManager.profileDir(profile);
        // A delayed flush must not recreate a profile that was just renamed or deleted.
        if (!profile.isEmpty() && !Files.isDirectory(profileDir)) {
            if (requireProfileDirectory) throw new NoSuchFileException(profileDir.toString());
            return;
        }
        Path file = profileDir.resolve(fileName);
        // Named profile directories are lifecycle-managed by ConfigManager. Recreating one here
        // after a concurrent rename/delete would resurrect a profile from a delayed flush.
        if (profile.isEmpty()) Files.createDirectories(file.getParent());
        final byte[] bytes;
        try {
            bytes = writer.writeToString(toConfig(snapshot)).getBytes(StandardCharsets.UTF_8);
        } catch (Exception failure) {
            throw new IOException("Failed to serialize compat snapshot for profile '" + profile + "'", failure);
        }
        writeAtomically(file, bytes);
    }

    private static void writeAtomically(Path file, byte[] bytes) throws IOException {
        Path parent = file.getParent();
        if (parent == null) throw new NoSuchFileException(file.toString());
        Path temporary = Files.createTempFile(parent, "." + file.getFileName() + ".", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public synchronized void renameProfile(String oldProfile, String newProfile) throws IOException {
        cancelPending(oldProfile);
        cancelPending(newProfile);
        IllegalStateException loadFailure = loadFailures.remove(oldProfile);
        loadFailures.remove(newProfile);
        Map<String, Map<String, Object>> snapshot = cache.remove(oldProfile);
        Map<String, Map<String, Object>> updates = cache.remove(newProfile);
        if (loadFailure != null) {
            // The filesystem rename moved the unreadable source file. Keep the destination poisoned
            // rather than allowing an unrelated update under its new identity to overwrite it.
            loadFailures.put(newProfile, loadFailure);
            return;
        }
        if (snapshot == null) {
            if (updates != null) {
                cache.put(newProfile, updates);
                flushOrThrow(newProfile);
            }
            return;
        }
        if (updates != null) {
            for (Map.Entry<String, Map<String, Object>> entry : updates.entrySet()) {
                snapshot.computeIfAbsent(entry.getKey(), ignored -> new ConcurrentHashMap<>())
                        .putAll(entry.getValue());
            }
        }
        cache.put(newProfile, snapshot);
        flushOrThrow(newProfile);
    }

    public synchronized void deleteProfile(String profile) {
        cancelPending(profile);
        cache.remove(profile);
        loadFailures.remove(profile);
    }

    private void cancelPending(String profile) {
        ScheduledFuture<?> pending = pendingFlush.remove(profile);
        if (pending != null) pending.cancel(false);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> readFromDisk(String profile) {
        Map<String, Map<String, Object>> out = new ConcurrentHashMap<>();
        Path file = ConfigManager.profileDir(profile).resolve(fileName);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return out;
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Compat snapshot is not a regular file: " + file);
        }
        try {
            String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (text.isEmpty()) throw new IOException("Snapshot file is empty");
            BackedConfig cfg = new BackedConfig(new HashMap<>());
            parser.parse(text, cfg, ParsingMode.MERGE);
            Map<String, Object> root = fromConfig(cfg);
            for (Map.Entry<String, Object> e : root.entrySet()) {
                if (!(e.getValue() instanceof Map)) {
                    throw new IOException("Snapshot namespace '" + e.getKey() + "' is not an object");
                }
                Map<String, Object> tree = new ConcurrentHashMap<>();
                tree.putAll((Map<String, Object>) e.getValue());
                out.put(e.getKey(), tree);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to read compat snapshot for profile '" + profile + "'", failure);
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
