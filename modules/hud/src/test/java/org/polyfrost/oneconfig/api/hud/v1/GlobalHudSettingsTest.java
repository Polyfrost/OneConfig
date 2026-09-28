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

package org.polyfrost.oneconfig.api.hud.v1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalHudSettingsTest {
    static class LookHud extends TextHud {
        LookHud() { super("global-settings-hud", "Global Settings Hud", Hud.Category.getINFO(), "", ""); }
        @Override public String getText() { return "hi"; }
    }

    @AfterEach
    void resetGlobals() {
        GlobalHudSettings.setEnabled(true);
        GlobalHudSettings.setOverrideFont(false);
        GlobalHudSettings.setFont(Font.Minecraft);
        GlobalHudSettings.setOverrideTextWeight(false);
        GlobalHudSettings.setTextWeight(Weight.Regular);
        GlobalHudSettings.setOverrideShowBackground(false);
        GlobalHudSettings.setShowBackground(true);
        GlobalHudSettings.setOverrideBackgroundRadius(false);
        GlobalHudSettings.setBackgroundRadius(4f);
    }

    @Test
    void defaultsKeepPerHudLooks() {
        LookHud hud = new LookHud();
        hud.setFont(Font.Poppins);
        hud.setTextWeight(Weight.Black);
        hud.setBgRadius(12f);
        hud.setShowBackground(false);

        assertTrue(GlobalHudSettings.getEnabled());
        assertEquals(Font.Poppins, hud.getEffectiveFont());
        assertEquals(Weight.Black, hud.getEffectiveTextWeight());
        assertEquals(12f, hud.getEffectiveBackgroundRadius(), 0.0001f);
        assertFalse(hud.getEffectiveShowBackground());
    }

    @Test
    void overridesWinWithoutTouchingStoredValues() {
        LookHud hud = new LookHud();
        hud.setFont(Font.Minecraft);
        hud.setTextWeight(Weight.Regular);
        hud.setBgRadius(4f);
        hud.setShowBackground(true);

        GlobalHudSettings.setOverrideFont(true);
        GlobalHudSettings.setFont(Font.Poppins);
        GlobalHudSettings.setOverrideTextWeight(true);
        GlobalHudSettings.setTextWeight(Weight.Bold);
        GlobalHudSettings.setOverrideShowBackground(true);
        GlobalHudSettings.setShowBackground(false);
        GlobalHudSettings.setOverrideBackgroundRadius(true);
        GlobalHudSettings.setBackgroundRadius(9f);

        assertEquals(Font.Poppins, hud.getEffectiveFont());
        assertEquals(Weight.Bold, hud.getEffectiveTextWeight());
        assertFalse(hud.getEffectiveShowBackground());
        assertEquals(9f, hud.getEffectiveBackgroundRadius(), 0.0001f);

        assertEquals(Font.Minecraft, hud.getFont());
        assertEquals(Weight.Regular, hud.getTextWeight());
        assertTrue(hud.getShowBackground());
        assertEquals(4f, hud.getBgRadius(), 0.0001f);

        GlobalHudSettings.setOverrideFont(false);
        GlobalHudSettings.setOverrideTextWeight(false);
        GlobalHudSettings.setOverrideShowBackground(false);
        GlobalHudSettings.setOverrideBackgroundRadius(false);
        assertEquals(Font.Minecraft, hud.getEffectiveFont());
        assertEquals(Weight.Regular, hud.getEffectiveTextWeight());
        assertTrue(hud.getEffectiveShowBackground());
        assertEquals(4f, hud.getEffectiveBackgroundRadius(), 0.0001f);
    }

    @Test
    void poppinsFontNameFollowsWeightOverride() {
        LookHud hud = new LookHud();
        hud.setFont(Font.Poppins);
        hud.setTextWeight(Weight.Regular);
        assertEquals("poppins", hud.getPoppinsFontName());

        GlobalHudSettings.setOverrideTextWeight(true);
        GlobalHudSettings.setTextWeight(Weight.Medium);
        assertEquals("poppins-medium", hud.getPoppinsFontName());

        GlobalHudSettings.setOverrideTextWeight(false);
        assertEquals("poppins", hud.getPoppinsFontName());
    }

    @Test
    void masterToggleRoundTrips() {
        assertTrue(GlobalHudSettings.getEnabled());
        GlobalHudSettings.setEnabled(false);
        assertFalse(GlobalHudSettings.getEnabled());
    }
}
