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

package cc.polyfrost.oneconfig.api.v1.config.processor;

import cc.polyfrost.oneconfig.api.v1.config.OneConfig;
import cc.polyfrost.oneconfig.api.v1.config.option.OptionHolder;
import cc.polyfrost.oneconfig.api.v1.config.processor.collector.AnnotationCollector;
import cc.polyfrost.oneconfig.api.v1.config.processor.collector.Collector;
import cc.polyfrost.oneconfig.api.v1.config.processor.serializers.GsonSerializer;
import cc.polyfrost.oneconfig.api.v1.config.processor.serializers.Serializer;
import cc.polyfrost.oneconfig.api.v1.config.property.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handles most of the processing of {@link OptionHolder} objects in {@link OneConfig}.
 */
public class OptionProcessor {
    private final List<Collector> collectors = new ArrayList<>();
    private final HashMap<Property, Serializer> serializers = new HashMap<>();
    public OptionProcessor() {
        collectors.add(new AnnotationCollector());
        serializers.put(Property.GSON_SERIALIZATION, new GsonSerializer());
    }

    /**
     * Collects all {@link OptionHolder} objects from the given {@link OneConfig}.
     * This is done by iterating through all {@link Collector} objects in {@link #collectors}.
     * @param config The {@link OneConfig} to collect {@link OptionHolder} objects from.
     * @return A {@link List} of all {@link OptionHolder} objects found in the given {@link OneConfig}.
     */
    public List<OptionHolder> collect(OneConfig config) {
        List<OptionHolder> holders = new ArrayList<>();
        for (Collector collector : collectors) {
            holders.addAll(collector.collect(config));
        }
        return holders;
    }

    /**
     * Serializes the given {@link OneConfig} with the given {@link OptionHolder}s, then writing the result
     * to {@link OneConfig#getPath()}.
     * This is done by iterating through all {@link Serializer} objects in {@link #serializers}.
     * @param config The {@link OneConfig} to serialize.
     * @param options The {@link OptionHolder}s to serialize.
     */
    public void serialize(OneConfig config, List<OptionHolder> options) {
        for (Property property : config.getProperties().getElements()) {
            if (serializers.containsKey(property)) {
                serializers.get(property).serialize(config, options);
                break;
            }
        }
    }

    /**
     * Deserializes the given {@link OneConfig} from {@link OneConfig#getPath()} to the given {@link OptionHolder}s.
     * This is done by iterating through all {@link Serializer} objects in {@link #serializers}.
     * @param config The {@link OneConfig} to deserialize.
     * @param options The {@link OptionHolder}s to deserialize.
     */
    public void deserialize(OneConfig config, List<OptionHolder> options) {
        for (Property property : config.getProperties().getElements()) {
            if (serializers.containsKey(property)) {
                serializers.get(property).deserialize(config, options);
            }
        }
    }
}
