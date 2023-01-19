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

package cc.polyfrost.oneconfig.api.v1.config.option;

import cc.polyfrost.oneconfig.api.v1.config.option.type.OptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.ButtonOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.ColorOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.IncludeOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.SliderOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.BooleanOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.TextOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.dropdown.EnumDropdownOptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.options.dropdown.OrdinalDropdownOptionType;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages {@link OptionType}s.
 * To register an {@link OptionType}, use {@link #register(Class, OptionType)}.
 * @see OptionHolder
 * @see OptionType
 */
public class OptionManager {
    public static final OptionManager INSTANCE = new OptionManager();
    private final Map<Class<? extends OptionType>, OptionType> options = new HashMap<>();

    public OptionManager() {
        register(ButtonOptionType.class, new ButtonOptionType());
        register(ColorOptionType.class, new ColorOptionType());
        register(EnumDropdownOptionType.class, new EnumDropdownOptionType());
        register(IncludeOptionType.class, new IncludeOptionType());
        register(OrdinalDropdownOptionType.class, new OrdinalDropdownOptionType());
        register(SliderOptionType.class, new SliderOptionType());
        register(BooleanOptionType.class, new BooleanOptionType());
        register(TextOptionType.class, new TextOptionType());
    }

    public OptionType getOption(Class<? extends OptionType> optionClass) {
        return options.computeIfAbsent(optionClass, clazz -> {
            System.out.println("OptionManager: Trying to instantiate unknown option type " + clazz.getName());
            try {
                return clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public OptionType register(Class<? extends OptionType> optionClass, OptionType optionType) {
        return options.put(optionClass, optionType);
    }
}
