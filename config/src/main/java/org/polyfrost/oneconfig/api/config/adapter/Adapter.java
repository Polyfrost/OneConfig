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

package org.polyfrost.oneconfig.api.config.adapter;

import org.polyfrost.oneconfig.api.config.util.ObjectSerializer;

import java.util.List;
import java.util.Map;

/**
 * While the Config API is equipped with an automatic deserializer (see {@link ObjectSerializer#serialize(Object)})
 * It is not perfect, and may produce ugly looking results. <br>
 * For this reason, the Adapter API exists, where you can create your own custom serializer for an object into a map of "simple" objects and string keys.
 *
 * @param <T> the type of this adapter
 * @see Adapter#serialize(Object)
 */
public abstract class Adapter<T> {
    /**
     * Deserialize your object. The form will be identical to what you made in the {@link #serialize(Object)} method.
     *
     * @param in the object (same as deserialize) to create your object from
     * @return the complete object
     */
    public abstract T deserialize(Object in);

    /**
     * Convert the given object into a serializable form.
     * <br>
     * The following types can be returned by this method, and nothing more:
     * <ul>
     *     <li>Primitive wrappers (any {@link Number}, {@link Character}, {@link CharSequence})</li>
     *     <li>An {@link Enum}</li>
     *     <li>A {@link List} of the above types</li>
     *     <li>A {@link Map}{@code <String, Object>} of the above types, mapped to Strings, which are used as keys.</li>
     *     <li>Lists containing lists or maps containing maps (or lists) are also supported.</li>
     * </ul>
     *
     * @param in the object to serialize
     */
    public abstract Object serialize(T in);

    /**
     * Due to Java type erasure, this method is used to get the type of the adapter. <br>
     * This is used internally to get what type adapter to use.
     */
    public abstract Class<T> getTargetClass();

    public final boolean equals(Object obj) {
        if (obj instanceof Adapter<?>) {
            return ((Adapter<?>) obj).getTargetClass() == this.getTargetClass();
        }
        return false;
    }
}
