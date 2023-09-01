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

package org.polyfrost.oneconfig.internal.config;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Config;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.backend.Backend;
import org.polyfrost.oneconfig.api.config.backend.impl.file.FileBackend;
import org.polyfrost.oneconfig.api.config.collector.PropertyCollector;
import org.polyfrost.oneconfig.api.config.data.Category;
import org.polyfrost.oneconfig.api.config.data.Mod;
import org.polyfrost.polyui.input.Translator;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    public static final Path ONECONFIG_DIR = new File("./OneConfig").toPath();
    public static final ConfigManager INSTANCE = new ConfigManager();
    private final List<PropertyCollector> collectors = new ArrayList<>(5);
    private final Map<Object, Mod> mods = new HashMap<>();
    private final Backend backend;


    private ConfigManager() {
        collectors.add(new AnnotationReflectiveCollector());
        backend = new FileBackend(ONECONFIG_DIR.resolve("default"));
    }

    public void openProfile(String profile) {
        if (backend instanceof FileBackend) {
            ((FileBackend) backend).setDirectory(ONECONFIG_DIR.resolve(profile));
        } else throw new UnsupportedOperationException("Cannot change backend directory for non-file backend");
    }

    public Collection<Mod> getMods() {
        return mods.values();
    }

    public void refresh() {
        backend.refresh();
    }

    public boolean refresh(String id) {
        return backend.get(id) != null;
    }

    public boolean registerConfig(String id, Object config) {
        Tree t = backend.get(id);
        if (t == null) {
            t = collectAndOverwrite(id, config, null);
        } else {
            collectAndOverwrite(id, config, t);
        }
        assert t.id.equals(id);
        backend.put(t);
        mods.putIfAbsent(config, new Mod(null, new Translator.Text(config.getClass().getSimpleName()), Category.OTHER, t));
        mods.get(config).setConfig(t);
        return true;
    }

    private @NotNull Tree collectAndOverwrite(String id, Object config, Tree in) {
        Tree t = null;
        for (PropertyCollector collector : collectors) {
            t = collector.collect(id, config);
            if (t != null) {
                break;
            }
        }
        if (in != null) {
            in.overwriteWith(t, true);
            return in;
        }
        if (t == null) throw new IllegalArgumentException("Cannot collect properties from " + config.getClass().getSimpleName() + ", no registered collectors support it");
        return t;
    }

    public boolean registerConfig(Config config) {
        mods.putIfAbsent(config, config.mod);
        return registerConfig(config.id, config);
    }

    public void registerCollector(PropertyCollector collector) {
        collectors.add(collector);
    }
}
