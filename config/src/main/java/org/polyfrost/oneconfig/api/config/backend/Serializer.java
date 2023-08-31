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

package org.polyfrost.oneconfig.api.config.backend;

import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Tree;

public interface Serializer {
    /**
     * Serialize the given tree into a string.
     *
     * @param c the tree to serialize. DO NOT MODIFY THIS TREE.
     * @return the serialized data
     */
    @NotNull String serialize(@NotNull Tree c);

    /**
     * Return a new tree with the data in it, and the ID field set to the given ID.
     *
     * @param id  the ID to set
     * @param src the serialized data
     * @return the tree created from the data
     */
    @NotNull Tree deserialize(@NotNull String id, @NotNull String src);
}
