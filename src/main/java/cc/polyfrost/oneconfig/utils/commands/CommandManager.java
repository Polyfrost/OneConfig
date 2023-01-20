/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost and Pinkulu.
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
import cc.polyfrost.oneconfig.utils.SimpleProfiler;
import cc.polyfrost.oneconfig.utils.commands.annotations.*;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();
    private final String[] EMPTY_ARRAY = new String[]{""};
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
     * Registers the provided command. <br>
     * This method is fail-fast, meaning that it will throw Exceptions if the command is invalid upon startup.
     *
     * @param obj the command to register (must be an instance of a class annotated with @Command).
     */
    public void registerCommand(Object obj) {
        SimpleProfiler.push("registering command");
        platform.createCommand(new OCCommand(obj));
        SimpleProfiler.pop("registering command");
    }

    /**
     * Shortcut for registering the provided command, if you are lazy. <br>
     * This method is fail-fast, meaning that it will throw Exceptions if the command is invalid upon startup.
     *
     * @param obj the command to register (must be an instance of a class annotated with @Command).
     */
    public static void register(Object obj) {
        INSTANCE.registerCommand(obj);
    }

    /**
     * Registers the provided command. <b>Deprecated!</b>
     *
     * @param cls the command to register as a class.
     * @deprecated <b>Replace with {@link #registerCommand(Object)} aka. {@code new YourCommand()}</b>
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
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
    @NotNull
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
     * Take a command, go through all its parents, and for each add itself and its aliases
     */
    @NotNull
    static String[] computePaths(@NotNull InternalCommand in) {
        List<String> out = new ArrayList<>();
        for (String path : in.getParentPaths()) {
            for (String alias : in.getAliases()) {
                out.add((path + (path.isEmpty() ? "" : DELIMITER) + alias).toLowerCase());
            }
        }
        return out.toArray(new String[0]);
    }

    @NotNull
    private static String[] computePaths(@NotNull String[] paths, @NotNull Class<?> cls) {
        List<String> out = new ArrayList<>();
        SubCommandGroup annotation = cls.getAnnotation(SubCommandGroup.class);
        for (String path : paths) {
            String prefix = path + (path.isEmpty() ? "" : DELIMITER);
            for (String alias : annotation.aliases()) {
                out.add((prefix + alias).toLowerCase());
            }
            out.add((prefix + annotation.value()).toLowerCase());
        }
        return out.toArray(new String[0]);
    }

    /**
     * Internal class for command handling.
     */
    protected class OCCommand {
        final Map<InternalCommand, String[]> commandsMap = new HashMap<>();
        final String[] helpCommand;
        private final Command meta;
        InternalCommand mainMethod;

        private OCCommand(@NotNull Object commandIsn) {
            Class<?> cls = commandIsn.getClass();
            if (cls.isAnnotationPresent(Command.class)) {
                meta = cls.getAnnotation(Command.class);

                for (Method method : cls.getDeclaredMethods()) {
                    if (!method.isAccessible()) method.setAccessible(true);
                    create(EMPTY_ARRAY, commandIsn, method);
                }

                for (Class<?> subcommand : cls.getDeclaredClasses()) {
                    if (!subcommand.isAnnotationPresent(SubCommandGroup.class)) continue;
                    walk(EMPTY_ARRAY, createIsnOf(subcommand, commandIsn));
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
        private void create(String[] parentPaths, @NotNull Object parent, @NotNull Method method) {
            if (parent.getClass().equals(Class.class)) return;
            if (!method.isAccessible()) method.setAccessible(true);
            if (!method.isAnnotationPresent(SubCommand.class)) {
                if (method.isAnnotationPresent(Main.class)) {
                    if (mainMethod == null) {
                        // If @Main *and* doesn't have any arguments, this is the main method
                        if (Arrays.equals(parentPaths, EMPTY_ARRAY) && method.getParameterCount() == 0) {
                            mainMethod = new InternalCommand(parent, method, parentPaths);
                        } else {
                            // If there's only one main method, take it even if it has arguments
                            Method[] methods = method.getDeclaringClass().getDeclaredMethods();
                            int mains = (int) Stream.of(methods).filter(m -> m.isAnnotationPresent(Main.class)).count();
                            if (mains == 1) {
                                mainMethod = new InternalCommand(parent, method, parentPaths);
                            }
                        }
                    }
                } else return;
            }
            InternalCommand internalCommand = new InternalCommand(parent, method, parentPaths);
            if (commandsMap.keySet().stream().anyMatch(internalCommand::equals)) {
                throw new IllegalArgumentException("Command " + method.getName() + " is already registered!");
            }
            commandsMap.put(internalCommand, computePaths(internalCommand));
        }

        /**
         * Walk through the class and add all subclasses.
         */
        private void walk(String[] paths, @NotNull Object self) {
            Class<?> classIn = self.getClass();
            paths = computePaths(paths, classIn);
            for (Method method : classIn.getDeclaredMethods()) {
                create(paths, self, method);
            }
            for (Class<?> cls : classIn.getDeclaredClasses()) {
                if (!cls.isAnnotationPresent(SubCommandGroup.class)) continue;
                Object subcommand = createIsnOf(cls, self);
                walk(paths, subcommand);
            }
        }

        @NotNull
        private String[] genHelpCommand() {
            String masterName = meta.value();
            StringBuilder sb = new StringBuilder(200);
            sb.append(meta.chatColor()).append(ChatColor.BOLD).append("Help for /").append(masterName).append(ChatColor.RESET).append(meta.chatColor());
            if (!meta.description().isEmpty()) sb.append(" - ").append(meta.description());
            sb.append(":           ").append(Arrays.toString(meta.aliases())).append("\n").append(meta.chatColor());
            for (Iterator<InternalCommand> it = commandsMap.keySet().stream().sorted().iterator(); it.hasNext(); ) {
                final InternalCommand command = it.next();
                final String path;
                Method method = command.getUnderlyingMethod();
                if (command.getPrimaryPath().endsWith(MAIN_METHOD_NAME)) {
                    Main annotation = method.isAnnotationPresent(Main.class) ? method.getAnnotation(Main.class) : null;
                    path = command.getPrimaryPath().substring(0, command.getPrimaryPath().length() - MAIN_METHOD_NAME.length()).replaceAll(DELIMITER, " ").trim();
                    sb.append("/").append(masterName).append(path.isEmpty() ? "" : " ").append(path).append(" ");
                    for (Parameter parameter : method.getParameters()) {
                        appendParameter(sb, parameter);
                    }
                    sb.append("- ").append(annotation != null && !annotation.description().isEmpty() ? annotation.description() : "Main command").append("\n").append(meta.chatColor());
                    continue;
                }
                path = command.getPrimaryPath().replaceAll(DELIMITER, " ");
                sb.append("/").append(masterName).append(" ").append(path).append(" ");
                for (Parameter parameter : command.method.getParameters()) {
                    appendParameter(sb, parameter);
                }
                if (command.hasHelp) sb.append("- ").append(command.getHelp());
                sb.append("\n").append(meta.chatColor());

            }
            return sb.toString().split("\n");
        }

        private void appendParameter(StringBuilder sb, Parameter parameter) {
            String s = parameter.isAnnotationPresent(Description.class) ?
                    parameter.getAnnotation(Description.class).value() : parameter.getType().getSimpleName();
            sb.append("<").append(s);
            if (parameter.getType().isArray() || parameter.isAnnotationPresent(Greedy.class))
                sb.append("...");
            sb.append("> ");
        }

        String[] getAdvancedHelp(InternalCommand command) {
            if (command != null) {
                // mm string builder looks great
                StringBuilder sb = new StringBuilder(200);
                sb.append(meta.chatColor()).append(ChatColor.BOLD).append("Advanced help for /").append(meta.value()).append(" ").append(command.getPrimaryPath().replaceAll(DELIMITER, " "));
                sb.append(ChatColor.RESET).append(meta.chatColor()).append(": ").append("\n").append(meta.chatColor());
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

    class InternalCommand implements Comparable<InternalCommand> {
        private final Method method;
        private final SubCommand meta;
        private final String[] aliases, paths;
        private final boolean hasHelp;
        private final Object parent;

        private InternalCommand(Object parent, @NotNull Method methodIn, String[] paths) {
            this.parent = parent;
            if (!methodIn.isAccessible()) methodIn.setAccessible(true);
            this.method = methodIn;
            this.meta = methodIn.isAnnotationPresent(SubCommand.class) ? methodIn.getAnnotation(SubCommand.class) : null;
            this.hasHelp = meta != null && !meta.description().isEmpty();

            // generate aliases
            this.aliases = new String[meta != null ? meta.aliases().length + 1 : 1];
            if (meta != null) {
                aliases[0] = methodIn.getName();
                System.arraycopy(meta.aliases(), 0, aliases, 1, meta.aliases().length);
            } else {
                aliases[0] =// methodIn.getParameterCount() == 0
                        /*?*/ MAIN_METHOD_NAME
                // : methodIn.getName() //+ DELIMITER + DELIMITER + methodIn.getName();
                ;
            }
            this.paths = paths;

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
                if ((argsIn.length != method.getParameterCount()) && (method.getParameterCount() == 0 || !method.getParameters()[method.getParameterCount() - 1].isAnnotationPresent(Greedy.class))) {
                    return ChatColor.RED + "Incorrect number of parameters, expected " + method.getParameterCount() + " but got " + argsIn.length;
                }
                return invokeWith(method, argsIn);
            } catch (Exception e) {
                e.printStackTrace();
                return ChatColor.RED + METHOD_RUN_ERROR.replace("@ROOT_COMMAND@", getName());
            }
        }

        private String invokeWith(Method method, String[] argsIn) throws InvocationTargetException, IllegalAccessException {
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
        }

        String[] getAliases() {
            return aliases;
        }

        String[] getParentPaths() {
            return paths;
        }

        String getName() {
            return aliases[0];
        }

        String getPrimaryPath() {
            return paths[0] + (paths[0].isEmpty() ? "" : DELIMITER) + aliases[0];
        }

        @Nullable
        String getHelp() {
            // return new Therapist();
            if (hasHelp) {
                return meta.description();
            } else {
                return null;
            }
        }

        Method getUnderlyingMethod() {
            return method;
        }

        @Override
        public String toString() {
            return "InternalCommand{" +
                    "method=" + method.getName() +
                    ", primary=" + getPrimaryPath() +
                    ", aliases=" + Arrays.toString(aliases) +
                    ", parents=" + Arrays.toString(paths).replaceAll(DELIMITER, " ") +
                    '}';
        }

        @Override
        public int compareTo(@NotNull InternalCommand cmd) {
            return this.getPrimaryPath().compareTo(cmd.getPrimaryPath());
        }
    }


    /**
     * A final Pair class to hold a command and its arguments.
     */
    final static class Pair<K, V> {
        private final @NotNull K key;
        private final @NotNull V value;

        Pair(@NotNull K key, @NotNull V value) {
            this.key = key;
            this.value = value;
        }

        @NotNull
        K getKey() {
            return key;
        }

        @NotNull
        V getValue() {
            return value;
        }

        @NotNull
        @Contract(pure = true)
        @Override
        public String toString() {
            return "CommandManager.Pair{"
                    + "key=" + key
                    + ", value=" + value
                    + "}";
        }
    }
}

