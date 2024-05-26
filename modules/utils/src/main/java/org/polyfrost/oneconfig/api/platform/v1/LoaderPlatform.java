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

package org.polyfrost.oneconfig.api.platform.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public interface LoaderPlatform {
    boolean isModLoaded(String id);

    @Nullable
    ActiveMod toActiveMod(@Nullable Object in);

    /**
     * Note: the list may contain null elements
     */
    @NotNull
    List<ActiveMod> getLoadedMods();

    @Nullable
    default ActiveMod getLoadedMod(String id) {
        for (ActiveMod mod : getLoadedMods()) {
            if (mod == null) continue;
            if (id.equals(mod.id)) return mod;
        }
        return null;
    }

    /**
     * return the minecraft version of the current instance, as per the preprocessor standard.
     * for example, if the minecraft version is 1.16.5, this will return 11605.
     */
    int getMinecraftVersion();

    /**
     * return a string representing the loader and the minecraft version of the current instance, as per the preprocessor standard.
     * for example, if the loader is Forge and the minecraft version is 1.16.5, this will return "1.16.5-forge".
     */
    default String getLoaderString() {
        char[] ver = String.valueOf(getMinecraftVersion()).toCharArray();
        StringBuilder sb = new StringBuilder();
        sb.append(ver[0]).append('.');
        if(ver[1] == '0') {
            sb.append(ver[2]);
        } else {
            sb.append(ver[1]).append(ver[2]);
        }
        sb.append('.');
        if(ver[3] == '0') {
            sb.append(ver[4]);
        } else {
            sb.append(ver[3]).append(ver[4]);
        }
        sb.append('-').append(getLoader().name().toLowerCase());
        return sb.toString();
    }

    boolean isDevelopmentEnvironment();

    Loaders getLoader();

    enum Loaders {
        FORGE,
        FABRIC
    }

    class ActiveMod {
        public final String name;
        public final String id;
        public final String version;
        public final Path source;

        public ActiveMod(String name, String id, String version, Path source) {
            this.name = name;
            this.id = id;
            this.version = version;
            this.source = source;
        }
    }
}
