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

package org.polyfrost.oneconfig.internal.ui.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.platform.v1.ModInfo;
import org.polyfrost.oneconfig.internal.ui.TestCompatibilityPlatform;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeConfigDataTest {
    private static void installed(String version) {
        TestCompatibilityPlatform.mods = Set.of(new ModInfo("probe", "Probe", version, null, null));
        ModInfo.forgetLoadedMods();
    }

    @AfterEach
    void clear() {
        TestCompatibilityPlatform.mods = Set.of();
        ModInfo.forgetLoadedMods();
    }

    @Test
    void aCardShowsTheVersionOfAModUpdatedWhileTheGameRuns() {
        installed("1.0");
        TreeConfigData card = new TreeConfigData(Tree.tree("probe"), ConfigSource.OC, null);
        assertEquals("1.0", card.getVersion());

        installed("2.0");
        assertEquals("2.0", card.getVersion());
    }
}
