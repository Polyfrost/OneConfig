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

package org.polyfrost.oneconfig.utils.hypixel;

import org.polyfrost.oneconfig.platform.Platform;

import java.util.Locale;

/**
 * Various utilities for Hypixel.
 */
public final class HypixelUtils {

    private HypixelUtils() {
    }

    /**
     * Alias for {@link LocrawUtil#INSTANCE}.
     */
    public static LocrawUtil getLocraw() {
        return LocrawUtil.INSTANCE;
    }

    /**
     * Checks whether the player is on Hypixel.
     *
     * @return Whether the player is on Hypixel.
     * @see <a href="https://canary.discord.com/channels/864592657572560958/945075920664928276/978649312013725747">this discord message from discord.gg/essential</a>
     */
    public static boolean isHypixel() {
        if (!Platform.getServerPlatform().inMultiplayer()) return false;

        String serverBrand = Platform.getServerPlatform().getServerBrand();

        if (serverBrand == null) return false;

        return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
    }
}
