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

package cc.polyfrost.oneconfig.internal.config.compatibility.forge;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.migration.Migrator;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class ForgeCompat {
    public static final HashMap<Mod, Runnable> compatMods = new HashMap<>();

    public static class ForgeCompatMod extends Mod {

        private ForgeCompatMod(String name, ModType modType, @Nullable String modIcon, @Nullable Migrator migrator) {
            super(name, modType, modIcon, migrator);
            config = new Config(this, "") {
                @Override
                public void initialize() {
                }

                @Override
                public void save() {
                }

                @Override
                public void load() {
                }

                @Override
                public void openGui() {
                    compatMods.get(mod).run();
                }

                @Override
                public boolean supportsProfiles() {
                    return false;
                }
            };
        }

        public ForgeCompatMod(String name, ModType modType) {
            this(name, modType, null, null);
        }
    }
}
