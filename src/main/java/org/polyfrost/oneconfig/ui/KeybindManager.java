/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.ui;

import org.jetbrains.annotations.Unmodifiable;
import org.polyfrost.oneconfig.api.events.event.RawKeyEvent;
import org.polyfrost.oneconfig.api.events.event.Stage;
import org.polyfrost.oneconfig.api.events.event.TickEvent;
import org.polyfrost.oneconfig.api.events.invoke.EventHandler;
import org.polyfrost.oneconfig.libs.universal.UKeyboard;
import org.polyfrost.oneconfig.platform.Platform;
import org.polyfrost.polyui.event.EventManager;
import org.polyfrost.polyui.input.KeyBinder;
import org.polyfrost.polyui.input.Keys;
import org.polyfrost.polyui.input.Modifiers;
import org.polyfrost.polyui.property.Settings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeybindManager {
    public static final KeybindManager INSTANCE = new KeybindManager();
    /**
     * A map of Minecraft (LWJGL) key codes to PolyUI modifiers.
     */
    @Unmodifiable
    public static final Map<Integer, Modifiers> modsMap;

    /**
     * A map of Minecraft (LWJGL) key codes to PolyUI keys.
     */
    @Unmodifiable
    public static final Map<Integer, Keys> keyMap;

    public final Settings settings = new Settings();
    public final KeyBinder keyBinder = new KeyBinder(settings);
    private final EventManager eventManager = new EventManager(null, keyBinder, settings);

    static {
        Map<Integer, Modifiers> mm = new HashMap<>(8, 1f);
        mm.put(UKeyboard.KEY_LSHIFT, Modifiers.LSHIFT);
        mm.put(UKeyboard.KEY_RSHIFT, Modifiers.RSHIFT);
        mm.put(UKeyboard.KEY_LCONTROL, Modifiers.LCONTROL);
        mm.put(UKeyboard.KEY_RCONTROL, Modifiers.RCONTROL);
        mm.put(UKeyboard.KEY_LMETA, Modifiers.LMETA);
        mm.put(UKeyboard.KEY_RMETA, Modifiers.RMETA);
        mm.put(UKeyboard.KEY_LMENU, Modifiers.LALT);
        mm.put(UKeyboard.KEY_RMENU, Modifiers.RALT);
        modsMap = Collections.unmodifiableMap(mm);

        Map<Integer, Keys> km = new HashMap<>(32);
        km.put(UKeyboard.KEY_F1, Keys.F1);
        km.put(UKeyboard.KEY_F2, Keys.F2);
        km.put(UKeyboard.KEY_F3, Keys.F3);
        km.put(UKeyboard.KEY_F4, Keys.F4);
        km.put(UKeyboard.KEY_F5, Keys.F5);
        km.put(UKeyboard.KEY_F6, Keys.F6);
        km.put(UKeyboard.KEY_F7, Keys.F7);
        km.put(UKeyboard.KEY_F8, Keys.F8);
        km.put(UKeyboard.KEY_F9, Keys.F9);
        km.put(UKeyboard.KEY_F10, Keys.F10);
        km.put(UKeyboard.KEY_F11, Keys.F11);
        km.put(UKeyboard.KEY_F12, Keys.F12);
        km.put(UKeyboard.KEY_ESCAPE, Keys.ESCAPE);
        km.put(UKeyboard.KEY_ENTER, Keys.ENTER);
        km.put(UKeyboard.KEY_BACKSPACE, Keys.BACKSPACE);
        km.put(UKeyboard.KEY_TAB, Keys.TAB);
        // km.put(UKeyboard.KEY_PRIOR, Keys.PAGE_UP);
        // km.put(UKeyboard.KEY_NEXT, Keys.PAGE_DOWN);
        km.put(UKeyboard.KEY_END, Keys.END);
        km.put(UKeyboard.KEY_HOME, Keys.HOME);
        km.put(UKeyboard.KEY_LEFT, Keys.LEFT);
        km.put(UKeyboard.KEY_UP, Keys.UP);
        km.put(UKeyboard.KEY_RIGHT, Keys.RIGHT);
        km.put(UKeyboard.KEY_DOWN, Keys.DOWN);
        // km.put(UKeyboard.KEY_INSERT, Keys.INSERT);
        keyMap = Collections.unmodifiableMap(km);
    }

    private KeybindManager() {
        EventHandler.of(RawKeyEvent.class, (event) -> {
            // keybindings only work when in game (todo maybe change)?
            if (Platform.getGuiPlatform().getCurrentScreen() == null) {
                translateKey(eventManager, event.key, event.character, event.state != 0);
            }
        }).register();
        EventHandler.of(TickEvent.class, (event) -> {
            if (event.stage == Stage.START) {
                keyBinder.update(50_000L, eventManager.getKeyModifiers());
            }
        }).register();
    }

    public static void registerKeybind(KeyBinder.Bind bind) {
        INSTANCE.keyBinder.add(bind);
    }

    /**
     * Translate a raw key event from Minecraft into a PolyUI one, handing it to the provided event manager.
     *
     * @param ev        the event manager to give the event to
     * @param keyCode   the key code of the event
     * @param typedChar the character of the event
     * @param down      weather this event was a down or up press
     * @see RawKeyEvent
     */
    public static void translateKey(EventManager ev, int keyCode, char typedChar, boolean down) {
//        System.err.println("keyc=" + keyCode + ", ch=" + typedChar + ", d=" + down);
        if (typedChar != 0) {
            // System.err.println("typing char " + typedChar);
            ev.keyTyped(typedChar);
            return;
        }

        Modifiers m = modsMap.get(keyCode);
        if (m != null) {
            // System.err.println("doing mod " + Arrays.toString(Modifiers.fromModifierMerged(v)));
            if (down) ev.addModifier(m.getValue());
            else ev.removeModifier(m.getValue());
            return;
        }

        Keys k = keyMap.get(keyCode);
        if (k != null) {
            // System.err.println("handling known " + k.getKeyName());
            if (down) ev.keyDown(k);
            else ev.keyUp(k);
            return;
        }


        // System.err.println("handling unknown " + keyCode);
        if (down) ev.keyDown(keyCode);
        else ev.keyUp(keyCode);
    }
}
