package org.polyfrost.oneconfig.api.ui.v1.keybind.internal;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinecraftKeybindCodecTest {
    private final MinecraftKeybindCodec codec = new MinecraftKeybindCodec();

    @Test
    void convertsKeys() {
        assertNull(codec.keyName(1));
        assertEquals(Integer.valueOf(InputConstants.KEY_1), codec.keyCode("key.keyboard.1"));
        assertEquals(InputConstants.KEY_1, InputConstants.getKey("key.keyboard.1").getValue());
        assertEquals("key.keyboard.a", codec.keyName(InputConstants.KEY_A));
        assertEquals(Integer.valueOf(InputConstants.KEY_A), codec.keyCode("key.keyboard.a"));
    }

    @Test
    void convertsMouseButtons() {
        String last = codec.mouseName(InputConstants.MOUSE_BUTTON_LEFT + 7);
        assertEquals(InputConstants.UNKNOWN, MinecraftKeybindCodec.mouse(InputConstants.MOUSE_BUTTON_LEFT + 8));
        assertEquals(Integer.valueOf(InputConstants.MOUSE_BUTTON_LEFT + 7), codec.mouseButton(last));
        assertEquals("key.mouse.left", codec.mouseName(InputConstants.MOUSE_BUTTON_LEFT));
        assertEquals(Integer.valueOf(InputConstants.MOUSE_BUTTON_LEFT), codec.mouseButton("key.mouse.left"));
    }

    @Test
    void migratesGlfwInputs() {
        assertEquals("key.keyboard.a", codec.legacyKeyName(65));
        assertEquals("key.keyboard.keypad.period", codec.legacyKeyName(330));
        assertEquals("key.keyboard.application", codec.legacyKeyName(348));
        assertEquals("key.mouse.left", codec.legacyMouseName(0));
        assertEquals("key.mouse.right", codec.legacyMouseName(1));
    }

    @Test
    void migratesAliases() {
        //? if >= 26.3 {
        /*assertEquals("key.keyboard.keypad.period", codec.keyName(99));
        assertEquals("key.keyboard.application", codec.keyName(101));
        assertEquals("key.keyboard.menu", codec.keyName(118));
        assertEquals("key.keyboard.keypad.decimal", codec.keyName(220));
        assertEquals(Integer.valueOf(99), codec.keyCode("key.keyboard.keypad.period"));
        assertEquals(Integer.valueOf(101), codec.keyCode("key.keyboard.application"));
        *///?} else {
        assertEquals("key.keyboard.keypad.period", codec.keyName(330));
        assertEquals("key.keyboard.application", codec.keyName(348));
        assertEquals(Integer.valueOf(330), codec.keyCode("key.keyboard.keypad.period"));
        assertEquals(Integer.valueOf(348), codec.keyCode("key.keyboard.application"));
        //?}
    }

    @Test
    void rejectsInvalidKeys() {
        assertNull(codec.keyName(9999));
        assertNull(codec.keyCode("key.keyboard.9999"));
        assertNull(codec.keyName(-1));
        assertNull(codec.keyCode("key.keyboard.-5"));
    }

    @Test
    void rejectsInvalidMouseButtons() {
        assertNull(codec.mouseName(InputConstants.MOUSE_BUTTON_LEFT - 1));
        assertNull(codec.mouseName(InputConstants.MOUSE_BUTTON_LEFT + 8));
        assertNull(codec.mouseButton("key.mouse.99"));
        assertNull(codec.mouseButton("key.keyboard.a"));
        assertNull(codec.legacyMouseName(99));
    }
}
