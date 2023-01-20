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

import cc.polyfrost.oneconfig.platform.I18nPlatform;
import net.minecraft.client.resources.I18n;

public class I18nPlatformImpl implements I18nPlatform {

    @Override
    public String format(String key, Object... args) {
        return I18n.format(key, args);
    }

    @Override
    public String getKeyName(int key, int scanCode) {
        //#if MC>=11600
            //#if FABRIC==1
            //$$ final String s = net.minecraft.client.util.InputUtil.fromKeyCode(key, scanCode).getLocalizedText().asString();
            //#else
            //$$ final String s = net.minecraft.client.util.InputMappings.getInputByCode(key, scanCode).func_237520_d_().getString();
            //#endif
            //$$ if (s == null) return "Unknown";
            //$$ else return s.length() == 1 ? s.toUpperCase() : s;
        //#else
        final String s = net.minecraft.client.settings.GameSettings.getKeyDisplayString(key);
        return s == null ? "Unknown" : s;
        //#endif
    }
}
