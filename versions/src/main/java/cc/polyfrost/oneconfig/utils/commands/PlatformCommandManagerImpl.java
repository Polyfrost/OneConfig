package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.commands.annotations.Name;
import cc.polyfrost.oneconfig.utils.commands.annotations.SubCommand;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;
import cc.polyfrost.oneconfig.utils.commands.arguments.Arguments;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;

import static cc.polyfrost.oneconfig.utils.commands.CommandManager.*;

public class PlatformCommandManagerImpl implements PlatformCommandManager {

    @Override
    public void createCommand(CommandManager.InternalCommand root, Command annotation) {
        //#if MC<=11202
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
            public void
                    //#if MC<=10809
                    processCommand(ICommandSender sender, String[] args)
                    //#else
                    //$$ execute(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args)
                    //#endif
            {
                handleCommand(root, annotation, args);
            }

            @Override
            public int getRequiredPermissionLevel() {
                return -1;
            }

            @Override
            public List<String>
                    //#if MC<=10809
                    addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
                    //#else
                    //$$ getTabCompletions(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
                    //#endif
            {
                return handleTabCompletion(root, annotation, args);
            }
        });
        //#else
        //#endif
    }

    private void handleCommand(CommandManager.InternalCommand root, Command annotation, String[] args) {
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
                UChat.chat(sendHelpCommand(root));
            } else {
                List<CommandManager.InternalCommand.InternalCommandInvoker> commands = new ArrayList<>();
                int depth = 0;
                for (CommandManager.InternalCommand command : root.children) {
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
                    List<CommandManager.CustomError> errors = new ArrayList<>();
                    for (CommandManager.InternalCommand.InternalCommandInvoker invoker : commands) {
                        try {
                            List<Object> params = getParametersForInvoker(invoker, depth, args);
                            if (params.size() == 1) {
                                Object first = params.get(0);
                                if (first instanceof CommandManager.CustomError) {
                                    errors.add((CommandManager.CustomError) first);
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
                        for (CommandManager.CustomError error : errors) {
                            UChat.chat("    " + ChatColor.RED + ChatColor.BOLD + error.message);
                        }
                    }
                }
            }
        }
    }

    private List<String> handleTabCompletion(CommandManager.InternalCommand root, Command annotation, String[] args) {
        try {
            Set<Pair<CommandManager.InternalCommand.InternalCommandInvoker, Integer>> commands = new HashSet<>();
            for (CommandManager.InternalCommand command : root.children) {
                loopThroughCommandsTab(commands, 0, command, args);
            }
            if (!commands.isEmpty() || annotation.helpCommand()) {
                List<Triple<CommandManager.InternalCommand.InternalCommandInvoker, Integer, Integer>> validCommands = new ArrayList<>(); // command, depth, and all processed params
                for (Pair<CommandManager.InternalCommand.InternalCommandInvoker, Integer> pair : commands) {
                    CommandManager.InternalCommand.InternalCommandInvoker invoker = pair.getLeft();
                    int depth = pair.getRight();
                    int currentParam = 0;
                    boolean failed = false;
                    while (args.length - depth > 1) {
                        Parameter param = invoker.method.getParameters()[currentParam];
                        if (param.isAnnotationPresent(Greedy.class) && currentParam + 1 != invoker.parameterTypes.length) {
                            failed = true;
                            break;
                        }
                        ArgumentParser<?> parser = INSTANCE.parsers.get(param.getType());
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
                if (!validCommands.isEmpty() || annotation.helpCommand()) {
                    Set<String> completions = new HashSet<>();
                    for (Triple<CommandManager.InternalCommand.InternalCommandInvoker, Integer, Integer> valid : validCommands) {
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
                        ArgumentParser<?> parser = INSTANCE.parsers.get(param.getType());
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
                    if (args.length == 1 && annotation.helpCommand()) {
                        if ("help".startsWith(args[0].toLowerCase(Locale.ENGLISH))) {
                            completions.add("help");
                        }
                    }
                    return new ArrayList<>(completions);
                }
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private List<Object> getParametersForInvoker(CommandManager.InternalCommand.InternalCommandInvoker invoker, int depth, String[] args) {
        List<Object> parameters = new ArrayList<>();
        int processed = depth;
        int currentParam = 0;
        while (processed < args.length) {
            Parameter param = invoker.method.getParameters()[currentParam];
            if (param.isAnnotationPresent(Greedy.class) && currentParam + 1 != invoker.parameterTypes.length) {
                return Collections.singletonList(new CommandManager.CustomError("Parsing failed: Greedy parameter must be the last one."));
            }
            ArgumentParser<?> parser = INSTANCE.parsers.get(param.getType());
            if (parser == null) {
                return Collections.singletonList(new CommandManager.CustomError("No parser for " + invoker.method.getParameterTypes()[currentParam].getSimpleName() + "! Please report this to the mod author."));
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
                        return Collections.singletonList(new CommandManager.CustomError("Failed to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
                    }
                } catch (Exception e) {
                    return Collections.singletonList(new CommandManager.CustomError("A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
                }
            } catch (Exception e) {
                return Collections.singletonList(new CommandManager.CustomError("A " + e.getClass().getSimpleName() + " has occured while try to parse " + param.getType().getSimpleName() + "! Please report this to the mod author."));
            }
        }
        return parameters;
    }

    private int loopThroughCommands(List<CommandManager.InternalCommand.InternalCommandInvoker> commands, int depth, CommandManager.InternalCommand command, String[] args) {
        int nextDepth = depth + 1;
        boolean thatOneSpecialError = false;
        if (command.isValid(args[depth], false)) {
            for (CommandManager.InternalCommand child : command.children) {
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
            for (CommandManager.InternalCommand.InternalCommandInvoker invoker : command.invokers) {
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

    private void loopThroughCommandsTab(Set<Pair<CommandManager.InternalCommand.InternalCommandInvoker, Integer>> commands, int depth, CommandManager.InternalCommand command, String[] args) {
        int nextDepth = depth + 1;
        if (command.isValid(args[depth], args.length == nextDepth)) {
            if (args.length != nextDepth) {
                for (CommandManager.InternalCommand child : command.children) {
                    if (child.isValid(args[nextDepth], args.length == nextDepth + 1)) {
                        loopThroughCommandsTab(commands, nextDepth, child, args);
                    }
                }
            }
            for (CommandManager.InternalCommand.InternalCommandInvoker invoker : command.invokers) {
                commands.add(new ImmutablePair<>(invoker, nextDepth));
            }
        }
    }

    //TODO: someone make the help command actually look nice lmao
    private String sendHelpCommand(CommandManager.InternalCommand root) {
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GOLD).append("Help for ").append(ChatColor.BOLD).append(root.name).append(ChatColor.RESET).append(ChatColor.GOLD).append(":\n");
        if (!root.description.isEmpty()) {
            builder.append("\n").append(ChatColor.GOLD).append("Description: ").append(ChatColor.BOLD).append(root.description);
        }
        for (CommandManager.InternalCommand command : root.children) {
            runThroughCommandsHelp(root.name, command, builder);
        }
        builder.append("\n").append(ChatColor.GOLD).append("Aliases: ").append(ChatColor.BOLD);
        int index = 0;
        for (String alias : root.aliases) {
            ++index;
            builder.append(alias).append(index < root.aliases.length ? ", " : "");
        }
        builder.append("\n");
        return builder.toString();
    }

    private void runThroughCommandsHelp(String append, CommandManager.InternalCommand command, StringBuilder builder) {
        if (!command.invokers.isEmpty()) {
            Class<?> declaringClass = command.invokers.get(0).method.getDeclaringClass();
            if (declaringClass.isAnnotationPresent(SubCommand.class)) {
                String description = declaringClass.getAnnotation(SubCommand.class).description();
                if (!description.isEmpty()) {
                    builder.append("\n").append(ChatColor.GOLD).append("Description: ").append(ChatColor.BOLD).append(description);
                }
            }
        }
        for (CommandManager.InternalCommand.InternalCommandInvoker invoker : command.invokers) {
            builder.append("\n").append(ChatColor.GOLD).append("/").append(append).append(" ").append(command.name);
            for (Parameter parameter : invoker.method.getParameters()) {
                String name = parameter.getName();
                if (parameter.isAnnotationPresent(Name.class)) {
                    name = parameter.getAnnotation(Name.class).value();
                }
                builder.append(" <").append(name).append(">");
            }
            if (!command.description.trim().isEmpty()) {
                builder.append(": ").append(ChatColor.BOLD).append(command.description);
            }
        }
        for (CommandManager.InternalCommand subCommand : command.children) {
            runThroughCommandsHelp(append + " " + command.name, subCommand, builder);
        }
    }
}