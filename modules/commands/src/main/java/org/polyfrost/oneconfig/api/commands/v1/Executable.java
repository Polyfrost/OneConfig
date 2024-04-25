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
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandCreationException;
import org.polyfrost.oneconfig.api.commands.v1.exceptions.CommandExecutionException;
import org.polyfrost.oneconfig.utils.v1.WrappingUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.polyfrost.oneconfig.utils.v1.WrappingUtils.unbox;

public class Executable extends Node {
    public final Param[] parameters;
    public final int arity;
    public final Function<Object[], Object> function;
    public final boolean isGreedy;

    public Executable(@NotNull String[] names, @Nullable String description, @NotNull Param[] parameters, boolean isGreedy, @NotNull Function<Object[], Object> function) {
        super(names, description);
        this.parameters = parameters;
        this.function = function;
        this.isGreedy = isGreedy;
        int arity = 0;
        for (Param p : parameters) {
            arity += p.arity;
        }
        this.arity = arity;
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

    @Override
    public boolean equals(Object o) {
        if (super.equals(o)) return true;
        if (getClass() != o.getClass()) return false;
        Executable that = (Executable) o;
        return arity == that.arity && Arrays.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + arity * 31;
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }

    @Override
    public String toString() {
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
            ArgumentParser<?> parser = parsers.get(WrappingUtils.getWrapped(type));
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


