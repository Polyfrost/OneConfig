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

package org.polyfrost.oneconfig.api.ui.v1.keybind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextInputFocusTest {
    private final Object token = new Object();

    @AfterEach
    void reset() {
        TextInputFocus.clear();
        TestScreenPlatform.setCurrent(null);
    }

    @Test
    void reportsTypingWhileAFieldOnTheOpenScreenHoldsFocus() {
        Object screen = new Object();
        TestScreenPlatform.setCurrent(screen);
        assertFalse(TextInputFocus.isTyping());

        TextInputFocus.acquire(token);
        assertTrue(TextInputFocus.isTyping());

        TextInputFocus.release(token);
        assertFalse(TextInputFocus.isTyping());
    }

    @Test
    void ignoresATokenLeftBehindByAScreenThatClosed() {
        TestScreenPlatform.setCurrent(new Object());
        TextInputFocus.acquire(token);

        TestScreenPlatform.setCurrent(new Object());
        assertFalse(TextInputFocus.isTyping());

        TestScreenPlatform.setCurrent(null);
        assertFalse(TextInputFocus.isTyping());
    }

    @Test
    void tracksEachFieldSeparatelySoOneReleaseDoesNotClearTheOther() {
        Object other = new Object();
        TestScreenPlatform.setCurrent(new Object());
        TextInputFocus.acquire(token);
        TextInputFocus.acquire(other);

        TextInputFocus.release(token);
        assertTrue(TextInputFocus.isTyping());

        TextInputFocus.release(other);
        assertFalse(TextInputFocus.isTyping());
    }
}
