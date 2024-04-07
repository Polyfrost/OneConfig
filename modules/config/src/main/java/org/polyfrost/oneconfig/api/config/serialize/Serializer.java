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

package org.polyfrost.oneconfig.api.config.serialize;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Tree;

/**
 * A serializer is a class that is capable of turning a {@link Tree} into a specific type {@link T}, and vice versa.
 * <br>
 * Trees are the primary data structure in OneConfig, and are used to represent configuration data. They are a tree structure,
 * with each node being a key-value pair of either a string and a sub-tree, or a string and some data.
 * <br>
 * <b>Trees can contain arbitrary, potentially complex data types in them</b>. You need to be able to serialize and deserialize this data into a specific type.
 * The easiest way to do this is to use the builtin {@link org.polyfrost.oneconfig.api.config.util.ObjectSerializer} which provides
 * simple methods to convert objects into simple types, along with the facility to use defined {@link org.polyfrost.oneconfig.api.config.serialize.adapter.Adapter}s.
 * The following types are used by this, and so you need to be able to serialize and deserialize them:
 * <ul>
 *     <li>primitive type wrappers (Number, Character, Boolean)</li>
 *     <li>CharSequence</li>
 *     <li>Lists or Arrays of the above types (at your discretion)</li>
 *     <li>Map (containing the above types).</li>
 * </ul>
 * If you don't want to use this facility, you can create your own complex object serializer and use that instead.
 * @param <T> the type of data that your serializer outputs and accepts as input.
 */
public interface Serializer<T> {
    /**
     * Serialize the given tree into type T.
     *
     * @return the serialized data
     */
    T serialize(@NotNull Tree config);

    /**
     * Return a new tree with the data in it.
     *
     * @param src the serialized data
     * @return the tree created from the data
     */
    Tree deserialize(T src);
}
