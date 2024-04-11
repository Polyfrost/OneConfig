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

package org.polyfrost.oneconfig.api.config.backend.impl.file;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.backend.Backend;
import org.polyfrost.oneconfig.api.config.exceptions.SerializationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

public class FileBackend extends Backend {
    public final Path folder;
    private final Map<String, FileSerializer<String>> serializers = new HashMap<>(8);
    private boolean hasWatcher = false;
    private volatile boolean dodge = false;

    @SafeVarargs
    public FileBackend(Path folder, FileSerializer<String>... serializers) {
        this.folder = folder;
        for (FileSerializer<String> s : serializers) {
            this.serializers.put(s.getExtension(), s);
        }
        try {
            Files.createDirectories(folder);
        } catch (Exception e) {
            LOGGER.error("Failed to create config folder!", e);
        }
    }

    public FileBackend addWatcher() throws IOException {
        if (hasWatcher) return this;
        WatchService service = folder.getFileSystem().newWatchService();
        folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        hasWatcher = true;

        Thread t = new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    WatchKey key = service.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        if (dodge) {
                            Backend.LOGGER.debug("Dodged self-created event!");
                            dodge = false;
                            continue;
                        }
                        Path p = folder.resolve((Path) event.context());
                        String id = p.getFileName().toString();
                        if (!exists(id)) continue;

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            LOGGER.info("config {} deleted? saving", id);
                            save(id);
                            dodge();
                        }
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            LOGGER.info("config {} modified, update requested", id);
                            requestUpdate(id);
                        }
                    }
                    if (!key.reset()) {
                        LOGGER.error("file watcher is invalid; disabled!");
                        break;
                    }
                } catch (Exception e) {
                    i++;
                    LOGGER.error("error with config file watcher (error no. {})", i, e);
                    if (i > 10) {
                        LOGGER.error("Too many errors, shutting down file watcher!");
                        break;
                    }
                }
            }
        });
        t.setName("OneConfig Config Watcher");
        t.setDaemon(true);
        t.start();
        return this;
    }

    public void dodge() {
        dodge = true;
    }

    @Override
    protected Tree load0(@NotNull String id) throws Exception {
        Path p = folder.resolve(id);
        if (!Files.exists(p)) return null;
        FileSerializer<String> serializer = getSerializer(p);
        try {
            return serializer.deserialize(read(p));
        } catch (Exception e) {
            LOGGER.error("Failed to load config ID {}, marking file as corrupted, config will be reset!", id);
            Files.move(p, folder.resolve(id + ".corrupted"));
            return null;
        }
    }

    @Override
    protected boolean save0(@NotNull Tree tree) {
        Path p = folder.resolve(tree.getID());
        FileSerializer<String> serializer = getSerializer(p);
        write(p, serializer.serialize(tree));
        return true;
    }

    private static String read(Path p) {
        StringBuilder buf = new StringBuilder();
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String o;
            while ((o = r.readLine()) != null) {
                buf.append(o).append('\n');
            }
        } catch (Exception e) {
            throw new SerializationException("Failed to read file", e);
        }
        return buf.toString();
    }

    private static void write(Path p, String s) {
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardOpenOption.CREATE)) {
            w.write(s);
        } catch (Exception e) {
            throw new SerializationException("Failed to write file", e);
        }
    }

    public void addSerializer(FileSerializer<String> serializer) {
        serializers.putIfAbsent(serializer.getExtension(), serializer);
    }

    private FileSerializer<String> getSerializer(Path p) {
        String path = p.toString();
        int i = path.lastIndexOf('.');
        if (i == -1) throw new IllegalArgumentException();
        String ext = path.substring(i);
        FileSerializer<String> s = serializers.get(ext);
        if (s == null) throw new IllegalArgumentException("No serializer found for extension " + ext);
        return s;
    }
}
