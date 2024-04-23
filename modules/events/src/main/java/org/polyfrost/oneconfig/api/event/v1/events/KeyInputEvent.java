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

package org.polyfrost.oneconfig.api.event.v1.events;

/**
 * Raw key event dispatched by the Minecraft instance.
 * <br>
 * If you want to translate this into something usable by PolyUI, have a look at {@code KeybindManager.translateKey(EventManager, keyCode, character, down)}
 */
public class KeyInputEvent implements Event {
    /**
     * The keycode that created this event.
     * A value of 0 indicates this was not a coded event but a character event (see {@link #character})
     */
    public final int key;
    /**
     * The character that created this event.<br>
     * A value of 0 indicates this was not a character event but a coded event (see {@link #key})
     */
    public final char character;
    /**
     * 0 = up <br>
     * 1 = down <br>
     * 2 = repeat
     */
    public final int state;

    public KeyInputEvent(int key, char character, int state) {
        this.key = key;
        this.character = character;
        this.state = state;
    }

    public int component1() {
        return key;
    }

    public char component2() {
        return character;
    }

    public int component3() {
        return state;
    }

    public boolean isPressed() {
        return state > 0;
    }
}
