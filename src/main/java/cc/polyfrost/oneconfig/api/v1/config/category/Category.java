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

package cc.polyfrost.oneconfig.api.v1.config.category;

import cc.polyfrost.oneconfig.api.v1.config.OneConfig;

/**
 * Represents a category of a {@link OneConfig}.
 */
public class Category {
    public static final Category PVP = new Category("PVP", "PvP");
    public static final Category HUD = new Category("HUD", "HUD");
    public static final Category UTIL_QOL = new Category("UTIL_QOL", "Utility");
    public static final Category HYPIXEL = new Category("HYPIXEL", "Hypixel");
    public static final Category SKYBLOCK = new Category("SKYBLOCK", "Skyblock");
    public static final Category THIRD_PARTY = new Category("THIRD_PARTY", "Third Party");

    private final String id;
    private final String name;

    public Category(String id, String name) {
        this.id = id;
        this.name = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
