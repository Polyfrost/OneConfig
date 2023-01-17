/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.api.v1.config.processor.serializers;

import cc.polyfrost.oneconfig.api.v1.config.OneConfig;
import cc.polyfrost.oneconfig.api.v1.config.option.OptionHolder;

import java.util.List;

/**
 * Represents a serializer for {@link OneConfig} objects.
 */
public interface Serializer {
    /**
     * Serializes the given {@link OneConfig} with the given {@link OptionHolder}s, then writing the result
     * to {@link OneConfig#getPath()}.
     * @param config The {@link OneConfig} to serialize.
     * @param options The {@link OptionHolder}s to serialize.
     */
    void serialize(OneConfig config, List<OptionHolder> options);

    /**
     * Deserializes the given {@link OneConfig} from {@link OneConfig#getPath()} to the given {@link OptionHolder}s.
     * @param config The {@link OneConfig} to deserialize.
     * @param options The {@link OptionHolder}s to deserialize.
     */
    void deserialize(OneConfig config, List<OptionHolder> options);
}
