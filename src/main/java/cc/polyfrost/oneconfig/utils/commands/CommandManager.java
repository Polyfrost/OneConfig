/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost and Pinkulu.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 * Co-author: Pinkulu <pinkulumc@gmail.com> <https://github.com/pinkulu>
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

package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import cc.polyfrost.oneconfig.utils.commands.arguments.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Handles the registration of OneConfig commands.
 *
 * @see Command
 */
public class CommandManager {
    private static final PlatformCommandManager platform = ServiceLoader.load(PlatformCommandManager.class, PlatformCommandManager.class.getClassLoader()).iterator().next();
    public static final CommandManager INSTANCE = new CommandManager();
    static final String NOT_FOUND_TEXT = "Command not found! Type /@ROOT_COMMAND@ help for help.";
    static final String TOO_MANY_PARAMETERS = "There were too many / little parameters for this command! Type /@ROOT_COMMAND@ help for help.";
    static final String METHOD_RUN_ERROR = "Error while running @ROOT_COMMAND@ method! Please report this to the developer.";
    final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

    private CommandManager() {
        addParser(new StringParser());
        addParser(new IntegerParser());
        addParser(new IntegerParser(), Integer.TYPE);
        addParser(new DoubleParser());
        addParser(new DoubleParser(), Double.TYPE);
        addParser(new FloatParser());
        addParser(new FloatParser(), Float.TYPE);
        addParser(new BooleanParser());
        addParser(new BooleanParser(), Boolean.TYPE);
    }

    /**
     * Adds a parser to the parsers map.
     *
     * @param parser The parser to add.
     * @param clazz  The class of the parser.
     */
    public void addParser(ArgumentParser<?> parser, Class<?> clazz) {
        parsers.put(clazz, parser);
    }

    /**
     * Adds a parser to the parsers map.
     *
     * @param parser The parser to add.
     */
    public void addParser(ArgumentParser<?> parser) {
        addParser(parser, parser.typeClass);
    }

    /**
     * Registers the provided command.
     *
     * @param clazz The command to register as a class.
     */
    public void registerCommand(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Command.class)) {
            final Command annotation = clazz.getAnnotation(Command.class);

            final InternalCommand root = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description().trim().isEmpty()
                    ? "Main command for " + annotation.value() : annotation.description(), annotation.color(),  null);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Main.class) && method.getParameterCount() == 0) {
                    root.invokers.add(new InternalCommand.InternalCommandInvoker(annotation.value(), annotation.aliases(), method, root));
                    break;
                }
            }
            addToInvokers(clazz.getDeclaredClasses(), root);
            platform.createCommand(root, annotation);
        }
    }

    private void addToInvokers(Class<?>[] classes, InternalCommand parent) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(SubCommand.class)) {
                SubCommand annotation = clazz.getAnnotation(SubCommand.class);
                InternalCommand command = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description(), annotation.color() == ChatColor.RESET ? parent.color : annotation.color(), parent);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Main.class)) {
                        command.invokers.add(new InternalCommand.InternalCommandInvoker(annotation.value(), annotation.aliases(), method, command));
                    }
                }
                parent.children.add(command);
                addToInvokers(clazz.getDeclaredClasses(), command);
            }
        }
    }

    static class CustomError {
        public String message;

        public CustomError(String message) {
            this.message = message;
        }
    }

    static class InternalCommand {
        public final String name;
        public final String[] aliases;
        public final String description;
        public final ChatColor color;
        public final ArrayList<InternalCommandInvoker> invokers = new ArrayList<>();
        public final InternalCommand parent;
        public final ArrayList<InternalCommand> children = new ArrayList<>();

        public InternalCommand(String name, String[] aliases, String description, ChatColor color, InternalCommand parent) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.parent = parent;
            this.color = color;
        }

        public boolean isValid(String name, boolean tabCompletion) {
            String lowerCaseName = this.name.toLowerCase(Locale.ENGLISH);
            String lowerCaseOtherName = name.toLowerCase(Locale.ENGLISH);
            if (!tabCompletion ? lowerCaseName.equals(lowerCaseOtherName) : lowerCaseName.startsWith(lowerCaseOtherName)) {
                return true;
            } else {
                for (String alias : aliases) {
                    String lowerCaseAlias = alias.toLowerCase(Locale.ENGLISH);
                    if (!tabCompletion ? lowerCaseAlias.equals(lowerCaseOtherName) : lowerCaseAlias.startsWith(lowerCaseOtherName)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "InternalCommand{" +
                    "name='" + name + '\'' +
                    ", aliases=" + Arrays.toString(aliases) +
                    ", description='" + description + '\'' +
                    ", invokers=" + invokers +
                    '}';
        }

        public static class InternalCommandInvoker {
            public final String name;
            public final String[] aliases;
            public final Method method;
            public final Parameter[] parameterTypes;
            public final InternalCommand parent;

            public InternalCommandInvoker(String name, String[] aliases, Method method, InternalCommand parent) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("All command methods must be static!");
                }
                this.name = name;
                this.aliases = aliases;
                this.method = method;
                this.parameterTypes = method.getParameters().clone();
                this.parent = parent;
                if (Modifier.isPrivate(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                    method.setAccessible(true);
                }
            }

            @Override
            public String toString() {
                return "InternalCommandInvoker{" +
                        "name='" + name + '\'' +
                        ", aliases=" + Arrays.toString(aliases) +
                        ", method=" + method +
                        ", parameterTypes=" + Arrays.toString(parameterTypes) +
                        '}';
            }
        }
    }
}

