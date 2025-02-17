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

package cc.polyfrost.oneconfig.internal.config.core;

import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.SubMainConfig;
import cc.polyfrost.oneconfig.internal.hud.HudCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigCore {
    public static List<Mod> mods = new ArrayList<>();
    public static HashMap<Mod, List<Mod>> subMods = new HashMap<>();

    public static void saveAll() {
        for (Mod modData : mods) {
            modData.config.save();
        }
    }

    public static void reInitAll() {
        for (Mod modData : mods) {
            if (!modData.config.supportsProfiles()) continue;
            modData.config.reInitialize();
        }
        HudCore.reInitHuds();
        KeyBindHandler.INSTANCE.reInitKeyBinds();
    }

    public static void sortMods() {
        List<Mod> mods = new ArrayList<>(ConfigCore.mods);
        ConfigCore.mods = mods.stream().filter((mod -> mod.config instanceof SubMainConfig)).sorted().collect(Collectors.toList());
        mods.removeAll(ConfigCore.mods);
        ConfigCore.mods.addAll(mods.stream().filter((mod -> OneConfigConfig.favoriteMods.contains(mod.name))).sorted().collect(Collectors.toList()));
        mods.removeAll(ConfigCore.mods);
        ConfigCore.mods.addAll(mods.stream().filter(mod -> mod.modType != ModType.THIRD_PARTY).sorted().collect(Collectors.toList()));
        mods.removeAll(ConfigCore.mods);
        ConfigCore.mods.addAll(mods.stream().sorted().collect(Collectors.toList()));
        OneConfigConfig.getInstance().save();
    }

    public static Mod getParentMod(Mod mod) {
        for (Map.Entry<Mod, List<Mod>> entry : subMods.entrySet()) {
            if (entry.getValue().contains(mod)) return entry.getKey();
        }
        return null;
    }
}
