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

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.backend.impl.NightConfigSerializer;
import org.polyfrost.oneconfig.api.config.backend.impl.file.FileBackend;
import org.polyfrost.oneconfig.api.config.backend.impl.file.FileSerializer;
import org.polyfrost.oneconfig.api.config.collect.PropertyCollector;
import org.polyfrost.oneconfig.api.config.collect.impl.OneConfigCollector;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.PreShutdownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Config Manager");
    public static final Path CONFIG_DIR = new File("./config").toPath();
    public static final FileSerializer<String> DEFAULT_SERIALIZER = NightConfigSerializer.JSON;
    public static final String DEFAULT_META_EXT = "-meta.toml";
    public static final ConfigManager INSTANCE = new ConfigManager();
    private final List<PropertyCollector> collectors = new ArrayList<>(4);
    private static volatile boolean shutdown = false;
    private FileBackend backend;

    private ConfigManager() {
        registerCollector(new OneConfigCollector());
        defaultProfile();
        registerShutdownHook();
    }

    public void defaultProfile() {
        openProfile("");
    }

    public void openProfile(String profile) {
        Path p = CONFIG_DIR.resolve(profile);
        p.toFile().mkdirs();
        LOGGER.info("Opening profile at {}", p);
        File[] files = CONFIG_DIR.toFile().listFiles();
        assert files != null;
        for (File cfg : files) {
            if (cfg.isDirectory()) continue;
            try {
                Files.copy(cfg.toPath(), p.resolve(cfg.getName()));
            } catch (FileAlreadyExistsException ignored) {
            } catch (Exception e) {
                throw new RuntimeException("Failed to copy over config file " + cfg.getName() + " to profile " + p, e);
            }
        }
        if (backend == null) backend = new FileBackend(p, DEFAULT_SERIALIZER);
        else backend = new FileBackend(p, backend.serializer);
    }

    private void registerShutdownHook() {
        // two hooks that guarantee that we save lol
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownHook));
        EventManager.register(PreShutdownEvent.class, this::shutdownHook);
    }

    private void shutdownHook() {
        if (shutdown) return;
        shutdown = true;
        LOGGER.info("shutdown requested; saving all configs...");
        for (Tree t : backend.getTrees()) {
            backend.save(t);
        }
    }

    public Tree get(String id) {
        return backend.get(id);
    }

    public Collection<Tree> trees() {
        return backend.getTrees();
    }


    /**
     * Register a config to OneConfig. This method essentially will merge the provided config with a file in the config backend.
     *
     * @param source the object to register as a config. If it is a Tree, then it is used directly, else, it is collected using a valid collector
     * @param cfg    the metadata instance for the config. If null, then a default one is created using the source's class name.
     * @throws IllegalArgumentException if no registered collector is able to collect from the source object
     */
    public Tree register(@NotNull Object source, @Nullable Config cfg) {
        long now = System.nanoTime();
        cfg = cfg == null ? new Config(source.getClass().getSimpleName(), (String) null, source.getClass().getSimpleName(), Config.Category.OTHER) : cfg;
        Tree t = collect(source);
        t.setID(cfg.id);
        t.lock();
        if (backend.load(t)) {
            LOGGER.info("Loaded config {} from backend (took {}ms)", cfg.id, (System.nanoTime() - now) / 1_000_000f);
        } else {
            LOGGER.info("Config {} not found in backend, saving...", cfg.id);
            backend.save(t);
        }
        return t;
    }

    public Tree register(@NotNull Config config) {
        return register(config, config);
    }

    private Tree collect(Object o) {
        if (o instanceof Tree) return (Tree) o;
        for (PropertyCollector collector : collectors) {
            Tree t = collector.collect(o);
            if (t != null) return t;
        }
        throw new IllegalArgumentException("No registered collector for object " + o.getClass().getName() + "!");
    }

    public void registerCollector(PropertyCollector collector) {
        collectors.add(collector);
    }
}
