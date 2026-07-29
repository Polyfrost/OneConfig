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

import org.junit.jupiter.api.Test;
import org.polyfrost.compose.render.PolyColor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PolyColor constants are shared instances. Using the same constant as the default of several
 * options must not make a change to one option leak into the others: {@code Property.Field#set0}
 * mutates complex values in place, so immutable value types must be replaced by reference instead.
 */
public class SharedColorDefaultTest {

    static class Bean {
        public PolyColor first = PolyColor.WHITE;
        public PolyColor second = PolyColor.WHITE;
    }

    @Test
    void settingOneOptionDoesNotChangeSiblingsSharingTheSameDefault() throws Exception {
        Bean b = new Bean();
        Property<?> first = Properties.field("first", null, Bean.class.getDeclaredField("first"), b);

        first.setAs(PolyColor.Companion.rgba(255, 0, 0, 255));

        assertEquals(0xFFFF0000, b.first.getRawArgb());
        assertEquals(0xFFFFFFFF, b.second.getRawArgb());
        assertEquals(0xFFFFFFFF, PolyColor.WHITE.getRawArgb());
    }

    @Test
    void chromaFlagIsNotLeakedOntoTheSharedConstant() throws Exception {
        Bean b = new Bean();
        Property<?> first = Properties.field("first", null, Bean.class.getDeclaredField("first"), b);

        first.setAs(PolyColor.WHITE.withChroma(true, 2f));

        assertTrue(b.first.getChroma());
        assertFalse(b.second.getChroma());
        assertFalse(PolyColor.WHITE.getChroma());
    }
}
