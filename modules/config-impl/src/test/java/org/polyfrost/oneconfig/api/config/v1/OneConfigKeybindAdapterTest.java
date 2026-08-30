package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.OneConfigKeybindAdapter;
import org.polyfrost.oneconfig.api.ui.v1.keybind.BindNotInScreen;
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.KeybindCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OneConfigKeybindAdapterTest {
    private final OneConfigKeybindAdapter adapter = new OneConfigKeybindAdapter(new TestCodec());

    @Test
    void writesKeyNames() {
        OneConfigKeybind keybind = new OneConfigKeybind(new int[]{4}, null, (byte) 2, 1L, ignored -> true);

        Map<String, Object> serialized = adapter.serialize(keybind);

        assertEquals(List.of("key.keyboard.a"), serialized.get("keyCodes"));
        assertNull(serialized.get("mouseBtns"));
        assertEquals((byte) 2, serialized.get("mods"));
        assertEquals(1L, serialized.get("durationNanos"));
    }

    @Test
    void readsKeyNames() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("keyCodes", List.of("key.keyboard.a"));
        serialized.put("mouseBtns", List.of("key.mouse.left"));

        OneConfigKeybind keybind = adapter.deserialize(serialized);

        assertArrayEquals(new int[]{4}, keybind.getKeyCodes());
        assertArrayEquals(new int[]{1}, keybind.getMouseBtns());
        assertEquals((byte) 0, keybind.getMods());
        assertEquals(0L, keybind.getDurationNanos());
    }

    @Test
    void migratesGlfwInputs() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("keyCodes", List.of(65));
        serialized.put("mouseBtns", List.of(0));

        OneConfigKeybind keybind = adapter.deserialize(serialized);

        assertArrayEquals(new int[]{4}, keybind.getKeyCodes());
        assertArrayEquals(new int[]{1}, keybind.getMouseBtns());
        assertEquals((byte) 0, keybind.getMods());
        assertEquals(0L, keybind.getDurationNanos());
    }

    @Test
    void readsArraysAsWellAsLists() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("keyCodes", new int[]{65}); // numeric entries are still migrated from the legacy GLFW codes
        serialized.put("mouseBtns", new Object[]{"key.mouse.left"});

        OneConfigKeybind keybind = adapter.deserialize(serialized);

        assertArrayEquals(new int[]{4}, keybind.getKeyCodes());
        assertArrayEquals(new int[]{1}, keybind.getMouseBtns());
    }

    @Test
    void dropsUnsupportedInputsInsteadOfThrowing() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("keyCodes", List.of("key.keyboard.a", "key.keyboard.nonsense", Boolean.TRUE));

        OneConfigKeybind keybind = adapter.deserialize(serialized);

        assertArrayEquals(new int[]{4}, keybind.getKeyCodes());
    }

    @Test
    void roundTripsRawCodesWithoutACodec() {
        OneConfigKeybindAdapter noCodec = new OneConfigKeybindAdapter(); // no KeybindCodec service on this classpath
        OneConfigKeybind keybind = new OneConfigKeybind(new int[]{4}, null, (byte) 0, 0L, ignored -> true);

        Map<String, Object> serialized = noCodec.serialize(keybind);

        assertEquals(List.of(4), serialized.get("keyCodes"));
        assertArrayEquals(new int[]{4}, noCodec.deserialize(serialized).getKeyCodes());
    }

    @Test
    void deserializesBindNotInScreen() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("class", BindNotInScreen.class.getName());
        serialized.put("keyCodes", List.of("key.keyboard.a"));

        assertInstanceOf(BindNotInScreen.class, adapter.deserialize(serialized));
    }

    private static final class TestCodec implements KeybindCodec {
        @Override
        public String keyName(int code) {
            return code == 4 ? "key.keyboard.a" : null;
        }

        @Override
        public Integer keyCode(String name) {
            return "key.keyboard.a".equals(name) ? 4 : null;
        }

        @Override
        public String mouseName(int button) {
            return button == 1 ? "key.mouse.left" : null;
        }

        @Override
        public Integer mouseButton(String name) {
            return "key.mouse.left".equals(name) ? 1 : null;
        }

        @Override
        public String legacyKeyName(int glfwCode) {
            return glfwCode == 65 ? "key.keyboard.a" : null;
        }

        @Override
        public String legacyMouseName(int glfwButton) {
            return glfwButton == 0 ? "key.mouse.left" : null;
        }
    }
}
