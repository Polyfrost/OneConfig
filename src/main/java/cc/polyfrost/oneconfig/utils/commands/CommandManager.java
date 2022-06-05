package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.*;
import cc.polyfrost.oneconfig.utils.commands.arguments.*;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;

import java.lang.reflect.InvocationTargetException;
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
    private final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();
    private static final String NOT_FOUND_TEXT = "Command not found! Type /@ROOT_COMMAND@ help for help.";
    private static final String METHOD_RUN_ERROR = "Error while running @ROOT_COMMAND@ method! Please report this to the developer.";

    private CommandManager() {
        addParser(new StringParser());
        addParser(new IntegerParser());
        addParser(new IntegerParser(), int.class);
        addParser(new DoubleParser());
        addParser(new DoubleParser(), double.class);
        addParser(new FloatParser());
        addParser(new FloatParser(), float.class);
        addParser(new BooleanParser());
        addParser(new BooleanParser(), boolean.class);
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
     * @param command The command to register.
     */
    public void registerCommand(Object command) {
        Class<?> clazz = command.getClass();
        if (clazz.isAnnotationPresent(Command.class)) {
            final Command annotation = clazz.getAnnotation(Command.class);
            ArrayList<InternalCommand.InternalCommandInvoker> mainCommandFuncs = new ArrayList<>();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Main.class) && method.getParameterCount() == 0) {
                    mainCommandFuncs.add(new InternalCommand.InternalCommandInvoker(annotation.value(), annotation.aliases(), method));
                    break;
                }
            }

            final InternalCommand root = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description().trim().isEmpty() ? "Main command for " + annotation.value() : annotation.description(), mainCommandFuncs);
            addToInvokers(clazz.getDeclaredClasses(), root);
            ClientCommandHandler.instance.registerCommand(new CommandBase() {
                @Override
                public String getCommandName() {
                    return annotation.value();
                }

                @Override
                public String getCommandUsage(ICommandSender sender) {
                    return "/" + annotation.value();
                }

                @Override
                public void processCommand(ICommandSender sender, String[] args) {
                    if (args.length == 0) {
                        if (!root.invokers.isEmpty()) {
                            try {
                                root.invokers.stream().findFirst().get().method.invoke(null);
                            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                                     ExceptionInInitializerError e) {
                                UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR);
                            }
                        }
                    } else {
                        if (annotation.helpCommand() && args[0].equalsIgnoreCase("help")) {
                            UChat.chat(sendHelpCommand(root));
                        } else {
                            for (InternalCommand command : root.children) {
                                String result = runThroughCommands(command, 0, args);
                                if (result == null) {
                                    return;
                                } else if (!result.equals(NOT_FOUND_TEXT)) {
                                    UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + result.replace("@ROOT_COMMAND@", getCommandName()));
                                    return;
                                }
                            }
                            UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + NOT_FOUND_TEXT.replace("@ROOT_COMMAND@", getCommandName()));
                        }
                    }
                }

                @Override
                public int getRequiredPermissionLevel() {
                    return -1;
                }
            });
        }
    }

    private String sendHelpCommand(InternalCommand root) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GOLD.toString() + "Help for " + ChatColor.BOLD + root.name + ChatColor.RESET + ChatColor.GOLD + ":\n");
        builder.append("\n");
        for (InternalCommand command : root.children) {
            runThroughCommandsHelp(root.name, root, builder);
        }
        builder.append("\n" + ChatColor.GOLD + "Aliases: " + ChatColor.BOLD);
        int index = 0;
        for (String alias : root.aliases) {
            ++index;
            builder.append(alias + (index < root.aliases.length ? ", " : ""));
        }
        builder.append("\n");
        return builder.toString();
    }

    private void runThroughCommandsHelp(String append, InternalCommand command, StringBuilder builder) {
        for (InternalCommand.InternalCommandInvoker invoker : command.invokers) {
            builder.append("\n" + ChatColor.GOLD + "/" + append + " " + command.name);
            for (Parameter parameter : invoker.method.getParameters()) {
                String name = parameter.getName();
                if (parameter.isAnnotationPresent(Name.class)) {
                    name = parameter.getAnnotation(Name.class).value();
                }
                builder.append(" <" + name + ">");
            }
            if (!command.description.trim().isEmpty()) {
                builder.append(": " + ChatColor.BOLD + command.description);
            }
        }
        for (InternalCommand subCommand : command.children) {
            runThroughCommandsHelp(append + " " + command.name, subCommand, builder);
        }
    }

    private String runThroughCommands(InternalCommand command, int layer, String[] args) {
        int newLayer = layer + 1;
        if (command.isEqual(args[layer]) && !command.invokers.isEmpty()) {
            Set<InternalCommand.InternalCommandInvoker> invokers = command.invokers.stream().filter(invoker -> newLayer == args.length - invoker.parameterTypes.length).sorted(Comparator.comparingInt((a) -> a.method.getAnnotation(Main.class).priority())).collect(Collectors.toSet());
            if (!invokers.isEmpty()) {
                for (InternalCommand.InternalCommandInvoker invoker : invokers) {
                    try {
                        String a = tryInvoker(invoker, newLayer, args);
                        if (a == null) {
                            return null;
                        } else if (a.contains(METHOD_RUN_ERROR)) {
                            return a;
                        }
                    } catch (Exception ignored) {

                    }
                }
            } else {
                for (InternalCommand subCommand : command.children) {
                    String result = runThroughCommands(subCommand, newLayer, args);
                    if (result == null) {
                        return null;
                    } else if (!result.equals(NOT_FOUND_TEXT)) {
                        return result;
                    }
                }
            }
        }
        return NOT_FOUND_TEXT;
    }

    private String tryInvoker(InternalCommand.InternalCommandInvoker invoker, int newLayer, String[] args) {
        try {
            ArrayList<Object> params = new ArrayList<>();
            int processed = newLayer;
            int currentParam = 0;
            while (processed < args.length) {
                Parameter param = invoker.method.getParameters()[currentParam];
                if (param.isAnnotationPresent(Greedy.class) && currentParam + 1 != invoker.method.getParameterCount()) {
                    return "Parsing failed: Greedy parameter must be the last one.";
                }
                ArgumentParser<?> parser = parsers.get(param.getType());
                if (parser == null) {
                    return "No parser for " + invoker.method.getParameterTypes()[currentParam].getSimpleName() + "! Please report this to the mod author.";
                }
                try {
                    Arguments arguments = new Arguments(Arrays.copyOfRange(args, processed, args.length), param.isAnnotationPresent(Greedy.class));
                    try {
                        Object a = parser.parse(arguments);
                        if (a != null) {
                            params.add(a);
                            processed += arguments.getPosition();
                            currentParam++;
                        } else {
                            return "Failed to parse " + param.getType().getSimpleName() + "! Please report this to the mod author.";
                        }
                    } catch (Exception e) {
                        return "A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author.";
                    }
                } catch (Exception e) {
                    return "A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author.";
                }
            }
            invoker.method.invoke(null, params.toArray());
            return null;
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                 ExceptionInInitializerError e) {
            return ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR;
        }
    }

    private void addToInvokers(Class<?>[] classes, InternalCommand parent) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(SubCommand.class)) {
                SubCommand annotation = clazz.getAnnotation(SubCommand.class);
                ArrayList<InternalCommand.InternalCommandInvoker> mainMethods = new ArrayList<>();
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Main.class)) {
                        mainMethods.add(new InternalCommand.InternalCommandInvoker(annotation.value(), annotation.aliases(), method));
                    }
                }
                InternalCommand command = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description(), mainMethods);
                parent.children.add(command);
                addToInvokers(clazz.getDeclaredClasses(), command);
            }
        }
    }

    private static class InternalCommand {
        public final String name;
        public final String[] aliases;
        public final String description;
        public final ArrayList<InternalCommandInvoker> invokers;
        public final ArrayList<InternalCommand> children = new ArrayList<>();

        public InternalCommand(String name, String[] aliases, String description, ArrayList<InternalCommandInvoker> invokers) {
            this.name = name;
            this.aliases = aliases;
            this.invokers = invokers;
            this.description = description;
        }

        public boolean isEqual(String name) {
            if (this.name.equals(name)) {
                return true;
            } else {
                for (String alias : aliases) {
                    if (alias.equals(name)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public static class InternalCommandInvoker {
            public final String name;
            public final String[] aliases;
            public final Method method;
            public final Parameter[] parameterTypes;

            public InternalCommandInvoker(String name, String[] aliases, Method method) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("All command methods must be static!");
                }
                this.name = name;
                this.aliases = aliases;
                this.method = method;
                this.parameterTypes = method.getParameters().clone();
                if (Modifier.isPrivate(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
                    method.setAccessible(true);
                }
            }
        }
    }
}
