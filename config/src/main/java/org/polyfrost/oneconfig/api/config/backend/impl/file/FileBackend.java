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
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.backend.Backend;
import org.polyfrost.oneconfig.api.config.backend.Serializer;
import org.polyfrost.oneconfig.api.config.exceptions.SerializationException;
import org.polyfrost.oneconfig.api.config.util.Triple;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBackend implements Backend {
    private Path folder;
    private WatchService service;

    private volatile boolean dodge = false;
    private final List<FileSerializer> serializers = new ArrayList<>();

    private final Map<String, Triple<File, Serializer, Tree>> configs = new HashMap<>();

    @SuppressWarnings({"CallToPrintStackTrace", "ResultOfMethodCallIgnored"})
    public FileBackend(Path folder) {
        setDirectory(folder);
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    WatchKey key = service.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (dodge) {
                            LOGGER.debug("Dodged self-created event!");
                            dodge = false;
                            continue;
                        }
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        File f = folder.resolve((Path) event.context()).toFile();

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            f.createNewFile();
                            get(idByFile(f));
                        }
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            get(idByFile(f));
                        }
                    }
                    if (!key.reset()) {
                        // AAAA
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.setName("Config Watcher");
        t.setDaemon(true);
        t.start();
    }

    public FileBackend(String folder) {
        this(Paths.get(folder));
    }

    public FileBackend addSerializer(FileSerializer serializer) {
        serializers.add(serializer);
        return this;
    }

    public FileBackend addSerializers(FileSerializer... serializers) {
        for (FileSerializer serializer : serializers) {
            addSerializer(serializer);
        }
        return this;
    }

    public FileBackend addSerializers(Collection<FileSerializer> serializers) {
        serializers.forEach(this::addSerializer);
        return this;
    }


    @Override
    public void put(@NotNull Tree tree) {
        dodge = true;
        Triple<File, Serializer, Tree> t = null;
        if (!configs.containsKey(tree.id)) {
            File file = folder.resolve(tree.id).toFile();
            for (FileSerializer serializer : serializers) {
                if (serializer.supports(file)) {
                    t = new Triple<>(file, serializer, tree);
                    configs.put(tree.id, t);
                    break;
                }
            }
        } else t = configs.get(tree.id);
        if (t == null) throw new SerializationException("No serializer found for " + tree.id);
        assert t.third == tree;
        File file = t.first;
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new SerializationException("Failed to create file", e);
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(t.second.serialize(t.third));
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize!", e);
        }
    }

    @Override
    public @Nullable Tree get(@NotNull String id) {
        Triple<File, Serializer, Tree> t = configs.get(id);
        if (t == null) return null;
        StringBuilder str = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(t.first))) {
            String l;
            while ((l = r.readLine()) != null) {
                str.append(l).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (t.third != null) t.third.overwriteWith(t.second.deserialize(id, str.toString()), true);
        else t.third = t.second.deserialize(id, str.toString());
        return t.third;
    }


    String idByFile(File f) {
        for (Map.Entry<String, Triple<File, Serializer, Tree>> e : configs.entrySet()) {
            if (e.getValue().first == f) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Change the directory where the configs are stored. This will update the watcher.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void setDirectory(@NotNull Path folder) {
        this.folder = folder;
        try {
            folder.toFile().mkdirs();
            if (service != null) service.close();
            service = FileSystems.getDefault().newWatchService();
            folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists(@NotNull String id) {
        return configs.containsKey(id);
    }

    @Override
    public boolean remove(@NotNull String id) {
        Triple<File, Serializer, Tree> t = configs.remove(id);
        if (t == null) return false;
        dodge = true;
        return t.first.delete();
    }

    @Override
    public void refresh() {
        for(Map.Entry<String, Triple<File, Serializer, Tree>> e : configs.entrySet()) {
            get(e.getKey());
        }
    }
}
