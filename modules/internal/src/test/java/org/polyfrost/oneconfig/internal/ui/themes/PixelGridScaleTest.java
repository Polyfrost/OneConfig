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

package org.polyfrost.oneconfig.internal.ui.themes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PixelGridScaleTest {
    private static final float ANCHOR_PX = 14f;

    private static final float FIT_1440P = 1.6564f;

    private static final float FIT_1080P = 1.2423f;

    @Test
    void growsToTheNextWholeGlyphPixelWhenTheWindowFitsIt() {
        assertEquals(20f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1440P), 1e-4f);
    }

    @Test
    void leavesTheScaleAloneWhenTheNextStepDoesNotFit() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1080P), 1e-4f);
    }

    @Test
    void neverShrinksTheScale() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, 1f), 1e-4f);
    }

    @Test
    void anAlreadySnappedAnchorIsLeftWhereItIs() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(20f, 2f), 1e-4f);
    }

    @Test
    void aRetinaAnchorOnlyNeedsANudge() {
        assertEquals(30f / 28f, ProviderKt.snapScaleToPixelGrid(28f, 1.8f), 1e-4f);
    }

    @Test
    void aDegenerateAnchorIsLeftAlone() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(0f, 2f), 1e-4f);
    }
}
