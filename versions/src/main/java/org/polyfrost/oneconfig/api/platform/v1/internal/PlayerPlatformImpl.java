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

package org.polyfrost.oneconfig.api.platform.v1.internal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Session;
import org.polyfrost.oneconfig.api.platform.v1.PlayerPlatform;

public class PlayerPlatformImpl implements PlayerPlatform {
    private ServerData old;
    private Server cache;

    @Override
    public boolean inMultiplayer() {
        return Minecraft.getMinecraft().theWorld != null && !Minecraft.getMinecraft().isSingleplayer();
    }

    @Override
    public boolean doesPlayerExist() {
        return Minecraft.getMinecraft().thePlayer != null;
    }

    @Override
    public String getPlayerName() {
        Session s = Minecraft.getMinecraft().getSession();
        return s.getUsername();
    }

    @Override
    public Server getCurrentServer() {
        ServerData d = Minecraft.getMinecraft().getCurrentServerData();
        if (d == null) return null;
        if (old == d) return cache;
        old = d;
        return cache = new Server(d.serverIP, d.serverName);
    }
}
