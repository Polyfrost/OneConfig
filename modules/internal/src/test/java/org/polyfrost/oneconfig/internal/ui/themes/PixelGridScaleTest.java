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

    private static final float FIT_8K = 4.9678f;

    private static final float FIT_4K = 2.4839f;

    private static final float FIT_1440P = 1.6564f;

    private static final float FIT_1080P = 1.2423f;

    private static final float FIT_720P = 0.8929f;

    @Test
    void growsIntoWhateverSpareRoomTheWindowHas() {
        assertEquals(20f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1440P), 1e-4f);
    }

    @Test
    void anEightKScreenKeepsOnGrowing() {
        assertEquals(65f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_8K), 1e-4f);
    }

    @Test
    void aFourKScreenGrowsToThreePixelsPerGlyphPixel() {
        assertEquals(30f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_4K), 1e-4f);
    }

    @Test
    void takesTheLargestStepA1080pWindowHasRoomFor() {
        assertEquals(15f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1080P), 1e-4f);
    }

    @Test
    void anAlreadySnappedAnchorWithNoRoomToGrowIsLeftWhereItIs() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(20f, 1f), 1e-4f);
    }

    @Test
    void aRequestBiggerThanTheGrownEmKeepsItsOwnSize() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(38f, 1f), 1e-4f);
    }

    @Test
    void shrinksOntoTheGridWhenTheRequestSitsJustAboveAStep() {
        assertEquals(0.8282f, ProviderKt.snapScaleToPixelGrid(21f, 0.8282f), 1e-4f);
    }

    @Test
    void aGridStepThatWouldOverflowTheWindowFallsBackToTheRoomThereIs() {
        assertEquals(FIT_720P, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_720P), 1e-4f);
    }

    @Test
    void theOnePixelPerGlyphPixelFloorYieldsToAWindowWithNoRoomForIt() {
        assertEquals(0.5f, ProviderKt.snapScaleToPixelGrid(6f, 0.5f), 1e-4f);
    }

    @Test
    void minecraftsSmallestWindowStillLeavesItsMargin() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(7.7355f, 1f), 1e-4f);
    }

    @Test
    void aChosenSizeReplacesWhateverTheWindowWouldHavePicked() {
        assertEquals(20f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1080P, 20f), 1e-4f);
    }

    @Test
    void aChosenSizeIsHonouredEvenWhenItOverflowsTheWindow() {
        assertEquals(40f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1080P, 40f), 1e-4f);
    }

    @Test
    void aChosenSizeStillCannotGoBelowOnePixelPerGlyphPixel() {
        assertEquals(10f / ANCHOR_PX, ProviderKt.snapScaleToPixelGrid(ANCHOR_PX, FIT_1080P, 5f), 1e-4f);
    }

    @Test
    void aDegenerateAnchorIsLeftAlone() {
        assertEquals(1f, ProviderKt.snapScaleToPixelGrid(0f, 2f), 1e-4f);
    }
}
