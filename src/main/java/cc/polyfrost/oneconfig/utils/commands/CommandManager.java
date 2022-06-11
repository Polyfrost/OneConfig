package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import cc.polyfrost.oneconfig.utils.commands.arguments.*;
import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.InvocationTargetException;
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
    public static final CommandManager INSTANCE = new CommandManager();
    private static final String NOT_FOUND_TEXT = "Command not found! Type /@ROOT_COMMAND@ help for help.";
    private static final String TOO_MANY_PARAMETERS = "There were too many / little parameters for this command! Type /@ROOT_COMMAND@ help for help.";
    private static final String METHOD_RUN_ERROR = "Error while running @ROOT_COMMAND@ method! Please report this to the developer.";
    private final HashMap<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();

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

            final InternalCommand root = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description().trim().isEmpty() ? "Main command for " + annotation.value() : annotation.description(), null);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Main.class) && method.getParameterCount() == 0) {
                    root.invokers.add(new InternalCommand.InternalCommandInvoker(annotation.value(), annotation.aliases(), method, root));
                    break;
                }
            }
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
                    handleCommand(root, annotation, args);
                }

                @Override
                public int getRequiredPermissionLevel() {
                    return -1;
                }

                @Override
                public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
                    return handleTabCompletion(root, args);
                }
            });
        }
    }

    private void handleCommand(InternalCommand root, Command annotation, String[] args) {
        if (args.length == 0) {
            if (!root.invokers.isEmpty()) {
                try {
                    root.invokers.get(0).method.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                        ExceptionInInitializerError e) {
                    e.printStackTrace();
                    UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR);
                }
            }
        } else {
            if (annotation.helpCommand() && args[0].equalsIgnoreCase("help")) {
                //UChat.chat(sendHelpCommand(root));
            } else {
                List<InternalCommand.InternalCommandInvoker> commands = new ArrayList<>();
                int depth = 0;
                for (InternalCommand command : root.children) {
                    int newDepth = loopThroughCommands(commands, 0, command, args);
                    if (newDepth != -1) {
                        depth = newDepth;
                        break;
                    }
                }
                if (commands.isEmpty()) {
                    if (depth == -2) {
                        UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + TOO_MANY_PARAMETERS.replace("@ROOT_COMMAND@", annotation.value()));
                    } else {
                        UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + NOT_FOUND_TEXT.replace("@ROOT_COMMAND@", annotation.value()));
                    }
                } else {
                    List<CustomError> errors = new ArrayList<>();
                    for (InternalCommand.InternalCommandInvoker invoker : commands) {
                        try {
                            List<Object> params = getParametersForInvoker(invoker, depth, args);
                            if (params.size() == 1) {
                                Object first = params.get(0);
                                if (first instanceof CustomError) {
                                    errors.add((CustomError) first);
                                    continue;
                                }
                            }
                            invoker.method.invoke(null, params.toArray());
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR);
                            return;
                        }
                    }
                    //noinspection ConstantConditions
                    if (!errors.isEmpty()) {
                        UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + "Multiple errors occurred:");
                        for (CustomError error : errors) {
                            UChat.chat("    " + ChatColor.RED + ChatColor.BOLD + error.message);
                        }
                    }
                }
            }
        }
    }

    private List<String> handleTabCompletion(InternalCommand root, String[] args) {
        try {
            Set<Pair<InternalCommand.InternalCommandInvoker, Integer>> commands = new HashSet<>();
            for (InternalCommand command : root.children) {
                loopThroughCommandsTab(commands, 0, command, args);
            }
            if (!commands.isEmpty()) {
                List<Triple<InternalCommand.InternalCommandInvoker, Integer, Integer>> validCommands = new ArrayList<>(); // command, depth, and all processed params
                for (Pair<InternalCommand.InternalCommandInvoker, Integer> pair : commands) {
                    InternalCommand.InternalCommandInvoker invoker = pair.getLeft();
                    int depth = pair.getRight();
                    int currentParam = 0;
                    boolean failed = false;
                    while (args.length - depth > 1) {
                        Parameter param = invoker.method.getParameters()[currentParam];
                        if (param.isAnnotationPresent(Greedy.class) && currentParam + 1 != invoker.parameterTypes.length) {
                            failed = true;
                            break;
                        }
                        ArgumentParser<?> parser = parsers.get(param.getType());
                        if (parser == null) {
                            failed = true;
                            break;
                        }
                        try {
                            Arguments arguments = new Arguments(Arrays.copyOfRange(args, depth, args.length), param.isAnnotationPresent(Greedy.class));
                            if (parser.parse(arguments) != null) {
                                depth += arguments.getPosition();
                                currentParam++;
                            } else {
                                failed = true;
                                break;
                            }
                        } catch (Exception e) {
                            failed = true;
                            break;
                        }
                    }
                    if (!failed) {
                        validCommands.add(new ImmutableTriple<>(pair.getLeft(), depth, currentParam));
                    }
                }
                if (!validCommands.isEmpty()) {
                    Set<String> completions = new HashSet<>();
                    for (Triple<InternalCommand.InternalCommandInvoker, Integer, Integer> valid : validCommands) {
                        if (valid.getMiddle() == args.length) {
                            completions.add(valid.getLeft().name);
                            completions.addAll(Arrays.asList(valid.getLeft().aliases));
                            continue;
                        }
                        if (valid.getRight() + 1 > valid.getLeft().parameterTypes.length) continue;
                        Parameter param = valid.getLeft().method.getParameters()[valid.getRight()];
                        if (param.isAnnotationPresent(Greedy.class) && valid.getRight() + 1 != valid.getLeft().parameterTypes.length) {
                            continue;
                        }
                        ArgumentParser<?> parser = parsers.get(param.getType());
                        if (parser == null) {
                            continue;
                        }
                        try {
                            Arguments arguments = new Arguments(Arrays.copyOfRange(args, valid.getMiddle(), args.length), param.isAnnotationPresent(Greedy.class));
                            List<String> possibleCompletions = parser.complete(arguments, param);
                            if (possibleCompletions != null) {
                                completions.addAll(possibleCompletions);
                            }
                        } catch (Exception ignored) {

                        }
                    }
                    return new ArrayList<>(completions);
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private List<Object> getParametersForInvoker(InternalCommand.InternalCommandInvoker invoker, int depth, String[] args) {
        List<Object> parameters = new ArrayList<>();
        int processed = depth;
        int currentParam = 0;
        while (processed < args.length) {
            Parameter param = invoker.method.getParameters()[currentParam];
            if (param.isAnnotationPresent(Greedy.class) && currentParam + 1 != invoker.parameterTypes.length) {
                return Collections.singletonList(new CustomError("Parsing failed: Greedy parameter must be the last one."));
            }
            ArgumentParser<?> parser = parsers.get(param.getType());
            if (parser == null) {
                return Collections.singletonList(new CustomError("No parser for " + invoker.method.getParameterTypes()[currentParam].getSimpleName() + "! Please report this to the mod author."));
            }
            try {
                Arguments arguments = new Arguments(Arrays.copyOfRange(args, processed, args.length), param.isAnnotationPresent(Greedy.class));
                try {
                    Object a = parser.parse(arguments);
                    if (a != null) {
                        parameters.add(a);
                        processed += arguments.getPosition();
                        currentParam++;
                    } else {
                        return Collections.singletonList(new CustomError("Failed to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
                    }
                } catch (Exception e) {
                    return Collections.singletonList(new CustomError("A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
                }
            } catch (Exception e) {
                return Collections.singletonList(new CustomError("A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
            }
        }
        return parameters;
    }

    private int loopThroughCommands(List<InternalCommand.InternalCommandInvoker> commands, int depth, InternalCommand command, String[] args) {
        int nextDepth = depth + 1;
        boolean thatOneSpecialError = false;
        if (command.isValid(args[depth], false)) {
            for (InternalCommand child : command.children) {
                if (args.length > nextDepth && child.isValid(args[nextDepth], false)) {
                    int result = loopThroughCommands(commands, nextDepth, child, args);
                    if (result > -1) {
                        return result;
                    } else if (result == -2) {
                        thatOneSpecialError = true;
                    }
                }
            }
            boolean added = false;
            for (InternalCommand.InternalCommandInvoker invoker : command.invokers) {
                if (args.length - nextDepth == invoker.parameterTypes.length) {
                    commands.add(invoker);
                    added = true;
                } else {
                    thatOneSpecialError = true;
                }
            }
            if (added) {
                return nextDepth;
            }
        }
        return thatOneSpecialError ? -2 : -1;
    }

    private void loopThroughCommandsTab(Set<Pair<InternalCommand.InternalCommandInvoker, Integer>> commands, int depth, InternalCommand command, String[] args) {
        int nextDepth = depth + 1;
        if (command.isValid(args[depth], args.length == nextDepth)) {
            if (args.length != nextDepth) {
                for (InternalCommand child : command.children) {
                    if (child.isValid(args[nextDepth], args.length == nextDepth + 1)) {
                        loopThroughCommandsTab(commands, nextDepth, child, args);
                    }
                }
            }
            for (InternalCommand.InternalCommandInvoker invoker : command.invokers) {
                commands.add(new ImmutablePair<>(invoker, nextDepth));
            }
        }
    }

    private void addToInvokers(Class<?>[] classes, InternalCommand parent) {
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(SubCommand.class)) {
                SubCommand annotation = clazz.getAnnotation(SubCommand.class);
                InternalCommand command = new InternalCommand(annotation.value(), annotation.aliases(), annotation.description(), parent);
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

    private static class CustomError {
        public String message;

        public CustomError(String message) {
            this.message = message;
        }
    }

    private static class InternalCommand {
        public final String name;
        public final String[] aliases;
        public final String description;
        public final ArrayList<InternalCommandInvoker> invokers = new ArrayList<>();
        public final InternalCommand parent;
        public final ArrayList<InternalCommand> children = new ArrayList<>();

        public InternalCommand(String name, String[] aliases, String description, InternalCommand parent) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
            this.parent = parent;
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

