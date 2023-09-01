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

package org.polyfrost.oneconfig.api.config.backend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A backend is a storage system for ConfigTrees.
 * <br>
 * It is responsible for getting and putting ConfigTrees, and by extension, serializing/deserializing them.
 */
public interface Backend {
    Logger LOGGER = LoggerFactory.getLogger("OneConfig Config API Backend");

    /**
     * Put a ConfigTree into the storage system.
     */
    void put(@NotNull Tree tree);

    /**
     * Get a ConfigTree from the storage system.
     *
     * @param id the ID of the ConfigTree to get.
     * @return the ConfigTree, or null if it does not exist.
     */
    @Nullable Tree get(@NotNull String id);

    boolean remove(@NotNull String id);

    default boolean remove(@NotNull Tree tree) {
        return remove(tree.id);
    }

    boolean exists(@NotNull String id);

    default boolean exists(@NotNull Tree tree) {
        return exists(tree.id);
    }

    /**
     * Refresh all currently registered ConfigTrees.
     */
    void refresh();
}
