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

package org.polyfrost.oneconfig.api.commands.factories.builder;

import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.Executable;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Java API for creating commands using a builder pattern.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CommandBuilder {
    final Map<Class<?>, ArgumentParser<?>> parsers;
    final CommandTree tree;
    final CommandBuilder parent;

    public CommandBuilder(CommandBuilder parent, Map<Class<?>, ArgumentParser<?>> parsers, String... aliases) {
        this.parsers = parsers;
        this.parent = parent;
        this.tree = new CommandTree(aliases, null);
    }

    public CommandBuilder(Map<Class<?>, ArgumentParser<?>> parsers, String... aliases) {
        this(null, parsers, aliases);
    }


    public CommandBuilder subcommand(String... aliases) {
        CommandBuilder b = new CommandBuilder(this, this.parsers, aliases);
        tree.put(b.tree);
        return b;
    }

    public CommandBuilder subcmd(String... aliases) {
        return subcommand(aliases);
    }

    public CommandBuilder then(ExeBuilder exe) {
        Executable.Param[] params = new Executable.Param[exe.args.size()];
        for (int i = 0; i < exe.args.size(); i++) {
            Arg arg = exe.args.get(i);
            params[i] = Executable.Param.create(arg.name, arg.description, arg.type, arg.arity, parsers);
        }
        tree.put(new Executable(exe.aliases.toArray(new String[0]), exe.description, params, exe.greedy, exe.function));
        return this;
    }

    public CommandBuilder description(String description) {
        tree.setDescription(description);
        return this;
    }

    public static ExeBuilder runs(String... aliases) {
        return new ExeBuilder(aliases);
    }

    public static CommandBuilder command(Map<Class<?>, ArgumentParser<?>> parsers, String... aliases) {
        return new CommandBuilder(parsers, aliases);
    }

    public static final class ExeBuilder {
        Set<String> aliases = new HashSet<>();
        String description = null;

        Function<Object[], Object> function;

        List<Arg> args = new ArrayList<>();

        boolean greedy = false;

        ExeBuilder(String... aliases) {
            this.alias(aliases);
        }

        public ExeBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ExeBuilder does(Runnable function) {
            this.function = args -> {
                function.run();
                return null;
            };
            return this;
        }

        public ExeBuilder does(Function<Object[], Object> function) {
            this.function = function;
            return this;
        }

        public ExeBuilder greedy() {
            this.greedy = true;
            return this;
        }

        public ExeBuilder does(Consumer<Object[]> function) {
            this.function = args -> {
                function.accept(args);
                return null;
            };
            return this;
        }

        public ExeBuilder alias(String... aliases) {
            this.aliases.addAll(Arrays.asList(aliases));
            return this;
        }

        public ExeBuilder with(Arg... arg) {
            this.args.addAll(Arrays.asList(arg));
            return this;
        }

    }

    public static final class Arg {
        final String name;
        final String description;
        final Class<?> type;
        final int arity;

        public Arg(String name, String description, int arity, Class<?> type) {
            this.name = name;
            this.description = description;
            this.arity = arity;
            this.type = type;
        }

        public static int getInt(Object o) {
            return (int) o;
        }

        public static long getLong(Object o) {
            return (long) o;
        }

        public static float getFloat(Object o) {
            return (float) o;
        }

        public static double getDouble(Object o) {
            return (double) o;
        }

        public static boolean getBoolean(Object o) {
            return (boolean) o;
        }

        public static char getChar(Object o) {
            return (char) o;
        }

        public static byte getByte(Object o) {
            return (byte) o;
        }

        public static short getShort(Object o) {
            return (short) o;
        }

        @SuppressWarnings("unchecked")
        public static <T> T get(Object o) {
            return (T) o;
        }

        public static String getString(Object o) {
            return (String) o;
        }
    }
}
