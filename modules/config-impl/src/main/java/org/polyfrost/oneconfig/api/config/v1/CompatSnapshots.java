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
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
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
    private final CompatSnapshotStore baselineStore = new CompatSnapshotStore("compat-baseline.json");
    private static final String BASELINE_BUCKET = "";
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
        tree.addMetadata(Backend.UI_ONLY_METADATA, Boolean.TRUE);
        Tree reg = ConfigManager.active().register(tree).get();
        reg.addMetadata(TAG, Boolean.TRUE);
        if (reg.getMetadata("custom_save") != null) {
            reg.addMetadata(Backend.CUSTOM_SAVE_TRACKED_METADATA, Boolean.TRUE);
        }
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
        String treeId = tree.getID();
        Map<String, Object> snap = store.load(profile).get(treeId);
        boolean[] changed = {false};
        forEachProp(tree, p -> {
            if (!isValueProp(p)) return;
            String key = keyOf(p);
            Object liveSer = trySerialize(p.get());
            Object baseline = getBaseline(treeId, key);

            if (baseline != null && liveSer != null && !valuesEqual(liveSer, baseline)) {
                store.putValue(profile, treeId, key, liveSer);
                setBaseline(treeId, key, liveSer);
                return;
            }

            Object stored = snap == null ? null : snap.get(key);
            if (stored == null) {
                if (liveSer != null) {
                    store.putValue(profile, treeId, key, liveSer);
                    setBaseline(treeId, key, liveSer);
                }
                return;
            }

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
                setBaseline(treeId, key, stored);
                changed[0] = true;
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to apply compat value for '{}'", key, t);
            } finally {
                applying.remove(p);
            }
        });
        store.flush(profile);
        baselineStore.flush(BASELINE_BUCKET);
        if (changed[0]) runSave(tree);
    }

    private void captureAll(Tree tree, String profile) {
        ensureKeys(tree);
        String treeId = tree.getID();
        forEachProp(tree, p -> {
            if (!isValueProp(p)) return;
            String key = keyOf(p);
            Object serialized = trySerialize(p.get());
            if (serialized != null) {
                store.putValue(profile, treeId, key, serialized);
                setBaseline(treeId, key, serialized);
            }
        });
        store.flush(profile);
        baselineStore.flush(BASELINE_BUCKET);
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
                tree.addMetadata(Backend.CUSTOM_SAVE_DIRTY_METADATA, Boolean.TRUE);
                Object serialized = trySerialize(value);
                if (serialized != null) {
                    store.putValue(ConfigManager.activeProfile(), treeId, key, serialized);
                    setBaseline(treeId, key, serialized);
                }
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

    private Object getBaseline(String treeId, String key) {
        return baselineStore.getValue(BASELINE_BUCKET, treeId, key);
    }

    private void setBaseline(String treeId, String key, Object serialized) {
        baselineStore.putValue(BASELINE_BUCKET, treeId, key, serialized);
    }

    @SuppressWarnings("unchecked")
    private static boolean valuesEqual(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> ma = (Map<String, Object>) a, mb = (Map<String, Object>) b;
            if (ma.size() != mb.size()) return false;
            for (Map.Entry<String, Object> e : ma.entrySet()) {
                if (!mb.containsKey(e.getKey())) return false;
                if (!valuesEqual(e.getValue(), mb.get(e.getKey()))) return false;
            }
            return true;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> la = (List<Object>) a, lb = (List<Object>) b;
            if (la.size() != lb.size()) return false;
            for (int i = 0; i < la.size(); i++) {
                if (!valuesEqual(la.get(i), lb.get(i))) return false;
            }
            return true;
        }
        return a.equals(b);
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
