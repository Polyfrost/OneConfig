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

package org.polyfrost.oneconfig.api.config.v1.backend.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.polyfrost.oneconfig.api.config.v1.exceptions.SerializationException;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class FileBackend extends Backend {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    public final Path folder;
    private final Map<String, FileSerializer<String>> serializers = new HashMap<>(8);
    private boolean hasWatcher = false;
    private volatile boolean dodge = false;

    @SafeVarargs
    public FileBackend(Path folder, FileSerializer<String>... serializers) {
        this.folder = folder;
        for (FileSerializer<String> s : serializers) {
            addSerializer(s);
        }
        mkdirs(folder);
    }

    protected static Path mkdirs(Path p) {
        try {
            return Files.createDirectories(p);
        } catch (IOException e) {
            throw new SerializationException("Failed to create directory", e);
        }
    }

    protected static String read(Path p) {
        try {
            return new String(Files.readAllBytes(p), CHARSET);
        } catch (Exception e) {
            throw new SerializationException("Failed to read file", e);
        }
    }

    protected static void write(Path p, String s) {
        try {
            Files.createDirectories(p.getParent());
            Files.write(p, s.getBytes(CHARSET));
        } catch (Exception e) {
            throw new SerializationException("Failed to write file", e);
        }
    }

    public synchronized FileBackend addWatcher() throws IOException {
        if (hasWatcher) return this;
        WatchService service = folder.getFileSystem().newWatchService();
        folder.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
//        Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                dir.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
//                return FileVisitResult.CONTINUE;
//            }
//        });
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
                        Path p = (Path) event.context();
                        String id = p.toString();
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
                        hasWatcher = false;
                        break;
                    }
                } catch (Exception e) {
                    i++;
                    LOGGER.error("error with config file watcher (error no. {})", i, e);
                    if (i > 10) {
                        hasWatcher = false;
                        LOGGER.error("Too many errors, shutting down file watcher!");
                        break;
                    }
                }
            }
            try {
                service.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close file watcher service", e);
            }
        });
        t.setName("OneConfig Config Watcher");
        t.setDaemon(true);
        t.start();
        LOGGER.info("installed watcher to ./{}", folder);
        return this;
    }

    public boolean hasWatcher() {
        return hasWatcher;
    }

    public void dodge() {
        dodge = true;
    }

    public Tree load0(Path p, String id) throws Exception {
        if (!Files.exists(p)) return null;
        FileSerializer<String> serializer = getSerializer(p);
        if (serializer == null) {
            LOGGER.error("No serializer found for loading file {}", p);
            return null;
        }
        try {
            return serializer.deserialize(read(p));
        } catch (Exception e) {
            LOGGER.error("Failed to load config ID {}, marking file as corrupted, config will be reset!", id, e);
            Files.move(p, folder.resolve(id + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
            return null;
        }
    }

    @Override
    protected Tree load0(@NotNull String id) throws Exception {
        return load0(folder.resolve(id), id);
    }

    @Override
    protected boolean save0(@NotNull Tree tree) {
        Path p = folder.resolve(tree.getID());
        FileSerializer<String> serializer = getSerializer(p);
        if (serializer == null) {
            LOGGER.error("No serializer found for saving file {}", p);
            return false;
        }
        write(p, serializer.serialize(tree));
        dodge();
        return true;
    }

    @Override
    protected boolean delete0(@NotNull Tree tree) throws Exception {
        Path p = folder.resolve(tree.getID());
        if (!Files.exists(p)) return false;
        Files.delete(p);
        dodge();
        return true;
    }

    /**
     * Gather all files in this directory and make trees of them where possible.
     */
    public Collection<Tree> gatherAll() {
        return gatherAll(null);
    }

    /**
     * Inspect all files in this directory (and optionally the given directory) and make trees of them where possible.
     */
    public Collection<Tree> gatherAll(@Nullable String sub) {
        ArrayList<Tree> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mkdirs(sub != null ? folder.resolve(sub) : folder))) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p) || p.toString().contains(".corrupted")) continue;
                FileSerializer<String> serializer = getSerializer(p);
                if (serializer == null) continue;
                try {
                    Tree t = serializer.deserialize(read(p));
                    t.setID(folder.relativize(p).toString());
                    out.add(t);
                } catch (Exception e) {
                    LOGGER.error("didn't gather tree from {}: {}", p, e.getMessage());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to gather all configs", e);
        }
        out.trimToSize();
        return out;
    }

    @UnmodifiableView
    public Collection<FileSerializer<String>> getSerializers() {
        return serializers.values();
    }

    public void addSerializer(FileSerializer<String> serializer) {
        for (String ext : serializer.getExtensions()) {
            FileSerializer<?> out = this.serializers.putIfAbsent(ext, serializer);
            if (out != null) LOGGER.warn("Serializer already registered for extension {}", ext);
        }
    }

    @Override
    protected boolean corrupt0(Tree t) {
        Path p = folder.resolve(t.getID());
        try {
            Files.move(p, folder.resolve(t.getID() + ".corrupted"), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to mark config {} as corrupted", t.getID(), e);
            return false;
        }
    }

    protected FileSerializer<String> getSerializer(Path p) {
        String path = p.toString();
        int i = path.lastIndexOf('.');
        if (i == -1) {
            LOGGER.warn("no serializer set for file {}, using YAML", path);
            return serializers.get(".yml");
        }
        return serializers.get(path.substring(i));
    }
}
