/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
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

package org.polyfrost.oneconfig.internal.libs.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.polyfrost.oneconfig.api.commands.v1.internal.RegisterCommandsEvent;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.universal.UChat;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric Client-side command manager implementation.
 * <br>
 * Taken from the Fabric API under the <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache 2.0 License</a>;
 * <a href="https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/impl/command/client/ClientCommandInternals.java">Click here for source</a>
 */
public final class ClientCommandInternals {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Commands");
    private static CommandDispatcher<ClientSuggestionProvider> activeDispatcher;

    private static void setup(@Nullable CommandDispatcher<ClientSuggestionProvider> dispatcher) {
        activeDispatcher = dispatcher;
        EventManager.INSTANCE.post(new RegisterCommandsEvent(dispatcher));
        // noinspection CodeBlock2Expr
        activeDispatcher.findAmbiguities((parent, child, sibling, inputs) -> {
            LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", activeDispatcher.getPath(child), activeDispatcher.getPath(sibling), inputs);
        });
    }

    /**
     * Executes a client-sided command. The message needs to start with a / to be accepted.
     *
     * @param command the command
     * @return true if the command should not be sent to the server, false otherwise
     */
    public static boolean executeCommand(String command) {
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            LOGGER.warn("skipping execution of {} as cannot access suggestions provider", command);
            return false;
        }

        ClientSuggestionProvider commandSource = client.getConnection().getSuggestionProvider();

        IProfiler profiler = client.getProfiler();
        profiler.startSection(command);

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
            UChat.chat("&c" + getErrorMessage(e));
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error while executing client-sided command '{}'", command, e);
            UChat.chat("&c" + e.getLocalizedMessage());
            return true;
        } finally {
            profiler.endSection();
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
    private static ITextComponent getErrorMessage(CommandSyntaxException e) {
        ITextComponent message = TextComponentUtils.toTextComponent(e.getRawMessage());
        String context = e.getContext();

        return context != null ?
                //#if MC<11900
                new net.minecraft.util.text.TranslationTextComponent
                //#else
                //#if FABRIC
                //$$ Text
                //#else
                //$$ Component
                //#endif
                //$$ .translatable
                //#endif
                        ("command.context.parse_error", message.getString(), e.getCursor(), context) : message;
    }

    public static void addCommands(CommandDispatcher<ClientSuggestionProvider> target, ClientSuggestionProvider source) {
        setup(new CommandDispatcher<>());
        Map<CommandNode<ClientSuggestionProvider>, CommandNode<ClientSuggestionProvider>> originalToCopy = new HashMap<>();
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
            CommandNode<ClientSuggestionProvider> origin,
            CommandNode<ClientSuggestionProvider> target,
            ClientSuggestionProvider source,
            Map<CommandNode<ClientSuggestionProvider>, CommandNode<ClientSuggestionProvider>> originalToCopy
    ) {
        for (CommandNode<ClientSuggestionProvider> child : origin.getChildren()) {
            if (!child.canUse(source)) continue;

            ArgumentBuilder<ClientSuggestionProvider, ?> builder = child.createBuilder();

            // Reset the unnecessary non-completion stuff from the builder
            builder.requires(s -> true); // This is checked with the if check above.

            if (builder.getCommand() != null) {
                builder.executes(context -> 0);
            }

            // Set up redirects
            if (builder.getRedirect() != null) {
                builder.redirect(originalToCopy.get(builder.getRedirect()));
            }

            CommandNode<ClientSuggestionProvider> result = builder.build();
            originalToCopy.put(child, result);
            target.addChild(result);

            if (!child.getChildren().isEmpty()) {
                copyChildren(child, result, source, originalToCopy);
            }
        }
    }
}