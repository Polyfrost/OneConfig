package org.polyfrost.oneconfig.api.ui.v1.keybind.internal;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
//? if >=1.21.10 {
//~if >= 1.21.11 'ResourceLocation;' -> 'Identifier;'
import net.minecraft.resources.Identifier;
//?}
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers;
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindManager;
import org.polyfrost.oneconfig.api.ui.v1.keybind.MinecraftKeybindBridge;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
//? if > 1.8.9 {
import org.polyfrost.oneconfig.internal.mixin.keybind.KeyMappingAccessor;
//?} else
//import org.polyfrost.oneconfig.internal.legacy.KeyCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
//? if > 1.8.9 {
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
//?}

/**
 * Mirrors {@link OneConfigKeybind}s into Minecraft's native Controls menu
 */
public final class MinecraftKeybindBridgeImpl implements MinecraftKeybindBridge {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/Keybinds");
    private static volatile MinecraftKeybindBridgeImpl instance;

    //? if >=1.21.10 {
    private final Map<String, KeyMapping.Category> categories = new HashMap<>();
    //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
    private static final Map<Identifier, String> CATEGORY_LABELS = new ConcurrentHashMap<>();
    private final Set<String> usedPaths = new HashSet<>();

    private KeyMapping.Category categoryFor(String name) {
        String label = (name == null || name.isBlank()) ? "OneConfig" : name;
        return categories.computeIfAbsent(label, n -> {
            //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
            Identifier id = Identifier.fromNamespaceAndPath("oneconfig", uniquePath(n));
            KeyMapping.Category category = KeyMapping.Category.register(id);
            CATEGORY_LABELS.put(id, n);
            return category;
        });
    }

    private String uniquePath(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
        if (base.isEmpty()) base = "keybinds";
        String path = base;
        for (int i = 2; !usedPaths.add(path); i++) path = base + "_" + i;
        return path;
    }

    //~if >= 1.21.11 'ResourceLocation' -> 'Identifier'
    public static String categoryLabel(Identifier id) {
        return CATEGORY_LABELS.get(id);
    }
    //?} else {
    /*private String categoryFor(String name) {
        String label = (name == null || name.isBlank()) ? "OneConfig" : name;
        //? if > 1.8.9 {
        java.util.Map<String, Integer> order = org.polyfrost.oneconfig.internal.mixin.keybind.KeyMappingCategoryAccessor.oneconfig$categorySortOrder();
        if (!order.containsKey(label)) order.put(label, order.size());
        //?} else
        //KeyMapping.getCategories().add(label);
        return label;
    }
    *///?}

    private final Map<OneConfigKeybind, KeyMapping> mappings = new LinkedHashMap<>();
    private final Map<KeyMapping, OneConfigKeybind> reverse = new LinkedHashMap<>();
    private final Set<KeyMapping> spliced = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    public MinecraftKeybindBridgeImpl() {
        instance = this;
        EventManager.register(ScreenOpenEvent.class, e -> reconcile());
    }

    public static MinecraftKeybindBridgeImpl instance() {
        return instance;
    }

    @Override
    public synchronized void register(OneConfigKeybind bind) {
        if (bind.getName() == null) return;
        KeyMapping existing = mappings.get(bind);
        if (existing != null) {
            applyKeyTo(existing, bind);
            return;
        }
        String id = identity(bind);
        mappings.entrySet().removeIf(e -> {
            if (identity(e.getKey()).equals(id)) {
                reverse.remove(e.getValue());
                return true;
            }
            return false;
        });
        OneConfigKeybind def = bind.getDefaultKeybind();
        OneConfigKeybind defSrc = def != null ? def : bind;
        InputConstants.Type type = defSrc.isMousePrimary() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        KeyMapping mapping = detached(
            bind.getName(),
            type.getOrCreate(defSrc.getBoundCode()),
            //? if > 1.8.9 {
            () -> new KeyMapping(bind.getName(), type, defSrc.getBoundCode(), categoryFor(bind.getCategory()))
            //?} else
            //() -> new KeyMapping(bind.getName(), KeyCodes.toLegacy(type.getOrCreate(defSrc.getBoundCode())), categoryFor(bind.getCategory()))
        );
        applyKeyTo(mapping, bind);
        mappings.put(bind, mapping);
        reverse.put(mapping, bind);
        syncOptionsArray();
    }

