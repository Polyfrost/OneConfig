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

package org.polyfrost.oneconfig.api.commands.arguments;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.utils.NetworkUtils;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The player argument parser. Returns a {@link GameProfile}.
 */
public class PlayerArgumentParser implements ArgumentParser<GameProfile> {
    private static final HashMap<String, UUID> uuidCache = new HashMap<>();

    @Override
    public @Nullable GameProfile parse(@NotNull String arg) {
        List<GameProfile> matchingPlayers = getMatchingPlayers(arg, false);
        for (GameProfile profile : matchingPlayers) {
            return profile;
        }
        return new GameProfile(getUUID(arg), arg);
    }

    @Override
    public boolean canParse(Class<?> type) {
        return type == GameProfile.class;
    }

    // checks mojang api for player uuid from name
    private static UUID getUUID(String name) {
        try {
            if (uuidCache.containsKey(name)) {
                return uuidCache.get(name);
            }
            JsonObject json = NetworkUtils.getJsonElement("https://api.mojang.com/users/profiles/minecraft/" + name).getAsJsonObject();
            if (json.has("error")) {
                return null;
            }
            UUID uuid = UUID.fromString(json.get("id").getAsString().replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
            ));
            uuidCache.put(name, uuid);
            return uuid;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<GameProfile> getMatchingPlayers(String arg, boolean startWith) {
        if (Minecraft.getMinecraft().theWorld == null) return Lists.newArrayList();
        return Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream().map(NetworkPlayerInfo::getGameProfile).filter(gameProfile -> {
            String name = gameProfile.getName().toLowerCase();
            if (name.startsWith("!")) {
                return false;
            } else {
                return startWith ? name.startsWith(arg.toLowerCase()) : name.equals(arg.toLowerCase());
            }
        }).collect(Collectors.toList());
    }
}
