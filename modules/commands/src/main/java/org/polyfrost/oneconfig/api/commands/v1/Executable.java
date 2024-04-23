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

package org.polyfrost.oneconfig.api.commands.v1;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandCreationException;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandExecutionException;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class Executable implements Node {
    @Unmodifiable
    public static final Map<Class<?>, Class<?>> primitiveWrappers;

    static {
        Map<Class<?>, Class<?>> m = new HashMap<>(8, 1f);
        m.put(double.class, Double.class);
        m.put(long.class, Long.class);
        m.put(float.class, Float.class);
        m.put(int.class, Integer.class);
        m.put(char.class, Character.class);
        m.put(byte.class, Byte.class);
        m.put(boolean.class, Boolean.class);
        m.put(short.class, Short.class);
        primitiveWrappers = Collections.unmodifiableMap(m);
    }

    public final String[] names;
    public final String description;
    public final Param[] parameters;
    public final int arity;
    public final Function<Object[], Object> function;
    public final boolean isGreedy;

    public Executable(@NotNull String[] names, @Nullable String description, @NotNull Param[] parameters, boolean isGreedy, @NotNull Function<Object[], Object> function) {
        this.names = names;
        this.description = description;
        this.parameters = parameters;
        this.function = function;
        this.isGreedy = isGreedy;
        int arity = 0;
        for (Param p : parameters) {
            arity += p.arity;
        }
        this.arity = arity;
    }

    /**
     * Because casting arrays by default would be stupid!
     */
    private static Object unbox(Object in) {
        if (!(in instanceof Object[])) return in;
        Object out;
        if (in instanceof Number[]) {
            Number[] a = ((Number[]) in);
            Number type = a[0];
            Class<?> c;
            if (type instanceof Float) c = float.class;
            else if (type instanceof Double) c = double.class;
            else if (type instanceof Byte) c = byte.class;
            else if (type instanceof Short) c = short.class;
            else if (type instanceof Integer) c = int.class;
            else if (type instanceof Long) c = long.class;
            else c = null;
            out = Array.newInstance(c, a.length);
            for (int i = 0; i < a.length; i++) {
                Array.set(out, i, a[i]);
            }
        } else if (in instanceof Boolean[]) {
            out = new boolean[((Boolean[]) in).length];
            for (int i = 0; i < ((Boolean[]) in).length; i++) {
                Array.setBoolean(out, i, ((Boolean[]) in)[i]);
            }
        } else if (in instanceof Character[]) {
            out = new char[((Character[]) in).length];
            for (int i = 0; i < ((Character[]) in).length; i++) {
                Array.setChar(out, i, ((Character[]) in)[i]);
            }
        } else out = in;
        return out;
    }

    public Object execute(String... args) {
        if (!isGreedy && args.length != arity) throw new CommandExecutionException("Invalid number of arguments!");
        Object[] parsed = new Object[parameters.length];
        if (arity == 0) return function.apply(parsed);
        int offset = 0;
        for (int i = 0; i < parameters.length; i++) {
            Param p = parameters[i];
            // if this is greedy, and we are doing last param, force the arity of the last parameter to be the remaining
            // number of arguments so it consumes all of them
            if (isGreedy && i == parameters.length - 1) {
                p.arity = args.length - offset;
            }
            parsed[i] = unbox(p.parsed(offset, args));
            offset += p.arity;
        }
        return function.apply(parsed);
    }

    public String[] names() {
        return names;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Executable").append(Arrays.toString(names));
        if (description != null) sb.append(": ").append(description).append(" ").append(Arrays.toString(parameters));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Executable that = (Executable) o;
        return arity == that.arity && Arrays.equals(names, that.names) && Objects.equals(description, that.description) && Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = (description == null) ? 0 : description.hashCode();
        result = 31 * result + arity;
        result = 31 * result + Arrays.hashCode(names);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }

    public String helpString() {
        return String.join(", ", names) + (parameters.length == 0 ? "" : " " + Arrays.toString(parameters)) + (description == null ? "" : ": " + description);
    }

    public static final class Param {
        @NotNull
        public final String name;
        @Nullable
        public final String description;
        @NotNull
        public final ArgumentParser<?> parser;
        public int arity;

        public Param(@NotNull String name, @Nullable String description, int arity, @NotNull ArgumentParser<?> parser) {
            this.name = name;
            this.description = description;
            this.parser = parser;
            this.arity = arity;
        }

        public static Param create(@NotNull String name, @Nullable String description, @NotNull Class<?> type, int arity, @NotNull Map<Class<?>, ArgumentParser<?>> parsers) {
            if (type.isArray()) type = type.getComponentType();
            ArgumentParser<?> parser = parsers.get(primitiveWrappers.getOrDefault(type, type));
            if (parser == null) {
                throw new CommandCreationException("No parser found for type " + type.getSimpleName() + "! Register with CommandManager#registerParser");
            }
            return new Param(name, description, arity, parser);
        }

        Object parsed(int offset, String[] args) {
            if (arity == 1) return parser.parse(args[offset]);
            Object first = parser.parse(args[offset]);
            Object[] parsed = (Object[]) Array.newInstance(first.getClass(), arity);
            parsed[0] = first;
            for (int i = 1; i < arity; i++) {
                parsed[i] = parser.parse(args[offset + i]);
            }
            return parsed;
        }

        Object parsedOrNull(int offset, String[] args) {
            try {
                return parsed(offset, args);
            } catch (Exception ignored) {
                return null;
            }
        }

        @Nullable
        List<@NotNull String> tryAutoComplete(String arg) {
            return parser.getAutoCompletions(arg);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (description != null) sb.append(": ").append(description);
            return sb.toString();
        }

        public Class<?> getType() {
            return parser.getType();
        }
    }
}


