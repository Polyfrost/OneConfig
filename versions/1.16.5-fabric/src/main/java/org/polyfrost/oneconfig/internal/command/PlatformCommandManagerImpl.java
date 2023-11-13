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

package org.polyfrost.oneconfig.internal.command;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.CommandManager;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.Executable;
import org.polyfrost.oneconfig.api.commands.Node;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.internal.mixin.HelpCommandAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 * Fabric Client-side command manager implementation.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/impl/command/client/ClientCommandInternals.java">Click here for source</a>
 */
@ApiStatus.Internal
public class PlatformCommandManagerImpl implements PlatformCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Commands");
    private static final Map<Class<?>, Supplier<ArgumentType<?>>> argTypeMap = new HashMap<>();


    static {
        registerIntrinsics();
        argTypeMap.put(PlayerEntity.class, EntityArgumentType::player);
        argTypeMap.put(Entity.class, EntityArgumentType::entity);
        argTypeMap.put(GameProfile.class, GameProfileArgumentType::gameProfile);
        argTypeMap.put(BlockPos.class, BlockPosArgumentType::blockPos);
        argTypeMap.put(ItemStack.class, ItemStackArgumentType::itemStack);
        argTypeMap.put(Integer.class, IntegerArgumentType::integer);
        argTypeMap.put(Float.class, FloatArgumentType::floatArg);
        argTypeMap.put(Double.class, DoubleArgumentType::doubleArg);
        argTypeMap.put(Long.class, LongArgumentType::longArg);
        argTypeMap.put(Boolean.class, BoolArgumentType::bool);
        argTypeMap.put(String.class, StringArgumentType::word);
    }

    @Override
    public boolean createCommand(CommandTree command) {
        ArgumentBuilder<ClientCommandSource, ?> master = literal(command.name());
        // master.then(literal("help").executes(context -> {
        //     UChat.say(command.helpString());
        //     return 0;
        // }).build());
        _create(master, command.getDedupedCommands().values());
        CommandNode<ClientCommandSource> out = master.build();
        // todo
        return true;
    }

    private static <S extends ClientCommandSource> void _create(ArgumentBuilder<S, ?> parent, Collection<List<Node>> nodesCollection) {
        for (List<Node> nodes : nodesCollection) {
            for (Node n : nodes) {
                String[] names = n.names();
                ArgumentBuilder<S, ?> builder = literal(names[0]);
                CommandNode<S> self;
                if (n instanceof Executable) {
                    Executable exe = (Executable) n;
                    for (int i = 0; i < exe.parameters.length; i++) {
                        builder = builder.then(argument(String.valueOf(i), getArgumentType(exe.parameters[i])));
                    }
                    self = builder.executes(context -> {
                        Object[] params = new Object[exe.parameters.length];
                        for (int i = 0; i < params.length; i++) {
                            Executable.Param p = exe.parameters[i];
                            params[i] = context.getArgument(String.valueOf(i), p.getType());
                        }
                        Object out = exe.function.apply(params);
                        if (out instanceof Number) return ((Number) out).intValue();
                        else return 0;
                    }).build();
                } else {
                    _create(builder, ((CommandTree) n).getDedupedCommands().values());
                    self = builder.build();
                }
                parent.then(self);
                for (int i = 1; i < names.length; i++) {
                    ArgumentBuilder<S, ?> builder2 = literal(names[i]);
                    parent.then(builder2.redirect(self));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentType<T> getArgumentType(Executable.Param p) {
        ArgumentParser<T> parser = (ArgumentParser<T>) p.parser;
        Class<T> parserType = parser.getType();
        Supplier<ArgumentType<?>> cached = argTypeMap.get(parserType);
        if (cached != null) return (ArgumentType<T>) cached.get();


        // no parser was found at <clinit>, just use oneconfig API
        ArgumentType<T> type = new ArgumentType<T>() {
            @Override
            public T parse(StringReader reader) throws CommandSyntaxException {
                if(p.arity == 1) return parser.parse(reader.readStringUntil(' '));
                else {
                    Object[] out = new Object[p.arity];
                    for(int i = 0; i < p.arity; i++) {
                        out[i] = parser.parse(reader.readStringUntil(' '));
                    }
                    return (T) out;
                }
            }

            @Override
            public Collection<String> getExamples() {
                return parser.getAutoCompletions("");
            }

            @Override
            public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                List<String> ls = parser.getAutoCompletions(builder.getInput());
                if (ls == null) return Suggestions.empty();
                for (String s : ls) {
                    builder.suggest(s);
                }
                return builder.buildFuture();
            }
        };
        argTypeMap.put(parserType, () -> type);
        return type;
    }

    private static CommandDispatcher<ClientCommandSource> activeDispatcher;

    public static void setActiveDispatcher(@Nullable CommandDispatcher<ClientCommandSource> dispatcher) {
        activeDispatcher = dispatcher;
    }

    public static CommandDispatcher<ClientCommandSource> getActiveDispatcher() {
        return activeDispatcher;
    }

    /**
     * Executes a client-sided command. Callers should ensure that this is only called
     * on slash-prefixed messages and the slash needs to be removed before calling.
     * (This is the same requirement as {@code ClientPlayerEntity#sendCommand}.)
     *
     * @param command the command with slash removed
     * @return true if the command should not be sent to the server, false otherwise
     */
    public static boolean executeCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();

        // The interface is implemented on ClientCommandSource with a mixin.
        // noinspection ConstantConditions
        ClientCommandSource commandSource = (ClientCommandSource) client.getNetworkHandler().getCommandSource();

        client.getProfiler().push(command);

        try {
            // TODO: Check for server commands before executing.
            //   This requires parsing the command, checking if they match a server command
            //   and then executing the command with the parse results.
            activeDispatcher.execute(command, commandSource);
            return true;
        } catch (CommandSyntaxException e) {
            boolean ignored = isIgnoredException(e.getType());

            if (ignored) {
                LOGGER.debug("Syntax exception for client-sided command '{}'", command, e);
                return false;
            }

            LOGGER.warn("Syntax exception for client-sided command '{}'", command, e);
            commandSource.sendError(getErrorMessage(e));
            return true;
        } catch (CommandException e) {
            LOGGER.warn("Error while executing client-sided command '{}'", command, e);
            commandSource.sendError(e.getTextMessage());
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Error while executing client-sided command '{}'", command, e);
            commandSource.sendError(Text.of(e.getMessage()));
            return true;
        } finally {
            client.getProfiler().pop();
        }
    }

    /**
     * Tests whether a command syntax exception with the type
     * should be ignored and the command sent to the server.
     *
     * @param type the exception type
     * @return true if ignored, false otherwise
     */
    private static boolean isIgnoredException(CommandExceptionType type) {
        BuiltInExceptionProvider builtins = CommandSyntaxException.BUILT_IN_EXCEPTIONS;

        // Only ignore unknown commands and node parse exceptions.
        // The argument-related dispatcher exceptions are not ignored because
        // they will only happen if the user enters a correct command.
        return type == builtins.dispatcherUnknownCommand() || type == builtins.dispatcherParseException();
    }

    // See ChatInputSuggestor.formatException. That cannot be used directly as it returns an OrderedText instead of a Text.
    private static Text getErrorMessage(CommandSyntaxException e) {
        Text message = Texts.toText(e.getRawMessage());
        String context = e.getContext();

        return context != null ? new TranslatableText("command.context.parse_error", message, e.getCursor(), context) : message;
    }

    /**
     * Runs final initialization tasks such as {@link CommandDispatcher#findAmbiguities(AmbiguityConsumer)}
     * on the command dispatcher. Also registers a {@code /fcc help} command if there are other commands present.
     */
    public static void finalizeInit() {
        // noinspection CodeBlock2Expr
        activeDispatcher.findAmbiguities((parent, child, sibling, inputs) -> {
            LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", activeDispatcher.getPath(child), activeDispatcher.getPath(sibling), inputs);
        });
    }

    private static int executeRootHelp(CommandContext<ClientCommandSource> context) {
        return executeHelp(activeDispatcher.getRoot(), context);
    }

    private static int executeArgumentHelp(CommandContext<ClientCommandSource> context) throws CommandSyntaxException {
        ParseResults<ClientCommandSource> parseResults = activeDispatcher.parse(StringArgumentType.getString(context, "command"), context.getSource());
        List<ParsedCommandNode<ClientCommandSource>> nodes = parseResults.getContext().getNodes();

        if (nodes.isEmpty()) {
            throw HelpCommandAccessor.getFailedException().create();
        }

        return executeHelp(Iterables.getLast(nodes).getNode(), context);
    }

    private static int executeHelp(CommandNode<ClientCommandSource> startNode, CommandContext<ClientCommandSource> context) {
        Map<CommandNode<ClientCommandSource>, String> commands = activeDispatcher.getSmartUsage(startNode, context.getSource());

        for (String command : commands.values()) {
            context.getSource().sendFeedback(Text.of("/" + command));
        }

        return commands.size();
    }

    public static void addCommands(CommandDispatcher<ClientCommandSource> target, ClientCommandSource source) {
        Map<CommandNode<ClientCommandSource>, CommandNode<ClientCommandSource>> originalToCopy = new HashMap<>();
        originalToCopy.put(activeDispatcher.getRoot(), target.getRoot());
        copyChildren(activeDispatcher.getRoot(), target.getRoot(), source, originalToCopy);
    }

    /**
     * Copies the child commands from origin to target, filtered by {@code child.canUse(source)}.
     * Mimics vanilla's CommandManager.makeTreeForSource.
     *
     * @param origin         the source command node
     * @param target         the target command node
     * @param source         the command source
     * @param originalToCopy a mutable map from original command nodes to their copies, used for redirects;
     *                       should contain a mapping from origin to target
     */
    private static void copyChildren(
            CommandNode<ClientCommandSource> origin,
            CommandNode<ClientCommandSource> target,
            ClientCommandSource source,
            Map<CommandNode<ClientCommandSource>, CommandNode<ClientCommandSource>> originalToCopy
    ) {
        for (CommandNode<ClientCommandSource> child : origin.getChildren()) {
            if (!child.canUse(source)) continue;

            ArgumentBuilder<ClientCommandSource, ?> builder = child.createBuilder();

            // Reset the unnecessary non-completion stuff from the builder
            builder.requires(s -> true); // This is checked with the if check above.

            if (builder.getCommand() != null) {
                builder.executes(context -> 0);
            }

            // Set up redirects
            if (builder.getRedirect() != null) {
                builder.redirect(originalToCopy.get(builder.getRedirect()));
            }

            CommandNode<ClientCommandSource> result = builder.build();
            originalToCopy.put(child, result);
            target.addChild(result);

            if (!child.getChildren().isEmpty()) {
                copyChildren(child, result, source, originalToCopy);
            }
        }
    }

    private static void registerIntrinsics() {
        CommandManager.INSTANCE.registerParser(
                new ArgumentParser<PlayerEntity>() {
                    @Override
                    public PlayerEntity parse(@NotNull String arg) {
                        throw new RuntimeException("intrinsic");
                    }

                    @Override
                    public Class<PlayerEntity> getType() {
                        return PlayerEntity.class;
                    }
                },
                new ArgumentParser<Entity>() {
                    @Override
                    public Entity parse(@NotNull String arg) {
                        throw new RuntimeException("intrinsic");
                    }

                    @Override
                    public Class<Entity> getType() {
                        return Entity.class;
                    }
                },
                new ArgumentParser<GameProfile>() {
                    @Override
                    public GameProfile parse(@NotNull String arg) {
                        throw new RuntimeException("intrinsic");
                    }

                    @Override
                    public Class<GameProfile> getType() {
                        return GameProfile.class;
                    }
                },
                new ArgumentParser<BlockPos>() {
                    @Override
                    public BlockPos parse(@NotNull String arg) {
                        throw new RuntimeException("intrinsic");
                    }

                    @Override
                    public Class<BlockPos> getType() {
                        return BlockPos.class;
                    }
                },
                new ArgumentParser<ItemStack>() {
                    @Override
                    public ItemStack parse(@NotNull String arg) {
                        throw new RuntimeException("intrinsic");
                    }

                    @Override
                    public Class<ItemStack> getType() {
                        return ItemStack.class;
                    }
                }
        );
    }
}
