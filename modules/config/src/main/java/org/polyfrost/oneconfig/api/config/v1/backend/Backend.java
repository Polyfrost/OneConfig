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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.polyfrost.oneconfig.api.config.v1.Node;
import org.polyfrost.oneconfig.api.config.v1.Tree;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A backend is a storage system for ConfigTrees
 * <br>
 * It handles getting and putting ConfigTrees and also serializing and deserializing them
 */
public abstract class Backend {
    protected static final Logger LOGGER = LogManager.getLogger("OneConfig/Config");
    /**
     * Trees marked with this metadata are listed in the UI only such as Mod Menu compat entries
     * <br>
     * They are tracked in memory but never loaded from or written to backend storage
     */
    public static final String UI_ONLY_METADATA = "ui_only";
    public static final String UI_PLACEHOLDER_METADATA = "ui_placeholder";
    public static final String CUSTOM_SAVE_TRACKED_METADATA = "custom_save_tracked";
    public static final String CUSTOM_SAVE_DIRTY_METADATA = "custom_save_dirty";
    private final Map<String, Tree> trees = new ConcurrentHashMap<>();

    private static boolean isUiOnly(@NotNull Tree tree) {
        return Boolean.TRUE.equals(tree.getMetadata(UI_ONLY_METADATA));
    }

    private static boolean isUiPlaceholder(@NotNull Tree tree) {
        return Boolean.TRUE.equals(tree.getMetadata(UI_PLACEHOLDER_METADATA));
    }

    private static void runCustomSave(@NotNull Tree tree, @NotNull Runnable customSave) {
        boolean tracked = Boolean.TRUE.equals(tree.getMetadata(CUSTOM_SAVE_TRACKED_METADATA));
        if (tracked && !Boolean.TRUE.equals(tree.getMetadata(CUSTOM_SAVE_DIRTY_METADATA))) return;
        customSave.run();
        if (tracked) tree.removeMetadata(CUSTOM_SAVE_DIRTY_METADATA);
    }

    /**
     * Register the given config with the system
     * <br>
     * This is a hybrid of the load and save methods depending on the state of the given tree
     * <br>
     * New trees will be saved to the backend
     * <br>
     * Existing trees will be loaded and have the backend data overwritten onto them
     * <br>
     * <b>NOTE</b> do NOT use the parameter tree after this method is called because it may have been cleared
     * <br>
     * Use the returned tree instead
     */
    public final RegistrationResult register(@NotNull Tree in) {
        if (in.getID() == null) throw new IllegalArgumentException("ID must be set before registering");
        // warm storage lookup
        Tree current = trees.get(in.getID());

        if (current != null) {
            // already tracked so merge the input data into the stored tree and return that one

            if (isUiOnly(current) && isUiOnly(in)) {
                if (isUiPlaceholder(current) && !isUiPlaceholder(in)) {
                    trees.remove(in.getID());
                    putSafe(in);
                    return new RegistrationResult(in, RegistrationResult.NEW);
                }
                in.clear();
                return new RegistrationResult(current, RegistrationResult.MERGED);
            }
            if (isUiOnly(current)) {
                trees.remove(in.getID());
                if (load(in)) {
                    return new RegistrationResult(in, RegistrationResult.LOADED);
                }
                save(in);
                return new RegistrationResult(in, RegistrationResult.NEW);
            }

            if (in.map.isEmpty()) return new RegistrationResult(current, RegistrationResult.MERGED);
            LOGGER.info("performing tree merge between {} and {}", current.getTitle(), in.getTitle());
            current.overwrite(in, false);
            // clear the old tree to prevent illegal usage
            in.clear();
            save(current);
            return new RegistrationResult(current, RegistrationResult.MERGED);
        } else {
            if (isUiOnly(in)) {
                putSafe(in);
                return new RegistrationResult(in, RegistrationResult.NEW);
            }
            // untracked so start tracking it and pull any data that exists in cold storage
            if (load(in)) {
                return new RegistrationResult(in, RegistrationResult.LOADED);
            } else {
                save(in);
                return new RegistrationResult(in, RegistrationResult.NEW);
            }
        }
    }

    /**
     * Load in a tree with the given ID
     * <br>
     * Returns null if the tree does not exist
     */
    protected abstract Tree load0(@NotNull String id) throws Exception;

    /**
     * Load the tree with data stored in this backend
     *
     * @return true if the tree was modified by the load operation
     */
    public final boolean load(Tree tree) {
        if (isUiOnly(tree)) {
            putSafe(tree);
            return true;
        }
        if (tree.getMetadata("custom_save") != null) {
            putSafe(tree);
            return true;
        }
        if (tree.getID() == null) throw new IllegalArgumentException("tree must be master (have a valid ID)");
        Tree t;
        try {
            t = load0(tree.getID());
        } catch (Exception e) {
            LOGGER.error("error loading tree with ID {}!", tree.getID(), e);
            return false;
        }
        if (t == null) return false;
        if (t.get("reserved:overwritten") != null) {
            tree.put(Objects.requireNonNull(t.get("reserved:overwritten")));
        }
        tree.overwrite(t, false);

        putSafe(tree);
        return true;
    }

    /**
     * Load a tree with the given ID
     *
     * @return the tree or null if it could not be loaded or does not exist
     */
    public final @Nullable Tree load(@NotNull String id) {
        try {
            return load0(id);
        } catch (Exception e) {
            LOGGER.error("error loading tree with ID {}!", id, e);
            return null;
        }
    }

