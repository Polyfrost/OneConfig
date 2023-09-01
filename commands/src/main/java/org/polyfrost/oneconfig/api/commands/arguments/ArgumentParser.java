/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.api.commands.arguments;

import org.jetbrains.annotations.NotNull;

/**
 * A parser for command arguments, which takes a String and returns the parsed object as the correct type.
 * <br>
 * Implementations of this interface should be registered with {@link CommandManager#registerParser(ArgumentParser)}. <br>
 * <b>Only one parser should only ever be registered for a given type!</b> This is to avoid ambiguity when parsing arguments. <br>
 */
public interface ArgumentParser<T> {
    /**
     * Parse the given argument into the correct type.
     *
     * @return the parsed object
     */
    T parse(@NotNull String arg);

    /**
     * Check if this parser can parse the given type. <br>
     * This type should also be the same type that is returned by {@link #parse(String)}.
     *
     * @param type the type to check
     * @return true if this parser can parse the given type, false otherwise
     */
    boolean canParse(Class<?> type);


    ArgumentParser<Double> doubleParser = new ArgumentParser<Double>() {
        @Override
        public @NotNull Double parse(@NotNull String arg) {
            return Double.parseDouble(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == double.class || type == Double.class;
        }
    };

    ArgumentParser<Float> floatParser = new ArgumentParser<Float>() {
        @Override
        public @NotNull Float parse(@NotNull String arg) {
            return Float.parseFloat(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == float.class || type == Float.class;
        }
    };

    ArgumentParser<Integer> intParser = new ArgumentParser<Integer>() {
        @Override
        public @NotNull Integer parse(@NotNull String arg) {
            return Integer.parseInt(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == int.class || type == Integer.class;
        }
    };

    ArgumentParser<Long> longParser = new ArgumentParser<Long>() {
        @Override
        public @NotNull Long parse(@NotNull String arg) {
            return Long.parseLong(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == long.class || type == Long.class;
        }
    };

    ArgumentParser<Short> shortParser = new ArgumentParser<Short>() {
        @Override
        public @NotNull Short parse(@NotNull String arg) {
            return Short.parseShort(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == short.class || type == Short.class;
        }
    };

    ArgumentParser<Byte> byteParser = new ArgumentParser<Byte>() {
        @Override
        public @NotNull Byte parse(@NotNull String arg) {
            return Byte.parseByte(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == byte.class || type == Byte.class;
        }
    };

    ArgumentParser<Boolean> booleanParser = new ArgumentParser<Boolean>() {
        @Override
        public @NotNull Boolean parse(@NotNull String arg) {
            return Boolean.parseBoolean(arg);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == boolean.class || type == Boolean.class;
        }
    };

    ArgumentParser<String> stringParser = new ArgumentParser<String>() {
        @Override
        public @NotNull String parse(@NotNull String arg) {
            return arg;
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == String.class;
        }
    };

    ArgumentParser<Character> charParser = new ArgumentParser<Character>() {
        @Override
        public @NotNull Character parse(@NotNull String arg) {
            return arg.charAt(0);
        }

        @Override
        public boolean canParse(Class<?> type) {
            return type == char.class || type == Character.class;
        }
    };

    ArgumentParser<?>[] defaultParsers = {
            doubleParser,
            floatParser,
            intParser,
            longParser,
            shortParser,
            byteParser,
            booleanParser,
            charParser,
            stringParser
    };
}
