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

package cc.polyfrost.oneconfig.utils.hypixel;

import cc.polyfrost.oneconfig.internal.utils.Deprecator;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import com.google.gson.JsonObject;

import java.util.Locale;

/**
 * Various utilities for Hypixel.
 */
public class HypixelUtils {
    public static final HypixelUtils INSTANCE = new HypixelUtils();

    public void initialize() {
        LocrawUtil.INSTANCE.initialize();
    }

    /**
     * Checks whether the player is on Hypixel.
     *
     * @return Whether the player is on Hypixel.
     * @see <a href="https://canary.discord.com/channels/864592657572560958/945075920664928276/978649312013725747">this discord message from discord.gg/essential</a>
     */
    public boolean isHypixel() {
        if (!Platform.getServerPlatform().inMultiplayer()) return false;

        String serverBrand = Platform.getServerPlatform().getServerBrand();

        if (serverBrand == null) return false;

        return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
    }

    public boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        try {
            JsonObject gotten = NetworkUtils.getJsonElement("https://api.hypixel.net/key?key=" + key).getAsJsonObject();
            return gotten.has("success") && gotten.get("success").getAsBoolean();
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Returns whether the player is in game.
     *
     * @return Whether the player is in game.
     * @deprecated Moved to {@link LocrawUtil}
     */
    @Deprecated
    public boolean isInGame() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.isInGame();
    }

    /**
     * Returns the current {@link LocrawInfo}.
     *
     * @return The current {@link LocrawInfo}.
     * @deprecated Moved to {@link LocrawUtil}
     * @see LocrawInfo
     */
    @Deprecated
    public LocrawInfo getLocrawInfo() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.getLocrawInfo();
    }

    /**
     * Returns the previous {@link LocrawInfo}.
     *
     * @return The previous {@link LocrawInfo}.
     * @deprecated Moved to {@link LocrawUtil}
     * @see LocrawInfo
     */
    @Deprecated
    public LocrawInfo getPreviousLocraw() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.getLastLocrawInfo();
    }
}
