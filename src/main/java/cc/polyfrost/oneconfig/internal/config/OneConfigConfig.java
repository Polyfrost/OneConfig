/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.core.OneColor;

import java.util.ArrayList;
import java.util.List;

public class OneConfigConfig extends InternalConfig {
    public static String currentProfile = "Default Profile";
    public static boolean autoUpdate = true;
    /**
     * 0 = Releases
     * 1 = Pre-Releases
     */
    public static int updateChannel = 0;
    public static List<String> favoriteMods = new ArrayList<>();
    public static List<OneColor> favoriteColors = new ArrayList<>();
    public static List<OneColor> recentColors = new ArrayList<>();
    public static boolean australia = false;

    private static OneConfigConfig INSTANCE;

    public OneConfigConfig() {
        super("", "OneConfig.json");
        initialize();
        INSTANCE = this;
    }

    public static OneConfigConfig getInstance() {
        return INSTANCE == null ? (INSTANCE = new OneConfigConfig()) : INSTANCE;
    }
}
