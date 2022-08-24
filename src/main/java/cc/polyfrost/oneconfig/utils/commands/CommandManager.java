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

import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Descriptor;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
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
    private static final PlatformCommandManager platform = ServiceLoader.load(PlatformCommandManager.class, PlatformCommandManager.class.getClassLoader()).iterator().next();
    public static final CommandManager INSTANCE = new CommandManager();
    static final String NOT_FOUND_TEXT = "Command not found! Type /@ROOT_COMMAND@ help for help.";
    static final String NOT_FOUND_HELP_TEXT = "Help for this command was not found! Type /@ROOT_COMMAND@ help for generic help first.";
    static final String METHOD_RUN_ERROR = "Error while running @ROOT_COMMAND@ method! Please report this to the developer.";
    /**
     * <a href="https://https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt">UTF-8 Stress Test</a> Character 2.3.1 (used because it's never going to be used in theory)
     */
    private static final String DELIMITER = "\uD7FF";
    private final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

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
        private final HashMap<String, InternalCommand> commandsMap = new HashMap<>();
        private final String[] helpCommand;
        private final Command meta;
        private final InternalCommand mainMethod;

        private OCCommand(@NotNull Object commandIsn) {
            Class<?> cls = commandIsn.getClass();
            if (cls.isAnnotationPresent(Command.class)) {
                meta = cls.getAnnotation(Command.class);

                InternalCommand mainMethodTempFixBecauseJavaBad = null;
                for (Method method : cls.getDeclaredMethods()) {
                    if (!method.isAccessible()) method.setAccessible(true);
                    // grab main method if present
                    if (method.getName().equalsIgnoreCase("main")) {
                        if (method.getParameterCount() != 0)
                            throw new IllegalArgumentException("The main method of a master command must have no parameters!");
                        mainMethodTempFixBecauseJavaBad = new InternalCommand(commandIsn, method, false);
                        continue;
                    }
                    // else just add that method
                    create("", commandIsn, method, false);
                }
                this.mainMethod = mainMethodTempFixBecauseJavaBad;

                for (Class<?> subcommand : cls.getDeclaredClasses()) {
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
            if (!method.isAccessible()) method.setAccessible(true);
            if (method.isAnnotationPresent(Exclude.class)) return;
            if (parent.getClass().equals(Class.class)) return;
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
                create((path + (path.equals("") ? "" : DELIMITER) + classIn.getAnnotation(Command.class).value()).toLowerCase() + DELIMITER, self, method, isAlias);
                for (String alias : classIn.getAnnotation(Command.class).aliases()) {
                    create((path + (path.equals("") ? "" : DELIMITER) + alias + DELIMITER).toLowerCase(), self, method, true);
                }
            }
            for (Class<?> cls : classIn.getDeclaredClasses()) {
                if (!cls.isAnnotationPresent(Command.class)) continue;
                Object subcommand = createIsnOf(cls, self);
                walk(path + (path.equals("") ? "" : DELIMITER) + classIn.getAnnotation(Command.class).value(), subcommand, isAlias);
                for (String alias : classIn.getAnnotation(Command.class).aliases()) {
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
                sb.append("/").append(masterName).append(" - ").append(mainMethod.hasHelp ? mainMethod.getHelp() : "Main command").append("\n").append(meta.chatColor());
            }
            for (Map.Entry<String, InternalCommand> entry : commandsMap.entrySet()) {
                InternalCommand command = entry.getValue();
                String path = entry.getKey().replaceAll(DELIMITER, " ");
                if (!command.isUnderAlias()) {
                    sb.append("/").append(masterName).append(" ").append(path).append(" ");
                    if (path.endsWith(" main")) {
                        sb.replace(sb.length() - 5, sb.length(), "");
                    }
                    for (Parameter parameter : command.method.getParameters()) {
                        String s = parameter.isAnnotationPresent(Descriptor.class) ?
                                parameter.getAnnotation(Descriptor.class).value() : parameter.getType().getSimpleName();
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

        public String[] doCommand(@NotNull String[] args) {
            if (args.length == 0) {
                if (mainMethod != null) return new String[]{mainMethod.invoke()};
                else return helpCommand;
            } else if (args[0].equalsIgnoreCase("help")) {
                if (args.length == 1) {
                    return helpCommand;
                } else {
                    return getAdvancedHelp(args);
                }
            } else {
                Pair<String[], InternalCommand> command = getCommand(args);
                if (command != null) {
                    return new String[]{command.getValue().invoke(command.getKey())};
                }
            }
            return new String[]{meta.chatColor() + NOT_FOUND_TEXT.replace("@ROOT_COMMAND@", meta.value())};
        }

        /**
         * Convert the String[] args into a processable command
         */
        @Nullable
        private Pair<String[], InternalCommand> getCommand(String[] args) {
            // turn the given args into a 'path'
            String argsIn = String.join(DELIMITER, args).toLowerCase();
            for (int i = args.length - 1; i >= 0; i--) {
                // work backwards to find the first match
                if (commandsMap.containsKey(argsIn)) {
                    // create the args for the command
                    String[] newArgs = new String[args.length - i - 1];
                    System.arraycopy(args, i + 1, newArgs, 0, args.length - i - 1);
                    // return the command and the args
                    return new Pair<>(newArgs, commandsMap.get(argsIn));
                } else if (commandsMap.containsKey(argsIn + DELIMITER + "main")) {
                    String[] newArgs = new String[args.length - i - 1];
                    System.arraycopy(args, i + 1, newArgs, 0, args.length - i - 1);
                    return new Pair<>(newArgs, commandsMap.get(argsIn + DELIMITER + "main"));
                }
                // remove the last word
                int target = argsIn.lastIndexOf(DELIMITER);
                argsIn = argsIn.substring(0, target == -1 ? argsIn.length() : target);
            }
            return null;
        }

        private String[] getAdvancedHelp(String[] args) {
            // do advanced help stuff
            String[] args1 = new String[args.length - 1];
            System.arraycopy(args, 1, args1, 0, args.length - 1);
            Pair<String[], InternalCommand> cmd = getCommand(args1);
            if (cmd != null) {
                // mm string builder looks great
                StringBuilder sb = new StringBuilder(200);
                sb.append(meta.chatColor()).append(ChatColor.BOLD).append("Advanced help for /").append(meta.value()).append(" ");
                sb.append(String.join(" ", args1)).append(ChatColor.RESET).append(meta.chatColor()).append(": ").append(cmd.getValue().descriptor != null ?
                        "(" + cmd.getValue().descriptor.value() + ")" : "").append("\n").append(meta.chatColor());
                if (cmd.getValue().hasHelp) {
                    sb.append(ChatColor.BOLD).append("Description: ").append(ChatColor.RESET).append(meta.chatColor()).append(cmd.getValue().getHelp())
                            .append("\n").append(meta.chatColor());
                }
                if (cmd.getValue().getAliases().length > 0) {
                    sb.append("Aliases: ").append(String.join(", ", cmd.getValue().getAliases())).append("\n").append(meta.chatColor());
                }
                sb.append("Parameters:\n").append(meta.chatColor());
                for (Parameter parameter : cmd.getValue().method.getParameters()) {
                    Descriptor descriptor = parameter.isAnnotationPresent(Descriptor.class) ? parameter.getAnnotation(Descriptor.class) : null;
                    String s = descriptor != null ? descriptor.value() : parameter.getType().getSimpleName();
                    sb.append("<").append(s);
                    if (parameter.getType().isArray() || parameter.isAnnotationPresent(Greedy.class)) {
                        sb.append("...");
                    }
                    sb.append(">");
                    String desc = descriptor != null && !descriptor.description().isEmpty() ? descriptor.description() : null;
                    sb.append(desc != null ? ": " + desc : "\n").append(meta.chatColor());
                }
                return sb.toString().split("\n");
            } else return new String[]{meta.chatColor() + NOT_FOUND_HELP_TEXT.replace("@ROOT_COMMAND@", meta.value())};
        }

        public List<String> getTabCompletionOptions(String[] args) {
            List<String> opts = new ArrayList<>();
            Pair<String[], InternalCommand> command = getCommand(args);
            try {
                // means we are in a command
                if (command != null) {
                    Parameter currentParam = command.getValue().method.getParameters()[command.getKey().length - 1];
                    String type = currentParam.getType().getSimpleName();
                    boolean isNumeric = type.equalsIgnoreCase("int") || type.equalsIgnoreCase("long") ||
                            type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float") || type.equalsIgnoreCase("integer");

                    Descriptor descriptor = currentParam.isAnnotationPresent(Descriptor.class) ? currentParam.getAnnotation(Descriptor.class) : null;
                    String targets = descriptor != null && !descriptor.autoCompletesTo().isEmpty() ? descriptor.autoCompletesTo() : null;
                    if (targets != null) {
                        if (targets.equals("PLAYER")) {
                            if (platform.getPlayerNames() != null) {
                                opts.addAll(platform.getPlayerNames());
                            }
                        } else {
                            opts.addAll(Arrays.asList(targets.split(",")));
                        }
                    } else {
                        // yes.
                        if (isNumeric) opts.add("0");
                        else if (type.equalsIgnoreCase("boolean")) {
                            opts.add("true");
                            opts.add("false");
                        } else if (type.equalsIgnoreCase("string")) {
                            opts.add("String");
                        }
                    }
                } else {
                    String current = args.length == 0 ? "" : String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1));
                    //System.out.println(current);
                    commandsMap.forEach((key, value) -> {
                        if(!value.isAlias) {
                            String toAdd = "";
                            if(current.isEmpty()) {
                                if(!key.contains(DELIMITER)) toAdd = key;
                                else toAdd = key.substring(0, key.indexOf(DELIMITER));
                            } else {
                                //System.out.println(key);
                                /// TODO BROKEN AND I DONT KNOW WHY plz help me
                                //System.out.println(key.substring(current.length()));
                            }
                            if(!opts.contains(toAdd)) opts.add(toAdd);
                        }
                    });
                }
            } catch (Exception ignored) {
                // happens when there is too many args presented for a method
            }
            return opts.isEmpty() ? null : opts;
        }

        public Command getMetadata() {
            return meta;
        }
    }

    private class InternalCommand {
        private final Method method;
        private final Descriptor descriptor;
        private final String[] aliases;
        private final String name;
        private final boolean hasHelp, isAlias;
        private final Object parent;

        private InternalCommand(Object parent, @NotNull Method methodIn, boolean isAlias) {
            this.parent = parent;
            if (!methodIn.isAccessible()) methodIn.setAccessible(true);
            this.method = methodIn;
            this.descriptor = methodIn.isAnnotationPresent(Descriptor.class) ? methodIn.getAnnotation(Descriptor.class) : null;
            this.hasHelp = descriptor != null && !descriptor.description().isEmpty();
            this.isAlias = isAlias;

            // generate aliases
            this.name = methodIn.getName();
            this.aliases = descriptor != null ? descriptor.aliases() : new String[0];

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
        private String invoke(String... argsIn) {
            try {
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
                return ChatColor.RED + METHOD_RUN_ERROR.replace("@ROOT_COMMAND@", aliases[0]);
            }
        }

        /**
         * Return weather this command was created under an alias path
         */
        private boolean isUnderAlias() {
            return isAlias;
        }

        private String[] getAliases() {
            return aliases;
        }

        private String getName() {
            return name;
        }

        @Nullable
        private String getHelp() {
            // return new Therapist();
            if (hasHelp) {
                return descriptor.description();
            } else {
                return null;
            }
        }
    }


    /**
     * A final Pair class to hold a command and its arguments. The JavaFX one isn't final and might not be included so
     */
    private final static class Pair<K, V> {
        private final K key;
        private final V value;

        private Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        private K getKey() {
            return key;
        }

        private V getValue() {
            return value;
        }
    }
}

