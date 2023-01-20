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

package cc.polyfrost.oneconfig.api.v1.config.property;

import cc.polyfrost.oneconfig.api.v1.Namable;
import cc.polyfrost.oneconfig.api.v1.OneCollection;
import cc.polyfrost.oneconfig.api.v1.config.OneConfig;

/**
 * A class that represents and holds a value, and can be used to get that value.
 * Used in {@link OneConfig} to change certain behavior.
 *
 * @see OneCollection
 */
public class Property implements Namable {
    public static final Property NO_CATEGORY_SERIALIZATION = new Property("no_category_serialization", true);
    public static final Property SERIALIZE_BASED_ON_NAME = new Property("serialize_based_on_name", true);
    public static final Property SERIALIZE_BASED_ON_FIELD = new Property("serialize_based_on_field", true);
    public static final Property GSON_SERIALIZATION = new Property("gson_serialization", true);

    private final String name;
    private final Object value;

    public Property(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
