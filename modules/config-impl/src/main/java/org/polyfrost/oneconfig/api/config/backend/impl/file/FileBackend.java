/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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
import org.polyfrost.oneconfig.api.config.backend.impl.NightConfigSerializer;
import org.polyfrost.oneconfig.api.config.exceptions.SerializationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class FileBackend extends Backend {
    public final Path folder;
    public final FileSerializer<String> serializer;
    public final boolean isPhysicalFolder;
    private volatile boolean dodge = false;

    public FileBackend(Path folder, FileSerializer<String> serializer) {
        this.serializer = serializer;
        this.folder = folder;

        boolean is = true;
        WatchService ser;
        try {
            //noinspection ResultOfMethodCallIgnored
            folder.toFile().mkdirs();
            ser = folder.getFileSystem().newWatchService();
            folder.register(ser, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (Exception e) {
            is = false;
            ser = null;
            LOGGER.warn("Failed to setup file backend watcher onto {}", folder, e);
        }
        this.isPhysicalFolder = is;
        if (ser == null) return;

        final WatchService service = ser;
        Thread t = new Thread(() -> {
            int i = 0;
            while (true) {
                try {
                    WatchKey key = service.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (dodge) {
                            Backend.LOGGER.debug("Dodged self-created event!");
                            dodge = false;
                            continue;
                        }
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        Path p = folder.resolve((Path) event.context());
                        String id = removeExt(p.toString());
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
                        LOGGER.error("Failed to reset key, file watcher is off!");
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
        t.setName("Config Watcher");
        t.setDaemon(true);
        t.start();
    }

    public FileBackend(String folder) {
        this(Paths.get(folder), NightConfigSerializer.YAML);
    }

    private String removeExt(String s) {
        int st = s.lastIndexOf(folder.getFileSystem().getSeparator()) + 1;
        return s.substring(st, s.lastIndexOf('.'));
    }

    public void dodge() {
        dodge = true;
    }

    @Override
    protected Tree load0(@NotNull String id) {
        Path p = folder.resolve(id + serializer.getExtension());
        if (isPhysicalFolder && !p.toFile().exists()) return null;
        return serializer.deserialize(read(p));
    }

    @Override
    protected boolean save0(@NotNull Tree tree) {
        Path p = folder.resolve(tree.getID() + serializer.getExtension());
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

    @Override
    public boolean exists(String id) {
        return super.exists(id) || (isPhysicalFolder && folder.resolve(id + serializer.getExtension()).toFile().exists());
    }
}
