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

package org.polyfrost.oneconfig.api.config.backend.impl;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.hocon.HoconParser;
import com.electronwill.nightconfig.hocon.HoconWriter;
import com.electronwill.nightconfig.json.JsonFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.electronwill.nightconfig.yaml.YamlFormat;
import com.electronwill.nightconfig.yaml.YamlParser;
import com.electronwill.nightconfig.yaml.YamlWriter;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.backend.impl.file.FileSerializer;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.polyfrost.oneconfig.api.config.Property.prop;
import static org.polyfrost.oneconfig.api.config.Tree.tree;

public class NightConfigSerializer implements FileSerializer {
    public static final FileSerializer TOML = new NightConfigSerializer(new TomlWriter(), new TomlParser(), ".toml");
    public static final FileSerializer JSON = new NightConfigSerializer(JsonFormat.fancyInstance().createWriter(), JsonFormat.fancyInstance().createParser(), ".json");
    public static final FileSerializer HOCON = new NightConfigSerializer(new HoconWriter(), new HoconParser(), ".hocon");
    public static final FileSerializer YAML = new NightConfigSerializer(new YamlWriter(), new YamlParser(YamlFormat.defaultInstance()), ".yaml");
    public static final FileSerializer[] ALL = {TOML, JSON, HOCON, YAML};

    final ConfigWriter writer;
    final ConfigParser<?> reader;
    final String format;

    public NightConfigSerializer(ConfigWriter writer, ConfigParser<?> reader, String format) {
        this.writer = writer;
        this.reader = reader;
        this.format = format;
    }


    @Override
    public @NotNull String serialize(@NotNull Tree c) {
        Config cfg = Config.inMemory();
        add(c, cfg);
        return writer.writeToString(cfg);
    }

    @Override
    public boolean supports(File file) {
        return file.getName().endsWith(format);
    }

    protected void add(Tree c, Config cfg) {
        for (Property<?> p : c.values) {
            Object o = p.get();
            if (!(p.isPrimitiveArray() || o instanceof CharSequence || o instanceof Number || o instanceof Boolean || o instanceof Enum || o instanceof Config || o instanceof Object[] || o instanceof List<?>)) {
                cfg.add(p.name, ObjectSerializer.INSTANCE.convertFromField(o));
            } else cfg.add(p.name, p.get());
        }
        for (Tree t : c.children) {
            Config child = cfg.createSubConfig();
            add(t, child);
            cfg.add(t.id, child);
        }
    }

    @Override
    public @NotNull Tree deserialize(@NotNull String id, @NotNull String src) {
        Config cfg = reader.parse(src);
        return read(cfg.valueMap(), tree(id));
    }

    protected static Tree read(Map<String, Object> cfg, Tree.Builder b) {
        for (Map.Entry<String, Object> e : cfg.entrySet()) {
            if (e.getValue() instanceof Config) {
                if (((Config) e.getValue()).get("classType") != null) {
                    b.put(prop(e.getKey(), ObjectSerializer.INSTANCE.convertToField((Config) e.getValue())));
                } else b.put(read(((Config) e.getValue()).valueMap(), tree(e.getKey())));
            } else {
                b.put(prop(e.getKey(), e.getValue()));
            }
        }
        return b.build();
    }
}
