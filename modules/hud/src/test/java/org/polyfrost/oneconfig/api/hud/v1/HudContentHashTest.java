/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by Polyfrost, AND
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

package org.polyfrost.oneconfig.api.hud.v1;

import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HudContentHashTest {

    static class CountingHud extends TextHud {
        int measured = 0;
        long hash = Hud.NO_CONTENT_HASH;

        CountingHud() {
            super("counting-hud", "Counting Hud", Hud.Category.getINFO(), "", "");
            setUseGuiScale(true);
            setCustomScale(1f);
        }

        @Override public String getText() { return "hi"; }

        @Override public Pair<Float, Float> minimumSize() {
            measured++;
            return new Pair<>(10f, 10f);
        }

        @Override public long contentHash() { return hash; }
    }

    private CountingHud hud;

    @BeforeEach
    void setUp() {
        hud = new CountingHud();
    }

    private static void nextFrame() throws Exception {
        Field f = HudManager.class.getDeclaredField("frameId");
        f.setAccessible(true);
        f.setLong(HudManager.INSTANCE, f.getLong(HudManager.INSTANCE) + 1L);
    }

    @Test
    void withoutAContentHashEveryFrameIsMeasured() throws Exception {
        for (int i = 0; i < 5; i++) {
            nextFrame();
            hud.frameMinimumSize();
            hud.frameMinimumSize();
        }
        assertEquals(5, hud.measured, "one measure per frame, and only one despite two asks");
    }

    @Test
    void aStableContentHashIsMeasuredOnce() throws Exception {
        hud.hash = 7L;
        for (int i = 0; i < 5; i++) {
            nextFrame();
            hud.frameMinimumSize();
        }
        assertEquals(1, hud.measured, "nothing changed, so the first measurement still stands");
    }

    @Test
    void aChangedContentHashIsRemeasured() throws Exception {
        hud.hash = 7L;
        nextFrame();
        hud.frameMinimumSize();

        hud.hash = 8L;
        nextFrame();
        hud.frameMinimumSize();

        nextFrame();
        hud.frameMinimumSize();

        assertEquals(2, hud.measured, "once for each distinct hash");
    }

    @Test
    void changingScaleRemeasuresEvenOnAStableHash() throws Exception {
        hud.hash = 7L;
        nextFrame();
        hud.frameMinimumSize();

        hud.setCustomScale(2f);
        nextFrame();
        hud.frameMinimumSize();

        assertEquals(2, hud.measured, "scale is not part of contentHash, so it is compared separately");
    }

    @Test
    void theContentHashIsAskedForOncePerFrame() throws Exception {
        CountingHashHud counting = new CountingHashHud();
        nextFrame();
        counting.frameContentHash();
        counting.frameContentHash();
        counting.frameMinimumSize();
        assertEquals(1, counting.asked, "frameContentHash must memoise within a frame");
    }

    static class CountingHashHud extends TextHud {
        int asked = 0;

        CountingHashHud() {
            super("counting-hash-hud", "Counting Hash Hud", Hud.Category.getINFO(), "", "");
            setUseGuiScale(true);
            setCustomScale(1f);
        }

        @Override public String getText() { return "hi"; }

        @Override public long contentHash() { asked++; return 3L; }
    }
}
