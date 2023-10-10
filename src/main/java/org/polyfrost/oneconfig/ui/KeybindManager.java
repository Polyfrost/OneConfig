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

public class KeybindManager {
    public static final KeybindManager INSTANCE = new KeybindManager();
    public final Settings settings = new Settings();
    public final KeyBinder keyBinder = new KeyBinder(settings);
    private final EventManager eventManager = new EventManager(null, keyBinder, settings);

    private KeybindManager() {
        EventHandler.register(RawKeyEvent.class, (event) -> {
            // keybindings only work when in game (todo maybe change)?
            if (Platform.getGuiPlatform().getCurrentScreen() == null) {
                translateKey(eventManager, event.key, event.character, event.state != 0);
            }
        });
        EventHandler.register(TickEvent.class, (event) -> {
            if (event.stage == Stage.START) {
                keyBinder.update(50_000L, eventManager.getKeyModifiers());
            }
        });
    }

    public static void registerKeybind(KeyBinder.Bind bind) {
        INSTANCE.keyBinder.add(bind);
    }

    /**
     * Translate a raw key event from Minecraft into a PolyUI one.
     * @param ev the event manager to give the event to
     * @param keyCode the key code of the event
     * @param typedChar the character of the event
     * @param down weather this event was a down or up press
     * @see RawKeyEvent
     */
    public static void translateKey(EventManager ev, int keyCode, char typedChar, boolean down) {
//        System.err.println("keyc=" + keyCode + ", ch=" + typedChar + ", d=" + down);
        if (typedChar != 0) {
//            System.err.println("typing char " + typedChar);
            ev.keyTyped(typedChar);
            return;
        }
        if (keyCode == UKeyboard.KEY_LSHIFT) mod(ev, Modifiers.LSHIFT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RSHIFT) mod(ev, Modifiers.RSHIFT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LCONTROL) mod(ev, Modifiers.LCONTROL.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RCONTROL) mod(ev, Modifiers.RCONTROL.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LMENU) mod(ev, Modifiers.LALT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RMENU) mod(ev, Modifiers.RALT.getValue(), down);
        else if (keyCode == UKeyboard.KEY_LMETA) mod(ev, Modifiers.LMETA.getValue(), down);
        else if (keyCode == UKeyboard.KEY_RMETA) mod(ev, Modifiers.RMETA.getValue(), down);
        else {
            Keys k;
            // you can't switch because of the stupid noInline stuff
            if (keyCode == UKeyboard.KEY_F1) k = Keys.F1;
            else if (keyCode == UKeyboard.KEY_F2) k = Keys.F2;
            else if (keyCode == UKeyboard.KEY_F3) k = Keys.F3;
            else if (keyCode == UKeyboard.KEY_F4) k = Keys.F4;
            else if (keyCode == UKeyboard.KEY_F5) k = Keys.F5;
            else if (keyCode == UKeyboard.KEY_F6) k = Keys.F6;
            else if (keyCode == UKeyboard.KEY_F7) k = Keys.F7;
            else if (keyCode == UKeyboard.KEY_F8) k = Keys.F8;
            else if (keyCode == UKeyboard.KEY_F9) k = Keys.F9;
            else if (keyCode == UKeyboard.KEY_F10) k = Keys.F10;
            else if (keyCode == UKeyboard.KEY_F11) k = Keys.F11;
            else if (keyCode == UKeyboard.KEY_F12) k = Keys.F12;
            else if (keyCode == UKeyboard.KEY_ESCAPE) k = Keys.ESCAPE;
            else if (keyCode == UKeyboard.KEY_ENTER) k = Keys.ENTER;
            else if (keyCode == UKeyboard.KEY_BACKSPACE) k = Keys.BACKSPACE;
            else if (keyCode == UKeyboard.KEY_TAB) k = Keys.TAB;
//            else if (keyCode == UKeyboard.KEY_PRIOR) k = Keys.PAGE_UP;
//            else if (keyCode == UKeyboard.KEY_NEXT) k = Keys.PAGE_DOWN;
            else if (keyCode == UKeyboard.KEY_END) k = Keys.END;
            else if (keyCode == UKeyboard.KEY_HOME) k = Keys.HOME;
            else if (keyCode == UKeyboard.KEY_LEFT) k = Keys.LEFT;
            else if (keyCode == UKeyboard.KEY_UP) k = Keys.UP;
            else if (keyCode == UKeyboard.KEY_RIGHT) k = Keys.RIGHT;
            else if (keyCode == UKeyboard.KEY_DOWN) k = Keys.DOWN;
//            else if (keyCode == UKeyboard.KEY_INSERT) k = Keys.INSERT;
            else k = Keys.UNKNOWN;

            if (k == Keys.UNKNOWN) {
//                System.err.println("handling unknown " + keyCode);
                if (down) ev.keyDown(keyCode);
                else ev.keyUp(keyCode);
            } else {
//                System.err.println("handling known " + k.getKeyName());
                if (down) ev.keyDown(k);
                else ev.keyUp(k);
            }
        }
    }

    private static void mod(EventManager ev, short v, boolean down) {
//        System.err.println("doing mod " + Arrays.toString(Modifiers.fromModifierMerged(v)));
        if (down) ev.addModifier(v);
        else ev.removeModifier(v);
    }
}
