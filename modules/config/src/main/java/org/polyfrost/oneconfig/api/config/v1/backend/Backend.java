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

package org.polyfrost.oneconfig.api.config.v1.backend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.config.v1.Tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A backend is a storage system for ConfigTrees.
 * <br>
 * It is responsible for getting and putting ConfigTrees, and by extension, serializing/deserializing them.
 */
public abstract class Backend {
    protected static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    private final Map<String, Tree> trees = new HashMap<>();


    /**
     * Register the given config with the system. This is a hybrid of the load and save methods, depending on the state of the given tree.
     * <br>
     * New trees will be saved to the backend, while existing trees will be loaded and data overwritten on them with the backend data.
     * <br>
     * <b>NOTE:</b> do NOT use the parameter tree after this method is called, as it may have been cleared. Use the returned tree instead.
     */
    public final Tree register(@NotNull Tree in) {
        if (in.getID() == null) throw new IllegalArgumentException("ID must be set before registering");
        Tree current = trees.get(in.getID());
        if (current != null) {
            // no point in merging if the tree is empty.
            if (in.map.isEmpty()) return current;
            LOGGER.info("performing tree merge between {} and {}", current.getTitle(), in.getTitle());
            current.overwrite(in);
            // clear the old tree to prevent illegal usage.
            in.clear();
            save(current);
            return current;
        } else {
            if (load(in)) return in;
            else save(in);
            return in;
        }
    }

    /**
     * Load in a tree with the given ID. if the tree does not exist, return null.
     */
    protected abstract Tree load0(@NotNull String id) throws Exception;

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

        putSafe(tree);
        return true;
    }

    protected abstract boolean save0(@NotNull Tree tree) throws Exception;

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
        putSafe(tree);
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

    public final Tree get(String id) {
        return trees.get(id);
    }

    public final Collection<Tree> getTrees() {
        return trees.values();
    }

    /**
     * Request that the tree with the given ID needs to be updated, for example, the file has been edited externally.
     */
    @ApiStatus.Experimental
    protected void requestUpdate(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        Tree tree = trees.get(id);
        if (tree == null) {
            LOGGER.warn("can't update: no registered tree with ID {}", id);
            return;
        }
        load(tree);
    }

    protected void putSafe(Tree in) {
        // sanity check.
        // the tree here should either be new, or be the same tree as the one that was here before.
        // if it's not, then something wierd must've happened. this prevents wierd issues with backend desync.
        Tree out = trees.put(in.getID(), in);
        // new: OK
        if (out == null) return;
        if (out != in) {
            throw new IllegalStateException("Backend desync detected for tree " + in.getID());
        }
    }
}
