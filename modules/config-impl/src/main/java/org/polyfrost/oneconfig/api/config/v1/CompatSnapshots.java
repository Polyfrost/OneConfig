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

    public static final String SNAPSHOT_METADATA = "oc_compat_snapshot";
    public static final String CUSTOM_RESET_METADATA = "custom_reset";
    private static final String TAG = SNAPSHOT_METADATA;

    private final CompatSnapshotStore store = new CompatSnapshotStore();
    private final CompatSnapshotStore baselineStore = new CompatSnapshotStore("compat-baseline.json");
    private static final String BASELINE_BUCKET = "";
    private static final long DISPATCH_TIMEOUT_SECONDS = 30L;
    private final Map<String, Tree> known = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> defaults = new ConcurrentHashMap<>();
    private final Map<Property<?>, Boolean> wired = Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Property<?>> applying = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static final ThreadLocal<Boolean> APPLYING_HERE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private volatile java.util.function.Consumer<Runnable> dispatcher = Runnable::run;
    private volatile String currentProfile;

    private CompatSnapshots() {
    }

    public static boolean isApplying() {
        return APPLYING_HERE.get();
    }

    public static void setDispatcher(java.util.function.Consumer<Runnable> dispatcher) {
        INSTANCE.dispatcher = dispatcher == null ? Runnable::run : dispatcher;
    }

    public static Tree register(Tree tree) {
        return INSTANCE.register0(tree);
    }

    public static Tree track(Tree registered) {
        return INSTANCE.track0(registered);
    }

    private Tree register0(Tree tree) {
        tree.addMetadata(Backend.UI_ONLY_METADATA, Boolean.TRUE);
        dropStaleRegistration(tree.getID());
        return track0(ConfigManager.active().register(tree).get());
    }

    private Tree track0(Tree reg) {
        reg.addMetadata(TAG, Boolean.TRUE);
        if (reg.getMetadata("custom_save") != null) {
            reg.addMetadata(Backend.CUSTOM_SAVE_TRACKED_METADATA, Boolean.TRUE);
        }
        known.put(reg.getID(), reg);
        String profile = ConfigManager.activeProfile();
        if (currentProfile == null) currentProfile = profile;
        captureDefaults(reg);
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

    private void dropStaleRegistration(String id) {
        if (id == null) return;
        if (!ConfigManager.active().exists(id)) return;
        Tree existing = ConfigManager.active().get(id);
        if (existing == null || !Boolean.TRUE.equals(existing.getMetadata(TAG))) return;
        ConfigManager.active().unregister(id);
        known.remove(id, existing);
    }

    @Override
    public void onProfileChanged(String newProfile) {
        String old = currentProfile != null ? currentProfile : ConfigManager.activeProfile();
        currentProfile = newProfile;
        if (newProfile.equals(old)) return;
        dispatcher.accept(() -> {
            for (Tree tree : known.values()) {
                try {
                    applyProfile(tree, newProfile);
                } catch (Throwable t) {
                    ConfigManager.LOGGER.error("Failed to apply compat snapshot for '{}'", tree.getID(), t);
                }
            }
        });
    }

    @Override
    public void onProfileSaving(String profile) {
        if (store.hasLoadFailure(profile)) {
            ConfigManager.LOGGER.warn(
                    "Continuing the profile operation without rewriting unreadable compat snapshot '{}'", profile
            );
            return;
        }
        if (!profile.equals(currentProfile)) {
            flushForLifecycle(store, profile);
            return;
        }
        dispatchAndWait(() -> {
            for (Tree tree : known.values()) {
                captureAll(tree, profile);
            }
        });
        flushSnapshotThenBaseline(store, profile, baselineStore, BASELINE_BUCKET);
    }

    @Override
    public void onProfileCreated(String profile) {
        store.deleteProfile(profile);
        currentProfile = profile;
        dispatchAndWait(() -> {
            for (Tree tree : known.values()) {
                try {
                    restoreDefaults(tree);
                    captureAll(tree, profile);
                } catch (Throwable t) {
                    ConfigManager.LOGGER.error("Failed to initialize compat defaults for '{}'", tree.getID(), t);
                }
            }
        });
        flushSnapshotThenBaseline(store, profile, baselineStore, BASELINE_BUCKET);
    }

    @Override
    public void onProfileRenamed(String oldProfile, String newProfile) {
        if (oldProfile.equals(currentProfile)) currentProfile = newProfile;
        try {
            store.renameProfile(oldProfile, newProfile);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to move compat snapshot to profile '" + newProfile + "'", e);
        }
    }

    @Override
    public void onProfileDeleted(String profile) {
        store.deleteProfile(profile);
        if (profile.equals(currentProfile)) {
            currentProfile = "";
            dispatchAndWait(() -> {
                for (Tree tree : known.values()) {
                    applyProfile(tree, "");
                }
            });
        }
    }

    private void dispatchAndWait(Runnable action) {
        ConfigManager.dispatchAndWait(
                dispatcher,
                action,
                java.util.concurrent.TimeUnit.SECONDS.toNanos(DISPATCH_TIMEOUT_SECONDS),
                "the compat snapshot dispatcher"
        );
    }

    private static void flushForLifecycle(CompatSnapshotStore snapshotStore, String profile) {
        try {
            snapshotStore.flushOrThrow(profile);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to save compat snapshot for profile '" + profile + "'", e);
        }
    }

    static void flushSnapshotThenBaseline(
            CompatSnapshotStore snapshotStore,
            String profile,
            CompatSnapshotStore baselineStore,
            String baselineProfile
    ) {
        flushForLifecycle(snapshotStore, profile);
        flushForLifecycle(baselineStore, baselineProfile);
    }

    private void applyProfile(Tree tree, String profile) {
        if (gateClosed(tree)) return;
        captureDefaults(tree);
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
                ConfigManager.LOGGER.warn("Failed to deserialize compat value for '{}', re-snapshotting from live value", key, t);
                if (liveSer != null) {
                    store.putValue(profile, treeId, key, liveSer);
                    setBaseline(treeId, key, liveSer);
                }
                return;
            }
            Object live = p.get();
            if (live != null && value != null && live.getClass() != value.getClass()
                    && !(live instanceof Number && value instanceof Number)) {
                return;
            }
            applying.add(p);
            APPLYING_HERE.set(Boolean.TRUE);
            try {
                p.setAsReferential(value);
                setBaseline(treeId, key, stored);
                changed[0] = true;
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to apply compat value for '{}'", key, t);
            } finally {
                APPLYING_HERE.remove();
                applying.remove(p);
            }
        });
        // Persist the profile snapshot before its baseline. If the first write fails, keeping an
        // older baseline is safe: the next load treats the live value as an external change and
        // repairs the snapshot. The opposite order could make a stale snapshot look current and
        // roll a setting back after a restart.
        flushSnapshotThenBaseline(store, profile, baselineStore, BASELINE_BUCKET);
        if (changed[0]) runSave(tree);
    }

    public static void capture(Tree tree) {
        if (tree == null || !Boolean.TRUE.equals(tree.getMetadata(TAG))) return;
        INSTANCE.captureAll(tree, ConfigManager.activeProfile());
    }

    private void captureAll(Tree tree, String profile) {
        if (gateClosed(tree)) return;
        captureDefaults(tree);
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
    }

    private void captureDefaults(Tree tree) {
        if (gateClosed(tree)) return;
        ensureKeys(tree);
        Map<String, Object> snapshot = defaults.computeIfAbsent(tree.getID(), ignored -> new ConcurrentHashMap<>());
        forEachProp(tree, property -> {
            if (!isValueProp(property)) return;
            Object defaultValue = property.getMetadata("default");
            Object serialized = trySerialize(defaultValue != null ? defaultValue : property.get());
            if (serialized != null) snapshot.putIfAbsent(keyOf(property), serialized);
        });
    }

    private void restoreDefaults(Tree tree) {
        if (gateClosed(tree)) return;
        if (runCustomReset(tree)) return;
        Map<String, Object> snapshot = defaults.get(tree.getID());
        if (snapshot == null) return;
        boolean[] changed = {false};
        forEachProp(tree, property -> {
            Object stored = snapshot.get(keyOf(property));
            if (stored == null) return;
            Object value;
            try {
                value = deserialize(stored);
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to deserialize compat default for '{}'", keyOf(property), t);
                return;
            }
            Object live = property.get();
            if (live != null && value != null && live.getClass() != value.getClass()
                    && !(live instanceof Number && value instanceof Number)) {
                return;
            }
            applying.add(property);
            APPLYING_HERE.set(Boolean.TRUE);
            try {
                property.setAsReferential(value);
                changed[0] = true;
            } catch (Throwable t) {
                ConfigManager.LOGGER.warn("Failed to apply compat default for '{}'", keyOf(property), t);
            } finally {
                APPLYING_HERE.remove();
                applying.remove(property);
            }
        });
        if (changed[0]) runSave(tree);
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

    private boolean runCustomReset(Tree tree) {
        Object customReset = tree.getMetadata(CUSTOM_RESET_METADATA);
        if (!(customReset instanceof Runnable)) return false;
        try {
            ((Runnable) customReset).run();
        } catch (Throwable t) {
            ConfigManager.LOGGER.warn("custom_reset failed for compat tree '{}'", tree.getID(), t);
            return false;
        }
        runSave(tree);
        return true;
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
        // Baselines are deliberately not scheduled independently. They are only made durable
        // after the corresponding profile snapshot has been flushed successfully.
        baselineStore.putValueWithoutScheduling(BASELINE_BUCKET, treeId, key, serialized);
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
            Object serialized = ObjectSerializer.INSTANCE.serialize(value, true, true);
            return isStorable(serialized) ? serialized : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isStorable(Object value) {
        if (value == null || value instanceof CharSequence || value instanceof Number
                || value instanceof Boolean || value instanceof Enum) return true;
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                if (!isStorable(o)) return false;
            }
            return true;
        }
        if (value.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (!isStorable(java.lang.reflect.Array.get(value, i))) return false;
            }
            return true;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                if (!(e.getKey() instanceof String) || !isStorable(e.getValue())) return false;
            }
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object deserialize(Object stored) {
        return stored instanceof Map ? ObjectSerializer.INSTANCE.deserialize((Map<String, Object>) stored) : stored;
    }

    public static final String KEY_METADATA = "oc_snapshot_key";
    private static final String ACTION_META = "runnable";
    public static final String NO_SNAPSHOT_META = "oc_no_snapshot";
    public static final String GATE_METADATA = "oc_snapshot_gate";

    private static boolean gateClosed(Tree tree) {
        Object gate = tree.getMetadata(GATE_METADATA);
        return gate instanceof java.util.function.BooleanSupplier
                && !((java.util.function.BooleanSupplier) gate).getAsBoolean();
    }

    private static boolean isValueProp(Property<?> p) {
        return p.getMetadata(ACTION_META) == null && p.getMetadata(NO_SNAPSHOT_META) == null;
    }

    private static void ensureKeys(Tree tree) {
        int[] index = {0};
        forEachProp(tree, p -> {
            int i = index[0]++;
            if (p.getMetadata(KEY_METADATA) == null) {
                Object title = p.getTitle();
                p.addMetadata(KEY_METADATA, i + "|" + (title != null ? title : ""));
            }
        });
    }

    private static String keyOf(Property<?> p) {
        Object key = p.getMetadata(KEY_METADATA);
        return key != null ? key.toString() : p.getID();
    }

    private static void forEachProp(Tree tree, Consumer<Property<?>> action) {
        for (Node node : tree.map.values()) {
            if (node instanceof Tree) forEachProp((Tree) node, action);
            else if (node instanceof Property) action.accept((Property<?>) node);
        }
    }
}
