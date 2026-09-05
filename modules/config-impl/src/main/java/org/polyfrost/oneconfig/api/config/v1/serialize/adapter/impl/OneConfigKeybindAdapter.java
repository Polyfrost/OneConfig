package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.keybind.BindNotInScreen;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.KeybindCodec;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class OneConfigKeybindAdapter extends Adapter<OneConfigKeybind, Map> {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Keybinds");
    private final KeybindCodec codec;

    public OneConfigKeybindAdapter() {
        this.codec = null;
    }

    public OneConfigKeybindAdapter(KeybindCodec codec) {
        this.codec = codec;
    }

    @Override
    public Map<String, Object> serialize(OneConfigKeybind in) {
        Map<String, Object> out = new HashMap<>(4);
        List<Object> keys = merge(in.getKeyCodes() == null ? null : encode(in.getKeyCodes(), true), in.getUnresolvedKeyInputs());
        List<Object> mouse = merge(in.getMouseBtns() == null ? null : encode(in.getMouseBtns(), false), in.getUnresolvedMouseInputs());
        if (keys != null) out.put("keyCodes", keys);
        if (mouse != null) out.put("mouseBtns", mouse);
        if (in.getMods() != 0) out.put("mods", in.getMods());
        if (in.getDurationNanos() != 0L) out.put("durationNanos", in.getDurationNanos());
        return out;
    }

    @Override
    public OneConfigKeybind deserialize(Map in) {
        List<Object> droppedKeys = new ArrayList<>();
        List<Object> droppedMouse = new ArrayList<>();
        int[] keyCodes = decode(in.get("keyCodes"), true, droppedKeys);
        int[] mouseBtns = decode(in.get("mouseBtns"), false, droppedMouse);
        byte mods = ((Number) in.getOrDefault("mods", 0)).byteValue();
        long durationNanos = ((Number) in.getOrDefault("durationNanos", 0L)).longValue();
        OneConfigKeybind out = BindNotInScreen.class.getName().equals(in.get("class"))
                ? new BindNotInScreen(keyCodes, mouseBtns, mods, durationNanos, ignored -> true)
                : new OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, ignored -> true);
        if (!droppedKeys.isEmpty()) out.setUnresolvedKeyInputs(droppedKeys);
        if (!droppedMouse.isEmpty()) out.setUnresolvedMouseInputs(droppedMouse);
        return out;
    }

    private static @Nullable List<Object> merge(@Nullable List<Object> encoded, @Nullable List<?> unresolved) {
        if (unresolved == null || unresolved.isEmpty()) return encoded;
        List<Object> out = encoded == null ? new ArrayList<>(unresolved.size()) : new ArrayList<>(encoded);
        out.addAll(unresolved);
        return out;
    }

    @Override
    public Class<OneConfigKeybind> getTargetClass() {
        return OneConfigKeybind.class;
    }

    @Override
    public Class<Map> getOutputClass() {
        return Map.class;
    }

    private List<Object> encode(int[] values, boolean keyboard) {
        KeybindCodec codec = codec();
        List<Object> out = new ArrayList<>(values.length);
        for (int value : values) {
            if (codec == null) {
                out.add(value);
                continue;
            }
            String name = keyboard ? codec.keyName(value) : codec.mouseName(value);
            if (name != null) out.add(name);
            else LOGGER.warn("Cannot save unsupported {} input {}", keyboard ? "keyboard" : "mouse", value);
        }
        return out;
    }

    private int[] decode(Object value, boolean keyboard, List<Object> dropped) {
        List<?> values = asList(value);
        if (values == null) return null;

        KeybindCodec codec = codec();
        int[] out = new int[values.size()];
        int size = 0;
        for (Object entry : values) {
            Integer code = toCode(entry, keyboard, codec);
            if (code != null) out[size++] = code;
            else {
                LOGGER.warn("Unsupported {} input {} is inactive on this Minecraft version", keyboard ? "keyboard" : "mouse", entry);
                dropped.add(entry);
            }
        }

        if (size == out.length) return out;
        return Arrays.copyOf(out, size);
    }

    private @Nullable Integer toCode(Object entry, boolean keyboard, @Nullable KeybindCodec codec) {
        if (entry instanceof Number) {
            int legacy = ((Number) entry).intValue();
            if (codec == null) return legacy;
            String name = keyboard ? codec.legacyKeyName(legacy) : codec.legacyMouseName(legacy);
            return name == null ? null : (keyboard ? codec.keyCode(name) : codec.mouseButton(name));
        }
        if (entry instanceof String) {
            if (codec == null) return null;
            String name = (String) entry;
            return keyboard ? codec.keyCode(name) : codec.mouseButton(name);
        }
        return null;
    }

    private static @Nullable List<?> asList(Object value) {
        if (value == null) return null;
        if (value instanceof List) return (List<?>) value;
        if (value instanceof Collection) return new ArrayList<>((Collection<?>) value);
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            List<Object> out = new ArrayList<>(len);
            for (int i = 0; i < len; i++) out.add(Array.get(value, i));
            return out;
        }
        LOGGER.warn("Ignoring unsupported keybind value {}", value);
        return null;
    }

    private KeybindCodec codec() {
        return codec != null ? codec : CodecService.INSTANCE;
    }

    private static final class CodecService {
        private static final @Nullable KeybindCodec INSTANCE = load();

        private static @Nullable KeybindCodec load() {
            try {
                Iterator<KeybindCodec> it = ServiceLoader
                    .load(KeybindCodec.class, KeybindCodec.class.getClassLoader()).iterator();
                if (it.hasNext()) return it.next();
            } catch (Throwable t) {
                LOGGER.warn("Failed to load KeybindCodec; keybinds will be stored as raw platform codes", t);
                return null;
            }
            LOGGER.warn("No KeybindCodec found; keybinds will be stored as raw platform codes");
            return null;
        }
    }
}
