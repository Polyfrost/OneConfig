package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.oneconfig.api.notifications.v1.Notifications;
import org.polyfrost.oneconfig.api.ui.v1.keybind.BindNotInScreen;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.KeybindCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
        if (in.getKeyCodes() != null) out.put("keyCodes", encode(in.getKeyCodes(), true));
        if (in.getMouseBtns() != null) out.put("mouseBtns", encode(in.getMouseBtns(), false));
        if (in.getMods() != 0) out.put("mods", in.getMods());
        if (in.getDurationNanos() != 0L) out.put("durationNanos", in.getDurationNanos());
        return out;
    }

    @Override
    public OneConfigKeybind deserialize(Map in) {
        int[] keyCodes = decode(in.get("keyCodes"), true);
        int[] mouseBtns = decode(in.get("mouseBtns"), false);
        byte mods = ((Number) in.getOrDefault("mods", 0)).byteValue();
        long durationNanos = ((Number) in.getOrDefault("durationNanos", 0L)).longValue();
        if (BindNotInScreen.class.getName().equals(in.get("class"))) {
            return new BindNotInScreen(keyCodes, mouseBtns, mods, durationNanos, ignored -> true);
        }
        return new OneConfigKeybind(keyCodes, mouseBtns, mods, durationNanos, ignored -> true);
    }

    @Override
    public Class<OneConfigKeybind> getTargetClass() {
        return OneConfigKeybind.class;
    }

    @Override
    public Class<Map> getOutputClass() {
        return Map.class;
    }

    private List<String> encode(int[] values, boolean keyboard) {
        List<String> out = new ArrayList<>(values.length);
        for (int value : values) {
            String name = keyboard ? codec().keyName(value) : codec().mouseName(value);
            if (name != null) out.add(name);
            else LOGGER.warn("Cannot save unsupported {} input {}", keyboard ? "keyboard" : "mouse", value);
        }
        return out;
    }

    private int[] decode(Object value, boolean keyboard) {
        if (value == null) return null;
        List<?> values = (List<?>) value;

        int[] out = new int[values.size()];
        int size = 0;
        for (Object entry : values) {
            String name;
            if (entry instanceof String) {
                name = (String) entry;
            } else if (entry instanceof Number) {
                int glfwCode = ((Number) entry).intValue();
                name = keyboard ? codec().legacyKeyName(glfwCode) : codec().legacyMouseName(glfwCode);
            } else {
                throw new IllegalArgumentException("Unsupported keybind value: " + entry);
            }
            Integer code = name == null ? null : (keyboard ? codec().keyCode(name) : codec().mouseButton(name));
            if (code != null) out[size++] = code;
            else {
                LOGGER.warn("Ignoring unsupported {} input {}", keyboard ? "keyboard" : "mouse", entry);
                try {
                    Notifications.error(
                        "Unsupported keybind",
                        (keyboard ? "Keyboard key" : "Mouse button") + " '" + entry
                            + "' is not supported by this Minecraft version and was unbound."
                    );
                } catch (Throwable t) {
                    LOGGER.error("Failed to notify about unsupported keybind input {}", entry, t);
                }
            }
        }

        if (size == out.length) return out;
        return Arrays.copyOf(out, size);
    }

    private KeybindCodec codec() {
        return codec != null ? codec : CodecService.INSTANCE;
    }

    private static final class CodecService {
        private static final KeybindCodec INSTANCE = ServiceLoader.load(KeybindCodec.class, KeybindCodec.class.getClassLoader())
            .iterator().next();
    }
}
