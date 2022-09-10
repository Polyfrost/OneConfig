/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.platform;

import cc.polyfrost.oneconfig.libs.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public interface LoaderPlatform {
    boolean isModLoaded(String id);

    boolean hasActiveModContainer();

    @Nullable ActiveMod getActiveModContainer();

    @Nullable ActiveMod toActiveMod(@Nullable Object in);

    /**
     * Note: the list may contain null elements
     */
    @NotNull
    List<ActiveMod> getLoadedMods();

    /**
     * Delete the cached list and return it. <br>
     * Note: the list may contain null elements
     */
    @NotNull
    List<ActiveMod> reloadModsList();

    class ActiveMod {
        public final String name;
        public final String id;
        public final String version;
        /** <h2>The path to the mod's jar file.</h2>
         * <b>Forge:</b> Feel free to use {@link Path#toFile()}. <br>
         * <b>Fabric:</b> A path returned by this method may be incompatible with {@link Path#toFile()} as its FileSystem doesn't necessarily represent the OS file system, but potentially a virtual view of jar contents or another abstraction. <br>
         */
        public final Path source;

        public ActiveMod(String name, String id, String version, Path source) {
            this.name = name;
            this.id = id;
            this.version = version;
            this.source = source;
        }
    }
}
