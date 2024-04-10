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

package org.polyfrost.oneconfig.api.commands.factories.annotated;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.Executable;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.exceptions.WrongArgumentsException;
import org.polyfrost.oneconfig.api.commands.factories.CommandFactory;
import org.polyfrost.oneconfig.api.commands.factories.annotated.annotations.Command;
import org.polyfrost.oneconfig.api.commands.factories.annotated.annotations.Parameter;
import org.polyfrost.oneconfig.utils.MHUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public class AnnotationCommandFactory implements CommandFactory {

    @Override
    public CommandTree create(@NotNull Map<Class<?>, ArgumentParser<?>> parsers, @NotNull Object obj) {
        Command c = obj.getClass().getAnnotation(Command.class);
        if (c == null) return null;
        CommandTree tree = new CommandTree(c.value().length == 0 ? new String[]{obj.getClass().getSimpleName()} : c.value(), c.description().isEmpty() ? null : c.description());
        create(parsers, tree, obj);
        return tree;
    }

    static void create(Map<Class<?>, ArgumentParser<?>> parsers, CommandTree tree, Object it) {
        Arrays.stream(it.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(Command.class)).map(m -> map(it, m, parsers)).forEach(tree::put);
        for (Class<?> cls : it.getClass().getDeclaredClasses()) {
            if (cls.isAnnotationPresent(Command.class)) {
                Command c = cls.getAnnotation(Command.class);
                CommandTree sub = new CommandTree(c.value().length == 0 ? new String[]{cls.getSimpleName()} : c.value(), c.description().isEmpty() ? null : c.description());
                Object instance = MHUtils.instantiate(cls, false).getOrThrow();
                create(parsers, sub, instance);
                tree.put(sub);
            }
        }
    }

    static Executable map(Object it, Method method, Map<Class<?>, ArgumentParser<?>> parsers) {
        Command c = method.getAnnotation(Command.class);
        MethodHandle m = MHUtils.getMethodHandle(method, it).getOrThrow();
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
            } catch (Throwable e) {
                throw sneakyThrow(e);
            }
        };
    }

    public static RuntimeException sneakyThrow(Throwable t) {
        return sneakyThrow0(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T sneakyThrow0(Throwable t) throws T {
        throw (T)t;
    }


    static Executable.Param createParameterInfo(java.lang.reflect.Parameter parameter, Map<Class<?>, ArgumentParser<?>> parsers) {
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

    static Executable.Param[] createParameterInfos(Method method, Map<Class<?>, ArgumentParser<?>> parsers) {
        java.lang.reflect.Parameter[] parameters = method.getParameters();
        Executable.Param[] infos = new Executable.Param[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            infos[i] = createParameterInfo(parameters[i], parsers);
        }
        return infos;
    }
}
