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

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.polyfrost.oneconfig.api.config.serialize.impl.NightConfigSerializer;
import org.polyfrost.oneconfig.api.config.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.collect.PropertyCollector;
import org.polyfrost.oneconfig.api.config.collect.impl.OneConfigCollector;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.PreShutdownEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.polyfrost.oneconfig.api.config.Property.prop;
import static org.polyfrost.oneconfig.api.config.Tree.tree;

public final class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/Config");
    public static final Path PROFILES_DIR = Paths.get("profiles");

    private static final List<PropertyCollector> collectors = new ArrayList<>(1);
    private static final ConfigManager internal = new ConfigManager(Paths.get("OneConfig"), NightConfigSerializer.JSON);
    private static final ConfigManager core = new ConfigManager(Paths.get("config"), NightConfigSerializer.ALL);
    private static ConfigManager active;

    static {
        registerCollector(new OneConfigCollector());
    }

    private volatile boolean shutdown = false;
    private final FileBackend backend;


    @SuppressWarnings("unchecked")
    private ConfigManager(Path onto, FileSerializer<?>... serializers) {
        backend = new FileBackend(onto, (FileSerializer<String>[]) serializers);
    }

    /**
     * Returns a reference to the internal config manager, which is mounted onto the ./OneConfig directory.
     */
    @ApiStatus.Internal
    static ConfigManager internal() {
        return internal;
    }

    /**
     * Returns a reference to the active config manager, which is mounted to the current active profile.
     */
    public static ConfigManager active() {
        if (active == null) initialize();
        return active;
    }

    private static void initialize() {
        internal().register(
                tree("profiles.json").put(
                        prop("activeProfile", "")
                )
        );
        String activeProfile = internal().get("profiles.json").getProp("activeProfile").getAs();
        openProfile(activeProfile);
    }

    public static void openProfile(String profile) {
        internal().get("profiles.json").getProp("activeProfile").setAs(profile);
        internal().save("profiles.json");
        if (profile.isEmpty()) active = core();
        else {
            LOGGER.info("opening profile {}", profile);
            active = new ConfigManager(PROFILES_DIR.resolve(profile), NightConfigSerializer.ALL).withHook().withWatcher();
        }
    }

    /**
     * Returns a reference to the core config manager, which is mounted onto the ./config directory.
     * <b>internal use only!</b>
     */
    @ApiStatus.Internal
    public static ConfigManager core() {
        return core;
    }

    @UnmodifiableView
    public Collection<Tree> trees() {
        return backend.getTrees();
    }

    public Tree get(String id) {
        return backend.get(id);
    }

    public boolean save(String id) {
        return backend.save(id);
    }

    public Path getFolder() {
        return backend.folder;
    }

    public boolean register(Tree t) {
        return backend.register(t);
    }


    public Tree register(@NotNull Object o, @NotNull String id) {
        Tree t = collect(o, id);
        if (t == null) return null;
        if (!backend.register(t)) return null;
        return t;
    }

    @ApiStatus.Internal
    public static Tree collect(@NotNull Object o, @NotNull String id) {
        if (o instanceof Tree) return (Tree) o;
        for (PropertyCollector collector : collectors) {
            Tree t = collector.collect(o);
            if (t != null) {
                t.setID(id);
                t.lock();
                return t;
            }
        }
        LOGGER.error("No registered collector for object {}", o.getClass().getName());
        return null;
    }

    /**
     * Register a collector that can be used to collect trees from objects. these are shared between all config managers.
     */
    public static void registerCollector(PropertyCollector collector) {
        collectors.add(collector);
    }

    private ConfigManager withHook() {
        // two hooks that guarantee that we save lol
        Runtime.getRuntime().addShutdownHook(new Thread(this::hook));
        EventManager.register(PreShutdownEvent.class, this::hook);
        return this;
    }

    private ConfigManager withWatcher() {
        try {
            backend.addWatcher();
        } catch (Exception e) {
            LOGGER.error("Failed to register watcher onto {}", backend.folder, e);
        }
        return this;
    }

    private void hook() {
        if (shutdown) return;
        shutdown = true;
        LOGGER.info("shutdown requested; saving all configs in {}", backend.folder.getFileName());
        for (Tree t : backend.getTrees()) {
            backend.save(t);
        }
    }
}
