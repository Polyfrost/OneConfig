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
import org.polyfrost.oneconfig.api.config.util.Pair;

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
import java.util.stream.Collectors;

public class FileBackend implements Backend {
    private Path folder;
    private WatchService service;

    private volatile boolean dodge = false;
    private final List<FileSerializer> serializers = new ArrayList<>();
    private final Map<String, File> fileCache = new HashMap<>();
    private final Map<String, Pair<Serializer, Tree>> configs = new HashMap<>();

    @SuppressWarnings({"CallToPrintStackTrace", "ResultOfMethodCallIgnored"})
    public FileBackend(Path folder, Serializer... serializers) {
        addSerializers(serializers);
        setDirectory(folder);
        Thread t = new Thread(() -> {
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
                        File f = folder.resolve((Path) event.context()).toFile();

                        if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            f.createNewFile();
                            get(f.getName());
                        }
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            get(f.getName());
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

    @Override
    public FileBackend addSerializers(Serializer... serializers) {
        for (Serializer s : serializers) {
            if (s instanceof FileSerializer) this.serializers.add((FileSerializer) s);
        }
        return this;
    }

    public Path getDirectory() {
        return folder;
    }


    @Override
    public void put(@NotNull Tree tree) {
        dodge = true;
        Pair<Serializer, Tree> t = null;
        File file = getFile(tree.getID());
        if (!configs.containsKey(tree.getID())) {
            for (FileSerializer serializer : serializers) {
                if (serializer.supports(file)) {
                    t = new Pair<>(serializer, tree);
                    configs.put(tree.getID(), t);
                    break;
                }
            }
        } else t = configs.get(tree.getID());
        if (t == null) throw new SerializationException("No serializer found for " + tree.getID());
        assert t.second == tree;
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new SerializationException("Failed to create file", e);
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(t.first.serialize(t.second));
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize!", e);
        }
    }

    private File getFile(String id) {
        return fileCache.computeIfAbsent(id, it -> folder.resolve(it).toFile());
    }

    @Override
    public @Nullable Tree get(@NotNull String id) {
        Pair<Serializer, Tree> t = configs.get(id);
        StringBuilder str = new StringBuilder();
        File f = getFile(id);
        if (!f.exists()) return null;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String l;
            while ((l = r.readLine()) != null) {
                str.append(l).append("\n");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (t == null) {
            for (FileSerializer serializer : serializers) {
                if (serializer.supports(f)) {
                    t = new Pair<>(serializer, null);
                    configs.put(id, t);
                    break;
                }
            }
            if (t == null) return null;
        }
        if (t.second != null) t.second.merge(t.first.deserialize(id, str.toString()), false, true);
        else t.second = t.first.deserialize(id, str.toString());
        return t.second;
    }

    /**
     * Change the directory where the configs are stored. This will update the watcher and refresh all tracked trees.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public void setDirectory(@NotNull Path folder) {
        this.folder = folder;
        try {
            folder.toFile().mkdirs();
            service = FileSystems.getDefault().newWatchService();
            folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileCache.clear();
        for (String id : configs.keySet()) {
            get(id);
        }
    }

    @Override
    public boolean exists(@NotNull String id) {
        return configs.containsKey(id);
    }

    @Override
    public boolean remove(@NotNull String id) {
        return remove(id, false);
    }

    public boolean remove(String id, boolean keepFile) {
        Pair<Serializer, Tree> t = configs.remove(id);
        if (t == null) return false;
        dodge = true;
        File f = getFile(id);
        if (keepFile) {
            return f.renameTo(new File(f.getAbsolutePath() + ".corrupted"));
        } else return f.delete();
    }

    @Override
    public void refresh() {
        for (String s : configs.keySet()) {
            File f = getFile(s);
            if (f.isDirectory()) continue;
            get(f.getName());
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public void registerAllFiles() {
        for (File f : folder.toFile().listFiles()) {
            if (f.isDirectory()) continue;
            get(f.getName());
        }
    }

    @Override
    public Collection<Tree> getTrees() {
        return configs.values().stream().map(t -> t.second).collect(Collectors.toList());
    }
}
