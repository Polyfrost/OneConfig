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

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
//? if >= 26.2 {
import net.minecraft.locale.Language;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.polyfrost.oneconfig.api.platform.v1.I18nPlatform;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.internal.ComponentUtil;

public class I18nPlatformImpl implements I18nPlatform {
    @Override
    public Object translate(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, args);
    }

    @Override
    public String translateString(String key, Object... args) {
        return I18n.get(key, args);
    }

    @Override
    public boolean hasTranslation(String key) {
        //? if >= 26.2 {
        return Language.getInstance().has(key);
        //?} else {
        /*return I18n.exists(key);
        *///?}
    }

    @Override
    public String getUnformattedText(Object component) {
        String s = switch (component) {
            case Component component1 -> component1.getString();
            case FormattedText text -> {
                var componentUtil = ComponentUtil.toStringFormattedTextContentConsumer();
                text.visit(componentUtil.getFirst());
                yield componentUtil.getSecond().invoke();
            }
            case FormattedCharSequence sequence -> {
                var componentUtil = ComponentUtil.toComponentCharSink();
                sequence.accept(componentUtil.getFirst());
                yield componentUtil.getSecond().invoke().getString();
            }
            case net.kyori.adventure.text.Component adventure -> Platform.compatibility().resolveComponent(adventure);
            default -> component.toString();
        };
        return ChatFormatting.stripFormatting(s);
    }
}
