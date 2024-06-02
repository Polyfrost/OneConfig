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

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.polyfrost.oneconfig.api.platform.v1.I18nPlatform;

public class I18nPlatformImpl implements I18nPlatform {

    @Override
    public String format(String key, Object... args) {
        return I18n.format(key, args);
    }

    @Override
    public String getKeyName(int key, int scanCode) {
        //@formatter:off
        String s =
            //#if MC>=11600
            //$$ net.minecraft.client.util.InputMappings.getInputByCode(key, scanCode).toString();
            //#else
            net.minecraft.client.settings.GameSettings.getKeyDisplayString(key);
            //#endif
        //@formatter:on
        if (s == null) return "Unknown";
        else return s.length() == 1 ? s.toUpperCase() : s;
    }

    @Override
    public String getUnformattedText(Object component) {
        String s;
        if (component instanceof IChatComponent) {
            //noinspection StringOperationCanBeSimplified
            s = ((IChatComponent) component).getUnformattedText().toString();
        } else s = component.toString();
        return EnumChatFormatting.getTextWithoutFormattingCodes(s);
    }
}
