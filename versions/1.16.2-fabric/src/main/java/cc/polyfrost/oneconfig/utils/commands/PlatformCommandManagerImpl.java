package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.ChatColor;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import cc.polyfrost.oneconfig.utils.commands.arguments.ArgumentParser;
import cc.polyfrost.oneconfig.utils.commands.arguments.Arguments;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static cc.polyfrost.oneconfig.utils.commands.CommandManager.METHOD_RUN_ERROR;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

    final HashMap<Class<?>, Pair<ArgumentType<Object>, ArgumentType<Object>>> parsers = new HashMap<>(); // non-greedy, greedy

    @Override
    void createCommand(CommandManager.InternalCommand root, Command annotation) {
        LiteralArgumentBuilder<ServerCommandSource> builder = net.minecraft.server.command.CommandManager.literal(annotation.value());
        if (!root.invokers.isEmpty()) {
            builder.executes((context ->
            {
                try {
                    root.invokers.get(0).method.invoke(null);
                    return 1;
                } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException |
                         ExceptionInInitializerError e) {
                    UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR);
                    return 0;
                }
            }));
        }
        if (annotation.helpCommand()) {
            builder.then(net.minecraft.server.command.CommandManager.literal("help").executes((context ->
            {
                UChat.chat(sendHelpCommand(root));
                return 1;
            })));
        }
        for (CommandManager.InternalCommand command : root.children) {
            loopThroughChildren(command, builder, null);
        }
    }

    private void loopThroughChildren(CommandManager.InternalCommand command, LiteralArgumentBuilder<ServerCommandSource> root, LiteralArgumentBuilder<ServerCommandSource> builder) {
        if (command.invokers.isEmpty() || command.children.isEmpty()) return;
        if (builder == null) {
            builder = root.then(net.minecraft.server.command.CommandManager.literal(command.name));
        } else {
            builder = builder.then(net.minecraft.server.command.CommandManager.literal(command.name));
        }
        for (CommandManager.InternalCommand.InternalCommandInvoker invoker : command.invokers) {
            for (Parameter parameter : invoker.method.getParameters()) {
                Pair<ArgumentType<Object>, ArgumentType<Object>> pair = parsers.get(parameter.getType());
                builder.then(net.minecraft.server.command.CommandManager.argument(parameter.getName(), parameter.isAnnotationPresent(Greedy.class) ? pair.getRight() : pair.getLeft()));
            }
            builder.executes((context ->
            {
                try {
                    ArrayList<Object> args = new ArrayList<>(invoker.method.getParameterCount());
                    for (Parameter parameter: invoker.method.getParameters()) {
                        args.add(context.getArgument(parameter.getName(), Object.class));
                    }
                    invoker.method.invoke(null, args);
                    return 1;
                } catch (Exception e) {
                    e.printStackTrace();
                    UChat.chat(ChatColor.RED.toString() + ChatColor.BOLD + METHOD_RUN_ERROR);
                    return 0;
                }
            }));
        }
        for (CommandManager.InternalCommand child : command.children) {
            loopThroughChildren(child, root, builder);
        }
    }

    @Override
    public void handleNewParser(ArgumentParser<?> parser, Class<?> clazz) {
        parsers.put(clazz, new ImmutablePair<ArgumentType<Object>, ArgumentType<Object>>(new ArgumentType() {

            @Override
            public Object parse(StringReader reader) {
                final String text = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                return parser.parse(new Arguments(text.split("\\s+"), false));
            }

            @Override
            public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
                return ArgumentType.super.listSuggestions(context, builder);
            }
        }, new ArgumentType() {

            @Override
            public Object parse(StringReader reader) {
                final String text = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                return parser.parse(new Arguments(text.split("\\s+"), true));
            }

            @Override
            public CompletableFuture<Suggestions> listSuggestions(CommandContext context, SuggestionsBuilder builder) {
                return ArgumentType.super.listSuggestions(context, builder);
            }
        }));
    }
}
