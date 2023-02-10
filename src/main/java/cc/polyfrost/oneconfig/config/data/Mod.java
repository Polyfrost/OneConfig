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

package cc.polyfrost.oneconfig.config.data;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.elements.OptionPage;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Mod implements Comparable<Mod> {
    @NotNull
    public final String name;
    public final ModType modType;
    @Nullable
    public final String modIcon;
    public final int iconWidth;
    public final int iconHeight;
    @Nullable
    public final Migrator migrator;
    @NotNull
    public final OptionPage defaultPage;
    public Config config;

    /**
     * @param name       Friendly name of the mod
     * @param modType    Type of the mod (for example ModType.QOL)
     * @param modIcon    Path to icon of the mod (png or svg format)
     * @param iconWidth  Width of the icon
     * @param iconHeight Height of the icon
     * @param migrator   Migrator class to port the old config
     */
    public Mod(@NotNull String name, ModType modType, @Nullable String modIcon, int iconWidth, int iconHeight, @Nullable Migrator migrator) {
        this.name = name;
        this.modType = modType;
        this.modIcon = modIcon;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.migrator = migrator;
        this.defaultPage = new OptionPage(name, this);
    }

    /**
     * @param name     Friendly name of the mod
     * @param modType  Type of the mod (for example ModType.QOL)
     * @param modIcon  Path to icon of the mod (png or svg format)
     * @param migrator Migrator class to port the old config
     */
    public Mod(String name, ModType modType, @Nullable String modIcon, Migrator migrator) {
        this(name, modType, modIcon, -1, -1, null);
    }

    /**
     * @param name       Friendly name of the mod
     * @param modType    Type of the mod (for example ModType.QOL)
     * @param modIcon    Path to icon of the mod (png or svg format)
     * @param iconWidth  Width of the icon
     * @param iconHeight Height of the icon
     */
    public Mod(String name, ModType modType, @Nullable String modIcon, int iconWidth, int iconHeight) {
        this(name, modType, modIcon, iconWidth, iconHeight, null);
    }

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     * @param modIcon path to icon of the mod (png or svg format)
     */
    public Mod(String name, ModType modType, @Nullable String modIcon) {
        this(name, modType, modIcon, -1, -1, null);
    }

    /**
     * @param name     Friendly name of the mod
     * @param modType  Type of the mod (for example ModType.QOL)
     * @param migrator Migrator class to port the old config
     */
    public Mod(String name, ModType modType, @Nullable Migrator migrator) {
        this(name, modType, null, -1, -1, migrator);
    }

    /**
     * @param name    Friendly name of the mod
     * @param modType Type of the mod (for example ModType.QOL)
     */
    public Mod(String name, ModType modType) {
        this(name, modType, null, -1, -1, null);
    }

    @Override
    public int compareTo(@NotNull Mod mod) {
        return name.compareTo(mod.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj instanceof Mod) {
            Mod mod = (Mod) obj;
            return name.equals(mod.name);
        } else return false;
    }
}
