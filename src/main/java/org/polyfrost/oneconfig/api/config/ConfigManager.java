/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.backend.impl.NightConfigSerializer;
import org.polyfrost.oneconfig.api.config.backend.impl.file.FileBackend;
import org.polyfrost.oneconfig.api.config.collector.PropertyCollector;
import org.polyfrost.oneconfig.internal.config.OneConfigCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    public static final Path CONFIG_DIR = new File("./config").toPath();
    public static final ConfigManager INSTANCE = new ConfigManager();
    public static final String DEFAULT_EXT = ".yaml";
    public static final String DEFAULT_META_EXT = "-meta.toml";
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Config Manager");
    private final List<PropertyCollector> collectors = new ArrayList<>(4);
    private final FileBackend backend;

    private ConfigManager() {
        collectors.add(new OneConfigCollector());
        backend = new FileBackend(CONFIG_DIR, NightConfigSerializer.ALL);
        defaultProfile();
    }

    public void defaultProfile() {
        openProfile("");
    }

    public void openProfile(String profile) {
        Path p = CONFIG_DIR.resolve(profile);
        p.toFile().mkdirs();
        //LOGGER.info("Opening profile at " + p);
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
        backend.setDirectory(p);
    }

    public Tree get(String id) {
        return backend.get(id);
    }

    public Collection<Tree> trees() {
        return backend.getTrees();
    }


    public void refresh() {
        backend.refresh();
    }

    public boolean refresh(String id) {
        return backend.get(id) != null;
    }

    /**
     * Register a config to OneConfig. This method essentially will merge the provided config with a file in the config backend.
     *
     * @param source the object to register as a config. If it is a Tree, then it is used directly, else, it is collected using a valid collector
     * @param cfg    the metadata instance for the config. If null, then a default one is created using the source's class name.
     * @throws IllegalArgumentException if no registered collector is able to collect from the source object
     */
    public Tree register(@NotNull Object source, @Nullable Config cfg) {
        cfg = cfg == null ? new Config(source.getClass().getSimpleName() + DEFAULT_EXT, null, source.getClass().getSimpleName(), Config.Category.OTHER) : cfg;
        Tree t = null;
        try {
            t = backend.get(cfg.id);
        } catch (Exception e) {
            LOGGER.error("Failed to read config! Data will be ignored", e);
            backend.remove(cfg.id, true);
        }
        // brand-new config to the system
        if (t == null) {
            LOGGER.info("Registering new config " + cfg.id);
            t = collect(source);
            backend.put(t);
        } else {
            t.merge(collect(source), false, true);
            backend.put(t);
        }
        t.addMetadata("meta", cfg);
        // check and potentially add metadata
        Tree metaTree = backend.get(cfg.id + DEFAULT_META_EXT);
        if(metaTree != null) {
            LOGGER.info("Registering metadata for config " + cfg.id);
            _supplyMetadata(t, metaTree);
        }
        return t;
    }

    public boolean supplyMetadata(String id, Tree metaTree, boolean save) {
        Tree t = backend.get(id);
        if(t == null) return false;
        if(save) {
            //metaTree.id = id + DEFAULT_META_EXT;
            Tree old = backend.get(metaTree.id);
            if(old != null) {
                old.merge(metaTree, false, false);
                metaTree = old;
            } else backend.put(metaTree);
        }
        _supplyMetadata(t, metaTree);
        return true;
    }

    private static void _supplyMetadata(Tree tree, Tree meta) {
        for(Map.Entry<String, Node> e : tree.map.entrySet()) {
            Node n = meta.get(e.getKey());
            if(n == null) continue;
            Node c = e.getValue();
            c.addMetadata(n.getMetadata());
            if(c instanceof Tree) {
                _supplyMetadata((Tree) c, (Tree) n);
            }
        }
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
