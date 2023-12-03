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

package cc.polyfrost.oneconfig.internal.plugin.hooks;

import cc.polyfrost.oneconfig.internal.config.compatibility.vigilance.VigilanceConfig;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.platform.Platform;
import gg.essential.vigilance.Vigilant;
import kotlin.text.StringsKt;

import java.io.File;

@SuppressWarnings("unused")
public class VigilantHook {
    public static VigilanceConfig returnNewConfig(Vigilant vigilant, File file) {
        if (vigilant != null && Platform.getInstance().isCallingFromMinecraftThread()) {
            String name = !vigilant.getGuiTitle().equals("Settings") ? vigilant.getGuiTitle() : !Platform.getLoaderPlatform().hasActiveModContainer() ? "Unknown" : Platform.getLoaderPlatform().getActiveModContainer().name;
            if (StringsKt.isBlank(name)) return null;
            if (name.equals("OneConfig")) name = "Essential";
            if (name.equals("Patcher")) return null;
            String finalName = ChatColor.Companion.stripControlCodes(name);
            // duplicate fix
            if (ConfigCore.mods.stream().anyMatch(mod -> mod.name.equals(finalName))) return null;
            return new VigilanceConfig(new Mod(finalName, ModType.THIRD_PARTY), file.getAbsolutePath(), vigilant);
        } else {
            return null;
        }
    }
}
