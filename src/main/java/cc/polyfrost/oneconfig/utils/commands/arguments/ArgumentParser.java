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

import com.google.common.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class ArgumentParser<T> {
    private final TypeToken<T> type = new TypeToken<T>(getClass()) {
    };
    public final Class<?> typeClass = type.getRawType();

    /**
     * Parses the given string into an object of the type specified by this parser.
     * Should return null if the string cannot be parsed.
     *
     * @param arguments The string to parse.
     * @return The parsed object, or null if the string cannot be parsed.
     */
    @Nullable
    public abstract T parse(Arguments arguments);

    /**
     * Returns possible completions for the given arguments.
     * Should return an empty list or null if no completions are possible.
     *
     * @param arguments The arguments to complete.
     * @param parameter The parameter to complete.
     * @return A list of possible completions, or an empty list or null if no completions are possible.
     */
    @Nullable
    public List<String> complete(Arguments arguments, Parameter parameter) {
        return Collections.emptyList();
    }
}
