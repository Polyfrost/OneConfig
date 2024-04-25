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

package org.polyfrost.oneconfig.api.config.v1.serialize.impl;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.InMemoryFormat;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.electronwill.nightconfig.yaml.YamlParser;
import com.electronwill.nightconfig.yaml.YamlWriter;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.v1.Node;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.util.ObjectSerializer;
import org.polyfrost.oneconfig.utils.v1.WrappingUtils;

import java.util.HashMap;
import java.util.Map;

import static org.polyfrost.oneconfig.api.config.v1.Property.prop;
import static org.polyfrost.oneconfig.api.config.v1.Tree.tree;

public class NightConfigSerializer implements FileSerializer<String> {
    public static final FileSerializer<String> TOML = new NightConfigSerializer(new TomlWriter(), new TomlParser(), ".toml");       // 90 KB
    public static final FileSerializer<String> JSON = new NightConfigSerializer(JsonFormat.fancyInstance().createWriter(), JsonFormat.fancyInstance().createParser(), ".json");  // 55KB
    // public static final FileSerializer HOCON = new NightConfigSerializer(new HoconWriter(), new HoconParser(), ".hocon");        // 1.1MB
    public static final FileSerializer<String> YAML = new NightConfigSerializer(new YamlWriter(), new YamlParser(YamlFormat.defaultInstance()), ".yaml", ".yml");       // 1.2MB
    public static final FileSerializer<?>[] ALL = {TOML, JSON, YAML};

    final ConfigWriter writer;
    final ConfigParser<?> reader;
    final String[] formats;

    public NightConfigSerializer(ConfigWriter writer, ConfigParser<?> reader, String... formats) {
        this.writer = writer;
        this.reader = reader;
        this.formats = formats;
    }

    @SuppressWarnings("unchecked")
    private static void write(Tree c, Config cfg) {
        for (Map.Entry<String, Node> e : c.map.entrySet()) {
            Node n = e.getValue();
            if (n instanceof Property<?>) {
                Property<?> p = (Property<?>) n;
                // dummy
                if (p.type == Void.class) continue;
                Object o = p.get();
                if (o == null) continue;
                if (!WrappingUtils.isSimpleObject(o)) {
                    Object out = ObjectSerializer.INSTANCE.serialize(o, true);
                    if (out instanceof Map) cfg.add(n.getID(), mapsToConfigs((Map<String, Object>) out));
                    else cfg.add(n.getID(), out);
                } else cfg.add(n.getID(), o);
            } else {
                Tree in = (Tree) n;
                Config child = new BackedConfig(new HashMap<>(in.map.size(), 1f));
                write(in, child);
                cfg.add(n.getID(), child);
            }
        }
    }

    private static Tree read(Map<String, Object> cfg, Tree b) {
        for (Map.Entry<String, Object> e : cfg.entrySet()) {
            if (e.getValue() instanceof Config) {
                Config c = (Config) e.getValue();
                if (c.get("class") != null) {
                    b.put(prop(e.getKey(), ObjectSerializer.INSTANCE.deserialize(((Config) e.getValue()).valueMap())));
                } else b.put(read(c.valueMap(), tree(e.getKey())));
            } else {
                Object v = e.getValue();
                b.put(prop(e.getKey(), v));
            }
        }
        return b;
    }

    @SuppressWarnings("unchecked")
    private static Config mapsToConfigs(Map<String, Object> map) {
        Config c = new BackedConfig(map);
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Map) {
                Config cc = mapsToConfigs((Map<String, Object>) e.getValue());
                c.set(e.getKey(), cc);
            } else {
                c.add(e.getKey(), e.getValue());
            }
        }
        return c;
    }

    @Override
    public String[] getExtensions() {
        return formats;
    }

    @Override
    public @NotNull String serialize(@NotNull Tree c) {
        Config cfg = Config.inMemory();
        write(c, cfg);
        return writer.writeToString(cfg);
    }

    @Override
    public @NotNull Tree deserialize(@NotNull String src) {
        if (src.isEmpty()) throw new IllegalArgumentException("cannot deserialize empty string");
        Config cfg = reader.parse(src);
        return read(cfg.valueMap(), tree());
    }

    private static class BackedConfig extends AbstractConfig {
        BackedConfig(Map<String, Object> map) {
            super(map);
        }

        @Override
        public AbstractConfig clone() {
            return new BackedConfig(valueMap());
        }

        @Override
        public Config createSubConfig() {
            return new BackedConfig(new HashMap<>());
        }

        @Override
        public ConfigFormat<?> configFormat() {
            return InMemoryFormat.withUniversalSupport();
        }
    }
}
