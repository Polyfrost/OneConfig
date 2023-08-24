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

//#if MC<=11202
package org.polyfrost.oneconfig.utils.commands.arguments;

import org.polyfrost.oneconfig.internal.utils.Deprecator;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The old player argument parser. Returns an {@link EntityPlayer}.
 * @deprecated Use {@link PlayerArgumentParser} instead.
 */
@Deprecated
public class EntityPlayerArgumentParser extends ArgumentParser<EntityPlayer> {
    @Nullable
    @Override
    public EntityPlayer parse(@NotNull String arg) {
        Deprecator.markDeprecated();
        List<EntityPlayer> matchingPlayers = getMatchingPlayers(arg, false);
        for (EntityPlayer profile : matchingPlayers) {
            return profile;
        }
        throw new IllegalArgumentException("Player not found");
    }
    // This only returns players in tab list that match, not all players in the current server, hence why this is deprecated.
    private static List<EntityPlayer> getMatchingPlayers(String arg, boolean startsWith) {
        if (Minecraft.getMinecraft().theWorld == null) return Lists.newArrayList();
        return Minecraft.getMinecraft().theWorld.getPlayers(EntityPlayer.class,
                player -> (startsWith ? player.getName().startsWith(arg) : player.getName().equals(arg)));
    }

    @NotNull
    @Override
    public List<String> complete(String current, Parameter parameter) {
        return getMatchingPlayers(current, true).stream().map(EntityPlayer::getName).collect(Collectors.toList());
    }
}
//#endif
