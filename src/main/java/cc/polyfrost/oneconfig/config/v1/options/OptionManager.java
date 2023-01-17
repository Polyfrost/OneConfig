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

package cc.polyfrost.oneconfig.config.v1.options;

import cc.polyfrost.oneconfig.config.v1.options.type.OptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.options.ButtonOptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.options.IncludeOptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.options.SliderOptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.options.SwitchOptionType;
import cc.polyfrost.oneconfig.config.v1.options.type.options.TextOptionType;

import java.util.HashMap;

/**
 * Manages {@link OptionType}s.
 * To register an {@link OptionType}, use {@link #register(Class, OptionType)}.
 * @see OptionHolder
 * @see OptionType
 */
public class OptionManager {
    public static final OptionManager INSTANCE = new OptionManager();
    private final HashMap<Class<? extends OptionType>, OptionType> OPTIONS = new HashMap<>();

    public OptionManager() {
        register(ButtonOptionType.class, new ButtonOptionType());
        register(IncludeOptionType.class, new IncludeOptionType());
        register(SliderOptionType.class, new SliderOptionType());
        register(SwitchOptionType.class, new SwitchOptionType());
        register(TextOptionType.class, new TextOptionType());
    }

    public OptionType getOption(Class<? extends OptionType> optionClass) {
        return OPTIONS.get(optionClass);
    }

    public OptionType register(Class<? extends OptionType> optionClass, OptionType optionType) {
        return OPTIONS.put(optionClass, optionType);
    }
}
