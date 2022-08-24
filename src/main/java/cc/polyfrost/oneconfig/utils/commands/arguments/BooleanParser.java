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

package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Locale;

public class BooleanParser extends ArgumentParser<Boolean> {

    private static final List<String> VALUES = Lists.newArrayList("true", "false");

    @Override
    public @Nullable Boolean parse(Arguments arguments) {
        String next = arguments.poll();
        if (next.equalsIgnoreCase("true")) {
            return true;
        } else if (next.equalsIgnoreCase("false")) {
            return false;
        } else {
            return null;
        }
    }

    @Override
    public @Nullable List<String> complete(Arguments arguments, Parameter parameter) {
        String value = arguments.poll();
        if (value != null && !value.trim().isEmpty()) {
            for (String v : VALUES) {
                if (v.startsWith(value.toLowerCase(Locale.ENGLISH))) {
                    return Lists.newArrayList(v);
                }
            }
        }
        return VALUES;
    }
}
