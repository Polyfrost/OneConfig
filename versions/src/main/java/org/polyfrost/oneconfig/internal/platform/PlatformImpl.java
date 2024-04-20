/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.internal.platform;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.platform.Platform;

public class PlatformImpl implements Platform {
    @Override
    public boolean isCallingFromMinecraftThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    @Override
    public int getMinecraftVersion() {
        //#if MC>=12000
        //$$ return 12000;
        //#elseif MC>=11900
        //$$ return 11900;
        //#elseif MC>=11800
        //$$ return 11800;
        //#elseif MC>=11700
        //$$ return 11700;
        //#elseif MC>=11600
        //$$ return 11600;
        //#elseif MC>=11200
        //$$ return 11200;
        //#else
        return 10800;
        //#endif
    }

    //#if FORGE && MC<11300
    private static final boolean isDev;
    static {
        boolean dev;
        try {
            Class.forName("net.minecraft.block.BlockDirt");
            dev = true;
        } catch (Exception ignored) {
            dev = false;
        }
        isDev = dev;
    }
    //#endif

    @Override
    public boolean isDevelopmentEnvironment() {
        //#if FORGE && MC<11300
        return isDev;
        //#elseif FABRIC
        //$$ return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        //#else
        //$$ return !net.minecraftforge.fml.loading.FMLLoader.isProduction();
        //#endif
    }

    @Override
    public Loader getLoader() {
        //#if FORGE
        return Loader.FORGE;
        //#else
        //$$ return Loader.FABRIC;
        //#endif
    }

    @Override
    public String getPlayerName() {
        return Minecraft.getMinecraft().getSession().getUsername();
    }
}
