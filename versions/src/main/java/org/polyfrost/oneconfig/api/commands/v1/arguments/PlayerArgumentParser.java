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

package org.polyfrost.oneconfig.api.commands.v1.arguments;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The player argument parser. Returns a {@link GameProfile}.
 */
public class PlayerArgumentParser extends ArgumentParser<GameProfile> {
    @Override
    public @Nullable GameProfile parse(@NotNull String arg) {
        return getMatchingPlayers(arg, false).findFirst().orElse(null);
    }

    @Override
    public @Nullable List<@NotNull String> getAutoCompletions(String input) {
        return getMatchingPlayers(input, true).map(GameProfile::getName).collect(Collectors.toList());
    }

    @Override
    public Class<GameProfile> getType() {
        return GameProfile.class;
    }

    private static Stream<GameProfile> getMatchingPlayers(String arg, boolean startWith) {
        if (Minecraft.getMinecraft().theWorld == null) return Stream.of();
        String l = arg.toLowerCase();
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream().map(NetworkPlayerInfo::getGameProfile).filter(gameProfile -> {
            String n = gameProfile.getName();
            if (n == null) return false;
            String name = n.toLowerCase();
            if (name.charAt(0) == '!') {
                return false;
            } else {
                return startWith ? name.startsWith(l) : name.equals(l);
            }
        });
    }
}
