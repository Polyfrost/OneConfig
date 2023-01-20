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

package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.Platform;
import net.minecraft.client.Minecraft;

public class PlatformImpl implements Platform {
    @Override
    public boolean isCallingFromMinecraftThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    @Override
    public int getMinecraftVersion() {
        //#if MC>=11900
        //$$ return 11900;
        //#elseif MC>=11800
        //$$ return 11800;
        //#elseif MC>=11700
        //$$ return 11700;
        //#elseif MC>=11600
        //$$ return 11600;
        //#elseif MC>=11500
        //$$ return 11500;
        //#elseif MC>=11400
        //$$ return 11400;
        //#elseif MC>=11300
        //$$ return 11300;
        //#elseif MC>=11200
        //$$ return 11200;
        //#elseif MC>=11100
        //$$ return 11100;
        //#elseif MC>=11000
        //$$ return 11000;
        //#elseif MC>=10900
        //$$ return 10900;
        //#else
        return 10800;
        //#endif
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //#if FORGE==1 && MC<=11202
        try {
            Class.forName("net.minecraft.block.BlockDirt");
            return true;
        } catch (Exception ignored) {
            return false;
        }
        //#elseif FABRIC==1
        //$$ return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
        //#else
        //$$ return !net.minecraftforge.fml.loading.FMLLoader.isProduction();
        //#endif
    }

    @Override
    public Loader getLoader() {
        //#if FORGE==1
        return Loader.FORGE;
        //#else
        //$$ return Loader.FABRIC;
        //#endif
    }
}