    private synchronized void syncOptionsArray() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        KeyMapping[] current = mc.options.keyMappings;
        if (current == null) return;
        List<KeyMapping> rebuilt = new ArrayList<>(current.length + mappings.size());
        for (KeyMapping km : current) {
            if (!spliced.contains(km)) rebuilt.add(km);
        }
        rebuilt.addAll(mappings.values());
        spliced.clear();
        spliced.addAll(mappings.values());
        ((org.polyfrost.oneconfig.internal.mixin.keybind.OptionsAccessor) (Object) mc.options)
            .oneconfig$setKeyMappings(rebuilt.toArray(new KeyMapping[0]));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static KeyMapping detached(String name, InputConstants.Key defaultKey, java.util.function.Supplier<KeyMapping> constructor) {
        //? if > 1.8.9 {
        Map all;
        Map map;
        Object priorNamed;
        Object priorBound;
        try {
            all = org.polyfrost.oneconfig.internal.mixin.keybind.KeyMappingRegistryAccessor.oneconfig$all();
            map = org.polyfrost.oneconfig.internal.mixin.keybind.KeyMappingRegistryAccessor.oneconfig$map();
            priorNamed = all.get(name);
            priorBound = map.get(defaultKey);
        } catch (Throwable t) {
            LOGGER.warn("Cannot reach Minecraft's keybind registries; OneConfig keybinds may shadow Minecraft ones", t);
            return constructor.get();
        }

        KeyMapping mapping = constructor.get();
        try {
            if (all.get(name) == mapping) {
                if (priorNamed != null) all.put(name, priorNamed);
                else all.remove(name);
            }
            Object bound = map.get(defaultKey);
            if (bound == mapping) {
                if (priorBound != null) map.put(defaultKey, priorBound);
                else map.remove(defaultKey);
            } else if (bound instanceof List) {
                ((List) bound).remove(mapping);
                if (((List) bound).isEmpty()) map.remove(defaultKey);
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to detach the mirror of '{}' from Minecraft's keybind registries", name, t);
        }
        return mapping;
        //?} else {
        /*List all;
        try {
            all = org.polyfrost.oneconfig.internal.mixin.keybind.KeyMappingRegistryAccessor.oneconfig$all();
        } catch (Throwable t) {
            LOGGER.warn("Cannot reach Minecraft's keybind registries; OneConfig keybinds may shadow Minecraft ones", t);
            return constructor.get();
        }
        KeyMapping mapping = constructor.get();
        try {
            all.remove(mapping);
            KeyMapping.resetMapping();
        } catch (Throwable t) {
            LOGGER.warn("Failed to detach the mirror of '{}' from Minecraft's keybind registries", name, t);
        }
        return mapping;
        *///?}
    }

    private static String identity(OneConfigKeybind bind) {
        return bind.getCategory() + "\u0000" + bind.getName();
    }

    @Override
    public synchronized void unregister(OneConfigKeybind bind) {
        KeyMapping mapping = mappings.remove(bind);
        if (mapping != null) reverse.remove(mapping);
        syncOptionsArray();
    }

    @Override
    public synchronized void sync(OneConfigKeybind bind) {
        KeyMapping mapping = mappings.get(bind);
        if (mapping != null) applyKeyTo(mapping, bind);
    }

    public synchronized KeyMapping[] entries() {
        return mappings.values().toArray(new KeyMapping[0]);
    }

    public synchronized OneConfigKeybind bindFor(KeyMapping mapping) {
        return reverse.get(mapping);
    }

    private volatile KeyMapping activeRebind;

    public void setActiveRebind(KeyMapping mapping) {
        this.activeRebind = mapping;
    }

    public KeyMapping getActiveRebind() {
        return activeRebind;
    }

    public synchronized boolean isComboMapping(KeyMapping mapping) {
        OneConfigKeybind bind = reverse.get(mapping);
        return bind != null && comboLabel(bind, Component.empty()) != null;
    }

    public synchronized Component comboTextFor(KeyMapping mapping) {
        OneConfigKeybind bind = reverse.get(mapping);
        if (bind == null || comboLabel(bind, Component.empty()) == null) return null;
        return Component.literal(fullComboText(bind.getKeyCodes(), bind.getMouseBtns(), bind.getMods()));
    }

    private static final long MOUSE_TAG = 1L << 40;
    private static final long MODS_TAG = 2L << 40;

    public synchronized boolean menuConflict(KeyMapping self, KeyMapping other) {
        if (self == other) return false;
        long[] a = combo(self);
        long[] b = combo(other);
        return a != null && b != null && Arrays.equals(a, b);
    }

    private long[] combo(KeyMapping mapping) {
        java.util.TreeSet<Long> set = new java.util.TreeSet<>();
        OneConfigKeybind bind = reverse.get(mapping);
        if (bind != null) {
            int[] keys = bind.getKeyCodes();
            int[] mouse = bind.getMouseBtns();
            if ((keys == null || keys.length == 0) && (mouse == null || mouse.length == 0)) return null;
            if (keys != null) for (int k : keys) set.add((long) k);
            if (mouse != null) for (int m : mouse) set.add(MOUSE_TAG | m);
            set.add(MODS_TAG | (bind.getMods() & 0xFFL));
        } else {
            //? if > 1.8.9 {
            InputConstants.Key key = ((KeyMappingAccessor) mapping).oneconfig$getKey();
            //?} else
            //InputConstants.Key key = KeyCodes.fromLegacy(mapping.getKeyCode());
            int v = key.getValue();
            if (v < 0) return null;
            set.add(key.getType() == InputConstants.Type.MOUSE ? (MOUSE_TAG | v) : (long) v);
            set.add(MODS_TAG);
        }
        long[] out = new long[set.size()];
        int i = 0;
        for (long l : set) out[i++] = l;
        return out;
    }

    private KeyMapping previewMapping = null;
    private String previewText = null;
    private byte splitMods = KeyModifiers.NONE;

    public synchronized boolean menuRebind(KeyMapping mapping, int[] rawKeys, int[] mouse, boolean persist) {
        OneConfigKeybind bind = reverse.get(mapping);
        if (bind == null) return false;
        int[] keys = splitKeys(rawKeys, mouse != null && mouse.length > 0);
        bind.setKeyCodes(keys);
        bind.setMouseBtns(mouse);
        bind.setMods(splitMods);
        applyKeyTo(mapping, bind);
        if (persist) KeybindManager.notifyMenuEdit(bind);
        return true;
    }

    public synchronized void setPreview(KeyMapping mapping, int[] rawKeys, int[] mouse) {
        if (reverse.get(mapping) == null) return;
        int[] keys = splitKeys(rawKeys, mouse != null && mouse.length > 0);
        previewMapping = mapping;
        previewText = fullComboText(keys, mouse, splitMods);
    }

    public synchronized void clearPreview() {
        previewMapping = null;
        previewText = null;
    }

    public synchronized Component previewFor(KeyMapping mapping) {
        return mapping == previewMapping && previewText != null ? Component.literal(previewText) : null;
    }

    private int[] splitKeys(int[] rawKeys, boolean haveMouse) {
        byte mods = KeyModifiers.NONE;
        List<Integer> keys = new ArrayList<>();
        if (rawKeys != null) {
            for (int k : rawKeys) {
                byte bit = modBit(k);
                if (bit != KeyModifiers.NONE) mods |= bit;
                else keys.add(k);
            }
        }
        int[] keyArr = keys.isEmpty() ? null : toArray(keys);
        if (keyArr == null && !haveMouse && rawKeys != null && rawKeys.length > 0) {
            keyArr = rawKeys.clone();
            mods = KeyModifiers.NONE;
        }
        splitMods = mods;
        return keyArr;
    }

    private static int[] toArray(List<Integer> list) {
        int[] out = new int[list.size()];
        for (int i = 0; i < out.length; i++) out[i] = list.get(i);
        return out;
    }

    private static byte modBit(int glfw) {
        return switch (glfw) {
            case 340, 344 -> KeyModifiers.SHIFT;
            case 341, 345 -> KeyModifiers.CTRL;
            case 342, 346 -> KeyModifiers.ALT;
            case 343, 347 -> KeyModifiers.META;
            default -> KeyModifiers.NONE;
        };
    }

    public static String fullComboText(int[] keys, int[] mouse, byte mods) {
        List<String> parts = new ArrayList<>();
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.CTRL)) parts.add("Ctrl");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.SHIFT)) parts.add("Shift");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.ALT)) parts.add("Alt");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.META)) parts.add("Meta");
        if (keys != null) {
            for (int k : keys) parts.add(InputConstants.Type.KEYSYM.getOrCreate(k).getDisplayName().getString());
        }
        if (mouse != null) {
            for (int b : mouse) parts.add("Mouse " + (b + 1));
        }
        return parts.isEmpty() ? "None" : String.join(" + ", parts);
    }

    public synchronized boolean menuUnbind(KeyMapping mapping) {
        OneConfigKeybind bind = reverse.get(mapping);
        if (bind == null) return false;
        bind.setKeyCodes(null);
        bind.setMouseBtns(null);
        bind.setMods(KeyModifiers.NONE);
        setKey(mapping, InputConstants.UNKNOWN);
        KeybindManager.notifyMenuEdit(bind);
        return true;
    }

    public synchronized boolean menuReset(KeyMapping mapping) {
        OneConfigKeybind bind = reverse.get(mapping);
        if (bind == null) return false;
        OneConfigKeybind def = bind.getDefaultKeybind();
        if (def == null) return false;
        bind.setKeyCodes(def.getKeyCodes() == null ? null : def.getKeyCodes().clone());
        bind.setMouseBtns(def.getMouseBtns() == null ? null : def.getMouseBtns().clone());
        bind.setMods(def.getMods());
        applyKeyTo(mapping, bind);
        KeybindManager.notifyMenuEdit(bind);
        return true;
    }

    private boolean internalSetKey = false;

    public boolean isInternalSetKey() {
        return internalSetKey;
    }

    private void setKey(KeyMapping mapping, InputConstants.Key key) {
        internalSetKey = true;
        try {
            mapping.setKey(key);
            //? if = 1.8.9
            //KeyMapping.resetMapping();
        } finally {
            internalSetKey = false;
        }
    }

    private void applyKeyTo(KeyMapping mapping, OneConfigKeybind bind) {
        setKey(mapping, (bind.isMousePrimary() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM).getOrCreate(bind.getBoundCode()));
    }

    public void reconcile() {
        syncOptionsArray();
        List<Runnable> pending = new ArrayList<>();
        synchronized (this) {
            for (Map.Entry<OneConfigKeybind, KeyMapping> e : mappings.entrySet()) {
                OneConfigKeybind bind = e.getKey();
                //? if > 1.8.9 {
                InputConstants.Key actual = ((KeyMappingAccessor) e.getValue()).oneconfig$getKey();
                //?} else
                //InputConstants.Key actual = KeyCodes.fromLegacy(e.getValue().getKeyCode());
                int actualValue = actual.getValue();
                boolean actualMouse = actual.getType() == InputConstants.Type.MOUSE;

                if (actualValue == bind.getBoundCode() && actualMouse == bind.isMousePrimary()) continue;

                OneConfigKeybind def = bind.getDefaultKeybind();
                if (def != null && actualValue == def.getBoundCode() && actualMouse == def.isMousePrimary()) {
                    pending.add(() -> KeybindManager.rebindFromMinecraft(bind, def.getKeyCodes(), def.getMouseBtns(), def.getMods()));
                    continue;
                }

                int[] keys = (!actualMouse && actualValue >= 0) ? new int[]{actualValue} : null;
                int[] mouse = actualMouse ? new int[]{actualValue} : null;
                pending.add(() -> KeybindManager.rebindFromMinecraft(bind, keys, mouse, KeyModifiers.NONE));
            }
        }
        for (Runnable r : pending) r.run();
    }

    public static Component comboLabel(OneConfigKeybind bind, Component base) {
        StringBuilder prefix = new StringBuilder();
        byte mods = bind.getMods();
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.CTRL)) prefix.append("Ctrl + ");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.SHIFT)) prefix.append("Shift + ");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.ALT)) prefix.append("Alt + ");
        if (KeyModifiers.INSTANCE.has(mods, KeyModifiers.META)) prefix.append("Meta + ");

        int[] keys = bind.getKeyCodes();
        int[] mouse = bind.getMouseBtns();
        boolean keysPrimary = keys != null && keys.length > 0;

        List<String> extra = new ArrayList<>();
        if (keys != null) {
            for (int i = keysPrimary ? 1 : 0; i < keys.length; i++) {
                extra.add(InputConstants.Type.KEYSYM.getOrCreate(keys[i]).getDisplayName().getString());
            }
        }
        if (mouse != null) {
            for (int i = keysPrimary ? 0 : 1; i < mouse.length; i++) {
                extra.add("Mouse " + (mouse[i] + 1));
            }
        }

        if (prefix.isEmpty() && extra.isEmpty()) return null;
        String suffix = extra.isEmpty() ? "" : " + " + String.join(" + ", extra);
        return Component.literal(prefix + base.getString() + suffix);
    }
}
