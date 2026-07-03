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

package org.polyfrost.oneconfig.api.config.v1;

import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class CompatSnapshots implements ConfigManager.ProfileChangeListener {
    public static final CompatSnapshots INSTANCE = new CompatSnapshots();

    private static final String TAG = "oc_compat_snapshot";

    private final CompatSnapshotStore store = new CompatSnapshotStore();
    private final Map<String, Tree> known = new ConcurrentHashMap<>();
    private final Map<Property<?>, Boolean> wired = Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Property<?>> applying = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private volatile java.util.function.Consumer<Runnable> dispatcher = Runnable::run;
    private volatile String currentProfile;

    private CompatSnapshots() {
    }

    public static void setDispatcher(java.util.function.Consumer<Runnable> dispatcher) {
        INSTANCE.dispatcher = dispatcher == null ? Runnable::run : dispatcher;
    }

    public static Tree register(Tree tree) {
        return INSTANCE.register0(tree);
    }

    private Tree register0(Tree tree) {
        Tree reg = ConfigManager.active().register(tree).get();
        reg.addMetadata(TAG, Boolean.TRUE);
        known.put(reg.getID(), reg);
        String profile = ConfigManager.activeProfile();
        if (currentProfile == null) currentProfile = profile;
        wire(reg);
        dispatcher.accept(() -> {
            try {
                applyProfile(reg, profile);
            } catch (Throwable t) {
                ConfigManager.LOGGER.error("Failed to apply compat snapshot for '{}'", reg.getID(), t);
            }
        });
        return reg;
    }

    @Override
    public void onProfileChanged(String newProfile) {
        String old = currentProfile != null ? currentProfile : ConfigManager.activeProfile();
        currentProfile = newProfile;
        dispatcher.accept(() -> {
            if (old != null && !old.equals(newProfile)) {
                for (Tree tree : known.values()) {
                    try {
                        captureAll(tree, old);
                    } catch (Throwable t) {
                        ConfigManager.LOGGER.error("Failed to capture compat snapshot for '{}'", tree.getID(), t);
                    }
                }
            }
            for (Tree tree : known.values()) {
                try {
                    applyProfile(tree, newProfile);
                } catch (Throwable t) {
                    ConfigManager.LOGGER.error("Failed to apply compat snapshot for '{}'", tree.getID(), t);
                }
            }
        });
    }

    private void applyProfile(Tree tree, String profile) {
        ensureKeys(tree);
        Map<String, Object> snap = store.load(profile).get(tree.getID());
        if (snap == null || snap.isEmpty()) {
            captureAll(tree, profile);
            return;
        }
        boolean[] changed = {false};
        forEachProp(tree, p -> {
            if (!isValueProp(p)) return;
            String key = keyOf(p);
            if (!snap.containsKey(key)) return;
            Object stored = snap.get(key);
            Object value;
            try {
                value = deserialize(stored);
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to deserialize compat value for '{}'", key, t);
                return;
            }
            Object live = p.get();
            if (live != null && value != null && live.getClass() != value.getClass()
                    && !(live instanceof Number && value instanceof Number)) {
                return;
            }
            applying.add(p);
            try {
                p.setAsReferential(value);
                changed[0] = true;
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to apply compat value for '{}'", key, t);
            } finally {
                applying.remove(p);
            }
        });
        if (changed[0]) runSave(tree);
    }

    private void captureAll(Tree tree, String profile) {
        ensureKeys(tree);
        forEachProp(tree, p -> {
            if (!isValueProp(p)) return;
            String key = keyOf(p);
            Object serialized = trySerialize(p.get());
            if (serialized != null) store.putValue(profile, tree.getID(), key, serialized);
        });
        store.flush(profile);
    }

    private void wire(Tree tree) {
        ensureKeys(tree);
        String treeId = tree.getID();
        forEachProp(tree, p -> {
            if (!isValueProp(p)) return;
            if (wired.put(p, Boolean.TRUE) != null) return;
            String key = keyOf(p);
            @SuppressWarnings("unchecked")
            Property<Object> prop = (Property<Object>) p;
            prop.addCallback(value -> {
                if (applying.contains(p)) return false;
                Object serialized = trySerialize(value);
                if (serialized != null) store.putValue(ConfigManager.activeProfile(), treeId, key, serialized);
                return false;
            });
        });
    }

    private void runSave(Tree tree) {
        Object customSave = tree.getMetadata("custom_save");
        if (customSave instanceof Runnable) {
            try {
                ((Runnable) customSave).run();
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("custom_save failed for compat tree '{}'", tree.getID(), t);
            }
        }
    }

    private static Object trySerialize(Object value) {
        try {
            return ObjectSerializer.INSTANCE.serialize(value, false, false);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object deserialize(Object stored) {
        return stored instanceof Map ? ObjectSerializer.INSTANCE.deserialize((Map<String, Object>) stored) : stored;
    }

    private static final String KEY_META = "oc_snapshot_key";
    private static final String ACTION_META = "runnable";
    public static final String NO_SNAPSHOT_META = "oc_no_snapshot";

    private static boolean isValueProp(Property<?> p) {
        return p.getMetadata(ACTION_META) == null && p.getMetadata(NO_SNAPSHOT_META) == null;
    }

    private static void ensureKeys(Tree tree) {
        int[] index = {0};
        forEachProp(tree, p -> {
            int i = index[0]++;
            if (p.getMetadata(KEY_META) == null) {
                Object title = p.getTitle();
                p.addMetadata(KEY_META, i + "|" + (title != null ? title : ""));
            }
        });
    }

    private static String keyOf(Property<?> p) {
        Object key = p.getMetadata(KEY_META);
        return key != null ? key.toString() : p.getID();
    }

    private static void forEachProp(Tree tree, Consumer<Property<?>> action) {
        for (Node node : tree.map.values()) {
            if (node instanceof Tree) forEachProp((Tree) node, action);
            else if (node instanceof Property) action.accept((Property<?>) node);
        }
    }
}
