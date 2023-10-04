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

package org.polyfrost.oneconfig.api.commands.factories.annotated;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.Executable;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.exceptions.CommandCreationException;
import org.polyfrost.oneconfig.api.commands.exceptions.CommandExecutionException;
import org.polyfrost.oneconfig.api.commands.exceptions.WrongArgumentsException;
import org.polyfrost.oneconfig.api.commands.factories.CommandFactory;
import org.polyfrost.oneconfig.api.commands.factories.annotated.annotations.Command;
import org.polyfrost.oneconfig.api.commands.factories.annotated.annotations.Parameter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class AnnotationCommandFactory implements CommandFactory {
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    @Override
    public CommandTree create(@NotNull List<ArgumentParser<?>> parsers, @NotNull Object obj) {
        if (!obj.getClass().isAnnotationPresent(Command.class))
            return null;
        Command c = obj.getClass().getAnnotation(Command.class);
        CommandTree tree = new CommandTree(c.value().length == 0 ? new String[]{obj.getClass().getSimpleName()} : c.value(), c.description().isEmpty() ? null : c.description());
        create(parsers, tree, obj);
        return tree;
    }

    static void create(List<ArgumentParser<?>> parsers, CommandTree tree, Object it) {
        tree.putAll(Arrays.stream(it.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Command.class)).map(m -> map(it, m, parsers)));
        for (Class<?> cls : it.getClass().getDeclaredClasses()) {
            if (cls.isAnnotationPresent(Command.class)) {
                Command c = cls.getAnnotation(Command.class);
                CommandTree sub = new CommandTree(c.value().length == 0 ? new String[]{cls.getSimpleName()} : c.value(), c.description().isEmpty() ? null : c.description());
                create(parsers, sub, createInstance(cls, it));
                tree.put(sub);
            }
        }
    }

    static Object createInstance(Class<?> cls, Object parent) {
        try {
            if (Modifier.isStatic(cls.getModifiers())) {
                Constructor<?> constructor = cls.getDeclaredConstructor();
                if (!constructor.isAccessible()) constructor.setAccessible(true);
                return constructor.newInstance();
            } else {
                Constructor<?> constructor = cls.getDeclaredConstructor(parent.getClass());
                if (!constructor.isAccessible()) constructor.setAccessible(true);
                return constructor.newInstance(parent);
            }
        } catch (Exception e) {
            throw new CommandCreationException("Error while creating subcommand!", e);
        }
    }

    static Executable map(Object it, Method method, List<ArgumentParser<?>> parsers) {
        Command c;
        MethodHandle m;
        try {
            if (!method.isAccessible()) method.setAccessible(true);
            c = method.getAnnotation(Command.class);
            m = lookup.unreflect(method);
            if (!Modifier.isStatic(method.getModifiers())) m = m.bindTo(it);
        } catch (Exception e) {
            throw new CommandCreationException("Error while creating command!", e);
        }

        String[] names = c.value();
        if (names.length == 0) names = new String[]{method.getName()};

        return new Executable(names, c.description().isEmpty() ? null : c.description(), createParameterInfos(method, parsers), c.greedy(), getFunction(m));
    }

    @Contract(pure = true)
    static @NotNull Function<Object[], Object> getFunction(MethodHandle mh) {
        return args -> {
            try {
                return mh.invokeWithArguments(args);
            } catch (WrongMethodTypeException | ClassCastException e) {
                // should've been caught earlier
                throw new WrongArgumentsException("InternalError while executing command: CommandTree method argument mismatch!", e);
            } catch (Throwable throwable) {
                throw new CommandExecutionException("Error while executing command!", throwable);
            }
        };
    }

    static Executable.Param createParameterInfo(java.lang.reflect.Parameter parameter, List<ArgumentParser<?>> parsers) {
        String name = "";
        String desc = "";
        int arity = 1;
        if (parameter.isAnnotationPresent(Parameter.class)) {
            Parameter p = parameter.getAnnotation(Parameter.class);
            name = p.value();
            desc = p.description();
            arity = p.arity();
        }
        if (name.isEmpty()) name = parameter.getType().getSimpleName();
        return Executable.Param.create(name, desc.isEmpty() ? null : desc, parameter.getType(), arity, parsers);
    }

    static Executable.Param[] createParameterInfos(Method method, List<ArgumentParser<?>> parsers) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Executable.Param[] infos = new Executable.Param[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            infos[i] = createParameterInfo(parameters[i], parsers);
        }
        return infos;
    }
}
