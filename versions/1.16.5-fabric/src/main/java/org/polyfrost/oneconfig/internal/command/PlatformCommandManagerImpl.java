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
import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandException;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.CommandManager;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.arguments.PlayerArgumentParser;
import org.polyfrost.oneconfig.internal.mixin.HelpCommandAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fabric Client-side command manager implementation.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/impl/command/client/ClientCommandInternals.java">Click here for source</a>
 */
@ApiStatus.Internal
public class PlatformCommandManagerImpl implements PlatformCommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Commands");

    static {
        CommandManager.INSTANCE.registerParser(new PlayerArgumentParser());
    }

    @Override
    public void createCommand(CommandTree command) {

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
}
