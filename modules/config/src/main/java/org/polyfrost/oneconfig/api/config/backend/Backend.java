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

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A backend is a storage system for ConfigTrees.
 * <br>
 * It is responsible for getting and putting ConfigTrees, and by extension, serializing/deserializing them.
 */
public abstract class Backend {
    public static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Config API Backend");
    private final Map<String, Tree> trees = new HashMap<>();

    /**
     * Load in a tree with the given ID. if the tree does not exist, return null.
     */
    protected abstract Tree load0(@NotNull String id);

    /**
     * Load a tree with data stored in this backend.
     */
    public final boolean load(Tree tree) {
        if (tree.getID() == null) throw new IllegalArgumentException("tree must be master (have a valid ID)");
        Tree t;
        try {
            t = load0(tree.getID());
        } catch (Exception e) {
            LOGGER.error("error loading tree with ID {}!", tree.getID(), e);
            return false;
        }
        if (t == null) return false;
        tree.overwrite(t);
        trees.putIfAbsent(tree.getID(), tree);
        return true;
    }

    protected abstract boolean save0(@NotNull Tree tree);

    public final boolean save(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        Tree tree = trees.get(id);
        if (tree == null) throw new IllegalArgumentException("no registered tree with ID " + id);
        try {
            return save0(tree);
        } catch (Exception e) {
            LOGGER.error("error saving tree with ID {}!", id, e);
            return false;
        }
    }

    public final boolean save(Tree tree) {
        if (tree.getID() == null) throw new IllegalArgumentException("tree must be master (have a valid ID)");
        trees.putIfAbsent(tree.getID(), tree);
        try {
            return save0(tree);
        } catch (Exception e) {
            LOGGER.error("error saving tree with ID {}!", tree.getID(), e);
            return false;
        }
    }

    public boolean exists(String id) {
        return trees.containsKey(id);
    }

    public Tree get(String id) {
        return trees.get(id);
    }

    public Collection<Tree> getTrees() {
        return trees.values();
    }

    /**
     * Request that the tree with the given ID needs to be updated, for example, the file has been edited externally.
     */
    @ApiStatus.Experimental
    protected void requestUpdate(String id) {
        Tree tree = trees.get(id);
        if (tree == null) {
            LOGGER.warn("can't update: no registered tree with ID {}", id);
            return;
        }
        load(tree);
    }

}
