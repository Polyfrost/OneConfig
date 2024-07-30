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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.collect.PropertyCollector;
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector;
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.PolyColorAdapter;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.polyfrost.oneconfig.api.config.v1.Tree.tree;

public final class ConfigManager {
    public static final Path PROFILES_DIR = Paths.get("profiles");
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    private static final List<PropertyCollector> collectors = new ArrayList<>(1);
    private static final ConfigManager internal = new ConfigManager(Paths.get("oneconfig"), NightConfigSerializer.ALL);
    private static final ConfigManager core = new ConfigManager(Paths.get("config"), NightConfigSerializer.ALL);
    private static ConfigManager active;

    static {
        ObjectSerializer.INSTANCE.registerTypeAdapter(new PolyColorAdapter());
        registerCollector(new OneConfigCollector());
    }

    private final FileBackend backend;
    private volatile boolean shutdown = false;


    @SuppressWarnings("unchecked")
    private ConfigManager(Path onto, FileSerializer<?>... serializers) {
        backend = new FileBackend(onto, (FileSerializer<String>[]) serializers);
    }

    /**
     * Returns a reference to the internal config manager, which is mounted onto the ./OneConfig directory.
     */
    @ApiStatus.Internal
    public static ConfigManager internal() {
        return internal;
    }

    /**
     * Returns a reference to the active config manager, which is mounted to the current active profile.
     */
    public static synchronized ConfigManager active() {
        if (active == null) initialize();
        return active;
    }

    private static synchronized void initialize() {
        String activeProfile = internal().register(
                tree("profiles.json").put(
                        Properties.simple("activeProfile", "Active Profile", "The profile which is currently open.", "")
                )
        ).getProp("activeProfile").getAs();
        openProfile(activeProfile);
    }

    public static synchronized void openProfile(String profile) {
        internal().get("profiles.json").getProp("activeProfile").setAs(profile);
        internal().save("profiles.json");
        if (profile.isEmpty()) {
            LOGGER.info("opened config manager onto root (no profile)");
            active = core().withWatcher().withHook();
        } else {
            LOGGER.info("opening profile {}", profile);
            active = new ConfigManager(PROFILES_DIR.resolve(profile), core.backend.getSerializers().toArray(new FileSerializer[0])).withHook().withWatcher();
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

    @ApiStatus.Internal
    public static Tree collect(@NotNull Object o, @NotNull String id) {
        if (o instanceof Tree) return (Tree) o;
        for (PropertyCollector collector : collectors) {
            Tree t = collector.collect(o);
            if (t != null) {
                t.setID(id);
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

    @UnmodifiableView
    public Collection<Tree> trees() {
        return backend.getTrees();
    }

    public @Nullable Tree load(String id) {
        return backend.load(id);
    }

    public Tree get(String id) {
        return backend.get(id);
    }

    @ApiStatus.Internal
    public Tree getNoRegister(Path p) throws Exception {
        return backend.load0(p, p.getFileName().toString());
    }

    public boolean save(String id) {
        return backend.save(id);
    }

    public boolean save(Tree t) {
        return backend.save(t);
    }

    public void saveAll() {
        backend.saveAll();
    }

    public Path getFolder() {
        return backend.folder;
    }

    public Tree register(Tree t) {
        return backend.register(t);
    }

    public boolean delete(String id) {
        return backend.delete(id);
    }

    @ApiStatus.Internal
    public Collection<Tree> gatherAll(String sub) {
        return backend.gatherAll(sub);
    }

    public Tree register(@NotNull Object o, @NotNull String id) {
        Tree t = collect(o, id);
        if (t == null) return null;
        return backend.register(t);
    }

    private ConfigManager withHook() {
        // two hooks that guarantee that we save lol
        // seems to improve the reliability of saving when the game crashes
        Runtime.getRuntime().addShutdownHook(new Thread(this::onClose));
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

    private synchronized void onClose() {
        if (shutdown) return;
        shutdown = true;
        LOGGER.info("shutdown requested; saving all configs in {}", backend.folder.getFileName());
        backend.saveAll();
    }
}
