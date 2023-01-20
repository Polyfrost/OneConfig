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

package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.*;

/**
 * A class used to create a parser for a given String. Some examples can be found in this class.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class ArgumentParser<T> {
    private final TypeToken<T> type = new TypeToken<T>(getClass()) {
    };
    public final Class<?> typeClass = type.getRawType();

    /**
     * Parses the given string into an object of the type specified by this parser.
     * Should return a relevant exception if it cannot be parsed. <b>The exception's name is presented to the user if they input a bad argument.</b>
     *
     * @param arg The string to parse.
     * @return The parsed object, or an exception if it fails
     * @throws Exception a <b>RELEVANT</b> exception if the parsing fails
     */
    @Nullable
    public abstract T parse(@NotNull String arg) throws Exception;

    /**
     * Returns possible completions for the given argument.
     * Should return an empty list if no completions are possible.
     *
     * @param current The argument's current state.
     * @param parameter The parameter this argument is for. Can be used to get any annotations on the parameter.
     * @return A list of possible completions, or an empty list if no completions are possible.
     */
    @NotNull
    public List<String> complete(String current, Parameter parameter) {
        return Collections.emptyList();
    }


    // DEFAULT PARSERS
    public static class DoubleParser extends ArgumentParser<Double> {
        @Nullable
        @Override
        public Double parse(@NotNull String arg) {
            return Double.parseDouble(arg);
        }
    }

    public static class IntegerParser extends ArgumentParser<Integer> {
        @Nullable
        @Override
        public Integer parse(@NotNull String arg) {
            return Integer.parseInt(arg);
        }
    }

    public static class FloatParser extends ArgumentParser<Float> {
        @Nullable
        @Override
        public Float parse(@NotNull String arg) {
            return Float.parseFloat(arg);
        }
    }

    public static class StringParser extends ArgumentParser<String> {
        @Nullable
        @Override
        public String parse(@NotNull String arg) {
            return arg;
        }
    }

    public static class BooleanParser extends ArgumentParser<Boolean> {
        private static final Map<String, List<String>> VALUES =
                Maps.newHashMap();

        static {
            VALUES.put("true", Lists.newArrayList("on", "yes", "y", "enabled", "enable", "1"));
            VALUES.put("false", Lists.newArrayList("off", "no", "n", "disabled", "disable", "0"));
        }

        @Override
        public @Nullable Boolean parse(@NotNull String s) {
            return Boolean.parseBoolean(
                    VALUES.entrySet().stream()
                            .filter(it -> it.getKey().equalsIgnoreCase(s)
                                    || it.getValue().contains(s.toLowerCase()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    s + " is not any of: "
                                            + String.join(", ", VALUES.keySet())
                            ))
            );
        }

        @NotNull
        @Override
        public List<String> complete(String current, Parameter parameter) {
            if (current != null && !current.trim().isEmpty()) {
                for (String v : VALUES.keySet()) {
                    if (v.startsWith(current.toLowerCase(Locale.ENGLISH))) {
                        return Lists.newArrayList(v);
                    }
                }
            }
            return new ArrayList<>(VALUES.keySet());
        }
    }
}