    protected abstract boolean save0(@NotNull Tree tree) throws Exception;

    /**
     * Save a tree with the given ID
     *
     * @return true if the tree was saved successfully
     * @throws IllegalArgumentException if no tree with the given ID is registered
     */
    public final boolean save(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        Tree tree = trees.get(id);
        if (tree == null) throw new IllegalArgumentException("no registered tree with ID " + id);
        try {
            Object customSave = tree.getMetadata("custom_save");
            if (customSave != null) {
                if (customSave instanceof Runnable) {
                    runCustomSave(tree, (Runnable) customSave);
                }
                return true;
            }
            if (isUiOnly(tree)) return true;
            return save0(tree);
        } catch (Exception e) {
            LOGGER.error("error saving tree with ID {}!", id, e);
            return false;
        }
    }

    /**
     * Save all trees under the given path
     */
    public final void saveAll(@Nullable String matching) {
        String m = (String) Node.strv(matching);
        if (m == null) {
            for (Tree t : trees.values()) {
                save(t);
            }
        } else {
            for (Tree t : trees.values()) {
                if (t.getID().startsWith(m)) {
                    save(t);
                }
            }
        }
    }

    /**
     * Save all trees registered to this backend
     */
    public final void saveAll() {
        saveAll(null);
    }

    /**
     * Save the provided tree to the backend
     * <br>
     * If it is not registered when this is called it will be registered
     *
     * @return true if the tree was saved successfully
     * @throws IllegalStateException if the tree has become synchronized with the backend
     */
    public final boolean save(Tree tree) {
        if (tree.getID() == null) throw new IllegalArgumentException("tree must be master (have a valid ID)");
        putSafe(tree);
        try {
            Object customSave = tree.getMetadata("custom_save");
            if (customSave != null) {
                if (customSave instanceof Runnable) {
                    runCustomSave(tree, (Runnable) customSave);
                }
                return true;
            }
            if (isUiOnly(tree)) return true;
            return save0(tree);
        } catch (Exception e) {
            LOGGER.error("error saving tree with ID {}!", tree.getID(), e);
            return false;
        }
    }

    protected abstract boolean delete0(@NotNull Tree tree) throws Exception;

    /**
     * Untrack and permanently delete the tree with the given ID
     */
    public final boolean delete(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        Tree tree = trees.remove(id);
        if (tree == null) return false;
        if (tree.getID() == null) throw new IllegalArgumentException("tree must be master (have a valid ID)");
        try {
            return delete0(tree);
        } catch (Exception e) {
            LOGGER.error("error deleting tree with ID {}!", tree.getID(), e);
            return false;
        }
    }

    /**
     * Stops tracking a tree without deleting its stored data.
     */
    @ApiStatus.Internal
    public final @Nullable Tree unregister(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        return trees.remove(id);
    }

    public boolean exists(String id) {
        if (id == null) return false;
        return trees.containsKey(id);
    }

    public final Tree get(String id) {
        if (id == null) return null;
        return trees.computeIfAbsent(id, key -> {
            Tree t = load(key);
            if (t != null && t.getID() == null) t.setID(key);
            return t;
        });
    }

    @UnmodifiableView
    public final Collection<Tree> getTrees() {
        return Collections.unmodifiableCollection(trees.values());
    }

    /**
     * Request that the tree with the given ID needs to be updated for example when the file has been edited externally
     */
    @ApiStatus.Experimental
    protected void requestUpdate(String id) {
        if (id == null) throw new NullPointerException("id cannot be null");
        Tree tree = trees.get(id);
        if (tree == null) {
            LOGGER.warn("can't update: no registered tree with ID {}", id);
            return;
        }
        if (isUiOnly(tree)) return;
        load(tree);
    }

    /**
     * Explicitly mark a tree as corrupted
     * <br>
     * The given tree if present will be untracked by this backend
     * <br> this method is also automatically called when a tree fails to load <i>(implementation specific operation)</i>
     *
     * @param id the ID of the tree to mark as corrupted
     * @return true if the tree was marked as corrupted or false if the tree was not found
     */
    @ApiStatus.Experimental
    public final boolean corrupt(String id) {
        Tree t = trees.remove(id);
        if (t == null) return false;
        return corrupt0(t);
    }

    protected abstract boolean corrupt0(Tree t);

    protected void putSafe(Tree in) {
        // the tree must either be new or be the exact same instance as the one already stored
        // anything else means the backend has desynced
        Tree out = trees.put(in.getID(), in);
        if (out == null) return;
        if (out != in) {
            throw new IllegalStateException("Backend desync detected for tree " + in.getID());
        }
    }


    public static final class RegistrationResult {
        public final Tree tree;
        public final byte state;
        /**
         * This tree has never been registered before or could not be found in the backend storage
         */
        public static final byte NEW = 0;
        /**
         * This tree has been seen before by the backend so data has been loaded into it from the backend storage
         */
        public static final byte LOADED = 1;
        /**
         * This tree was already registered to the active system and has been merged with the input tree
         */
        public static final byte MERGED = 2;

        public RegistrationResult(Tree tree, byte state) {
            this.tree = tree;
            this.state = state;
        }

        public Tree get() {
            return tree;
        }
    }
}
