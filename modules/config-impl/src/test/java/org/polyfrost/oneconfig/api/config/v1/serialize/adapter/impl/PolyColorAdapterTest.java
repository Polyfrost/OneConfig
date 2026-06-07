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

package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.junit.jupiter.api.Test;
import org.polyfrost.compose.render.PolyColor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PolyColorAdapterTest {
    private final PolyColorAdapter adapter = new PolyColorAdapter();

    @Test
    public void serializesStaticColorsAsLegacyArrays() {
        PolyColor color = PolyColor.Companion.rgba(20, 55, 3, 100);

        Object serialized = adapter.serialize(color);

        assertInstanceOf(int[].class, serialized);
        assertArrayEquals(new int[]{20, 55, 3, 100}, (int[]) serialized);
        PolyColor deserialized = adapter.deserialize(serialized);
        assertEquals(color.getRawArgb(), deserialized.getRawArgb());
        assertFalse(deserialized.getChroma());
    }

    @Test
    public void roundTripsChromaMetadata() {
        PolyColor color = new PolyColor(0x64143703, true, 2.5f);

        Object serialized = adapter.serialize(color);

        assertInstanceOf(Map.class, serialized);
        Map<?, ?> map = (Map<?, ?>) serialized;
        assertArrayEquals(new int[]{20, 55, 3, 100}, (int[]) map.get("rgba"));
        assertEquals(true, map.get("chroma"));
        assertEquals(2.5f, (Float) map.get("chromaSpeed"));

        PolyColor deserialized = adapter.deserialize(serialized);
        assertEquals(color.getRawArgb(), deserialized.getRawArgb());
        assertTrue(deserialized.getChroma());
        assertEquals(2.5f, deserialized.getChromaSpeed());
    }
}
