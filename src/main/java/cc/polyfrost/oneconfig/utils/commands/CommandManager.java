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
import cc.polyfrost.oneconfig.utils.commands.annotations.*;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the registration of OneConfig commands.
 *
 * @see Command
 */
public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();
    static final PlatformCommandManager platform = ServiceLoader.load(PlatformCommandManager.class, PlatformCommandManager.class.getClassLoader()).iterator().next();
    static final String NOT_FOUND_TEXT = "Command not found! Type /@ROOT_COMMAND@ help for help.";
    static final String NOT_FOUND_HELP_TEXT = "Help for this command was not found! Type /@ROOT_COMMAND@ help for generic help first.";
    static final String METHOD_RUN_ERROR = "Error while running @ROOT_COMMAND@ method! Please report this to the developer.";
    /**
     * <a href="https://https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt">UTF-8 Stress Test</a> Character 2.3.1 (used because it's never going to be used in theory)
     */
    static final String DELIMITER = "\uD7FF";
    private final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();
    // so that no one can name a method this
    static final String MAIN_METHOD_NAME = "MAIN" + DELIMITER + DELIMITER + "MAIN";

    private CommandManager() {
        addParser(new ArgumentParser.StringParser());
        addParser(new ArgumentParser.IntegerParser());
        addParser(new ArgumentParser.IntegerParser(), Integer.TYPE);
        addParser(new ArgumentParser.DoubleParser());
        addParser(new ArgumentParser.DoubleParser(), Double.TYPE);
        addParser(new ArgumentParser.FloatParser());
        addParser(new ArgumentParser.FloatParser(), Float.TYPE);
        addParser(new ArgumentParser.BooleanParser());
        addParser(new ArgumentParser.BooleanParser(), Boolean.TYPE);
    }

    /**
     * Adds a parser to the parsers map.
     *
     * @param parser The parser to add.
     * @param cls    The class of the parser.
     */
    public void addParser(ArgumentParser<?> parser, Class<?> cls) {
        parsers.put(cls, parser);
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
     * @param obj the command to register (must be an instance of a class annotated with @Command).
     */
    public void registerCommand(Object obj) {
        platform.createCommand(new OCCommand(obj));
    }

    /**
     * Registers the provided command. <b>Deprecated!</b>
     *
     * @param cls the command to register as a class.
     * @deprecated <b>Replace with {@link #registerCommand(Object)} aka. {@code new YourCommand()}</b>
     */
    @Deprecated
    public void registerCommand(Class<?> cls) {
        try {
            registerCommand(cls.newInstance());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Legacy support failed, " +
                    "Replace #registerCommand(YourCommand.class) with #registerCommand(new YourCommand())");
        }
    }

    /**
     * Turn an inner class into an Object instance.
     */
    private static Object createIsnOf(Class<?> cls, Object parent) {
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
            throw new IllegalStateException("Error while creating subcommand!", e);
        }
    }

    /**
     * Internal class for command handling.
     */
    protected class OCCommand {
        final TreeMap<String, InternalCommand> commandsMap = new TreeMap<>();
        final String[] helpCommand;
        private final Command meta;
        InternalCommand mainMethod;

        private OCCommand(@NotNull Object commandIsn) {
            Class<?> cls = commandIsn.getClass();
            if (cls.isAnnotationPresent(Command.class)) {
                meta = cls.getAnnotation(Command.class);

                for (Method method : cls.getDeclaredMethods()) {
                    if (!method.isAccessible()) method.setAccessible(true);
                    create("", commandIsn, method, false);
                }

                for (Class<?> subcommand : cls.getDeclaredClasses()) {
                    if (!subcommand.isAnnotationPresent(SubCommandGroup.class)) continue;
                    walk("", createIsnOf(subcommand, commandIsn), false);
                }

                if (meta.customHelpMessage().length == 0) helpCommand = genHelpCommand();
                else helpCommand = meta.customHelpMessage();

            } else {
                throw new IllegalArgumentException("Master command class " + cls.getSimpleName() + " is not annotated with @Command!");
            }
        }

        /**
         * Turn a method into a InternalCommand and add it to the map.
         */
        private void create(String path, Object parent, @NotNull Method method, boolean isAlias) {
            if (parent.getClass().equals(Class.class)) return;
            if (!method.isAccessible()) method.setAccessible(true);
            if (!method.isAnnotationPresent(SubCommand.class)) {
                if (method.isAnnotationPresent(Main.class)) {
                    if (method.getParameterCount() != 0)
                        throw new IllegalArgumentException("Method " + method.getName() + " is annotated with @Main, and does not take 0 parameters");
                    if (!path.isEmpty())
                        commandsMap.put(path + MAIN_METHOD_NAME, new InternalCommand(parent, method, isAlias));
                    else mainMethod = new InternalCommand(parent, method, false);
                }
                return;
            }
            InternalCommand internalCommand = new InternalCommand(parent, method, isAlias);
            InternalCommand result = commandsMap.putIfAbsent(path + internalCommand.getName().toLowerCase(), internalCommand);
            if (result != null) {
                throw new IllegalArgumentException("Command " + method.getName() + " is already registered!");
            }
            internalCommand = new InternalCommand(parent, method, true);
            for (String s : internalCommand.getAliases()) {
                result = commandsMap.putIfAbsent((path + s).toLowerCase(), internalCommand);
                if (result != null) {
                    throw new IllegalArgumentException("Command " + method.getName() + " is already registered!");
                }
            }
        }

        /**
         * Walk through the class and add all subclasses.
         */
        private void walk(String path, @NotNull Object self, boolean isAlias) {
            Class<?> classIn = self.getClass();
            for (Method method : classIn.getDeclaredMethods()) {
                create((path + (path.equals("") ? "" : DELIMITER) + classIn.getAnnotation(SubCommandGroup.class).value()).toLowerCase() + DELIMITER, self, method, isAlias);
                for (String alias : classIn.getAnnotation(SubCommandGroup.class).aliases()) {
                    create((path + (path.equals("") ? "" : DELIMITER) + alias + DELIMITER).toLowerCase(), self, method, true);
                }
            }
            for (Class<?> cls : classIn.getDeclaredClasses()) {
                if (!cls.isAnnotationPresent(SubCommandGroup.class)) continue;
                Object subcommand = createIsnOf(cls, self);
                walk(path + (path.equals("") ? "" : DELIMITER) + classIn.getAnnotation(SubCommandGroup.class).value(), subcommand, isAlias);
                for (String alias : classIn.getAnnotation(SubCommandGroup.class).aliases()) {
                    walk(path + (path.equals("") ? "" : DELIMITER) + alias, subcommand, true);
                }
            }
        }

        @NotNull
        private String[] genHelpCommand() {
            String masterName = meta.value();
            StringBuilder sb = new StringBuilder(200);
            sb.append(meta.chatColor()).append(ChatColor.BOLD).append("Help for /").append(masterName).append(ChatColor.RESET).append(meta.chatColor());
            if (!meta.description().isEmpty()) sb.append(" - ").append(meta.description());
            sb.append(":           ").append(Arrays.toString(meta.aliases())).append("\n").append(meta.chatColor());
            if (mainMethod != null) {
                Main annotation = mainMethod.getUnderlyingMethod().isAnnotationPresent(Main.class) ? mainMethod.getUnderlyingMethod().getAnnotation(Main.class) : null;
                sb.append("/").append(masterName).append(" - ").append(annotation != null && !annotation.description().isEmpty() ? annotation.description() : "Main command").append("\n").append(meta.chatColor());
            }
            for (Map.Entry<String, InternalCommand> entry : commandsMap.entrySet()) {
                InternalCommand command = entry.getValue();
                String path;
                if (entry.getKey().endsWith(MAIN_METHOD_NAME)) {
                    Main annotation = command.getUnderlyingMethod().isAnnotationPresent(Main.class) ? command.getUnderlyingMethod().getAnnotation(Main.class) : null;
                    path = entry.getKey().substring(0, entry.getKey().length() - MAIN_METHOD_NAME.length()).replaceAll(DELIMITER, " ");
                    sb.append("/").append(masterName).append(path).append(" - ").append(annotation != null && !annotation.description().isEmpty() ? annotation.description() : "Main command").append("\n").append(meta.chatColor());
                    continue;
                }
                path = entry.getKey().replaceAll(DELIMITER, " ");
                if (!command.isUnderAlias()) {
                    sb.append("/").append(masterName).append(" ").append(path).append(" ");
                    for (Parameter parameter : command.method.getParameters()) {
                        String s = parameter.isAnnotationPresent(Description.class) ?
                                parameter.getAnnotation(Description.class).value() : parameter.getType().getSimpleName();
                        sb.append("<").append(s);
                        if (parameter.getType().isArray() || parameter.isAnnotationPresent(Greedy.class))
                            sb.append("...");
                        sb.append("> ");
                    }
                    if (command.hasHelp) sb.append("- ").append(command.getHelp());
                    sb.append("\n").append(meta.chatColor());

                }
            }
            return sb.toString().split("\n");
        }

        String[] getAdvancedHelp(InternalCommand command) {
            if (command != null) {
                // mm string builder looks great
                StringBuilder sb = new StringBuilder(200);
                sb.append(meta.chatColor()).append(ChatColor.BOLD).append("Advanced help for /").append(meta.value()).append(" ");
                sb.append(String.join(" ", command.getName())).append(ChatColor.RESET).append(meta.chatColor()).append(": ").append("\n").append(meta.chatColor());
                if (command.hasHelp) {
                    sb.append(ChatColor.BOLD).append("Description: ").append(ChatColor.RESET).append(meta.chatColor()).append(command.getHelp())
                            .append("\n").append(meta.chatColor());
                }
                if (command.getAliases().length > 0) {
                    sb.append("Aliases: ").append(String.join(", ", command.getAliases())).append("\n").append(meta.chatColor());
                }
                sb.append("Parameters:\n").append(meta.chatColor());
                for (Parameter parameter : command.method.getParameters()) {
                    Description description = parameter.isAnnotationPresent(Description.class) ? parameter.getAnnotation(Description.class) : null;
                    String s = description != null ? description.value() : parameter.getType().getSimpleName();
                    sb.append("<").append(s);
                    if (parameter.getType().isArray() || parameter.isAnnotationPresent(Greedy.class)) {
                        sb.append("...");
                    }
                    sb.append(">");
                    String desc = description != null && !description.description().isEmpty() ? description.description() : null;
                    sb.append(desc != null ? ": " + desc : "\n").append(meta.chatColor());
                }
                return sb.toString().split("\n");
            } else return new String[]{meta.chatColor() + NOT_FOUND_HELP_TEXT.replace("@ROOT_COMMAND@", meta.value())};
        }

        Command getMetadata() {
            return meta;
        }
    }

    class InternalCommand {
        private final Method method;
        private final SubCommand info;
        private final String[] aliases;
        private final String name;
        private final boolean hasHelp, isAlias;
        private final Object parent;

        private InternalCommand(Object parent, @NotNull Method methodIn, boolean isAlias) {
            this.parent = parent;
            if (!methodIn.isAccessible()) methodIn.setAccessible(true);
            this.method = methodIn;
            this.info = methodIn.isAnnotationPresent(SubCommand.class) ? methodIn.getAnnotation(SubCommand.class) : null;
            this.hasHelp = info != null && !info.description().isEmpty();
            this.isAlias = isAlias;

            // generate aliases
            this.name = methodIn.getName();
            this.aliases = info != null ? info.aliases() : new String[0];

            // check parameters
            int i = 0;
            for (Parameter parameter : method.getParameters()) {
                if (!parsers.containsKey(parameter.getType())) {
                    throw new IllegalArgumentException("Method " + method.getName() + " has a parameter of class " +
                            parameter.getType().getSimpleName() + " which does not have a valid parser; see CommandManager.addParser");
                }
                if (parameter.isAnnotationPresent(Greedy.class) && i != method.getParameters().length - 1) {
                    throw new IllegalArgumentException("Method " + method.getName() + " has a greedy parameter " +
                            parameter.getName() + " which is not the last parameter; this is not supported");
                }
                i++;
            }
        }

        @Nullable
        String invoke(String... argsIn) {
            try {
                // main method
                if (argsIn == null) {
                    method.invoke(parent);
                    return null;
                }
                if ((argsIn.length != method.getParameterCount()) && !method.getParameters()[method.getParameterCount() - 1].isAnnotationPresent(Greedy.class)) {
                    return ChatColor.RED + "Incorrect number of parameters, expected " + method.getParameterCount() + " but got " + argsIn.length;
                }
                Object[] args = new Object[method.getParameterCount()];
                Parameter[] parameters = method.getParameters();
                int i = 0;
                for (Parameter parameter : parameters) {
                    try {
                        if (i == args.length - 1 && parameter.isAnnotationPresent(Greedy.class)) {
                            // I love streams
                            args[i] = Arrays.stream(argsIn).skip(i).collect(Collectors.joining(" "));
                        } else {
                            args[i] = parsers.get(parameter.getType()).parse(argsIn[i]);
                        }
                    } catch (NumberFormatException ne) {
                        return ChatColor.RED + "Error while parsing parameter '" + argsIn[i] + "': " + "Parameter should be a number!";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ChatColor.RED + "Error while parsing parameter '" + argsIn[i] + "': " + e.getMessage();
                    }
                    i++;
                }
                method.invoke(parent, args);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return ChatColor.RED + METHOD_RUN_ERROR.replace("@ROOT_COMMAND@", getName());
            }
        }

        /**
         * Return weather this command was created under an alias path
         */
        boolean isUnderAlias() {
            return isAlias;
        }

        String[] getAliases() {
            return aliases;
        }

        String getName() {
            return name;
        }

        @Nullable
        String getHelp() {
            // return new Therapist();
            if (hasHelp) {
                return info.description();
            } else {
                return null;
            }
        }

        Method getUnderlyingMethod() {
            return method;
        }
    }


    /**
     * A final Pair class to hold a command and its arguments. The JavaFX one isn't final and might not be included so
     */
    final static class Pair<K, V> {
        private final K key;
        private final V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        K getKey() {
            return key;
        }

        V getValue() {
            return value;
        }
    }
}

