/* This file contains an adaptation of code from Fabric API (FabricMC/fabric)
 * Project found at <https://github.com/FabricMC/fabric/tree/1.18.2/>
 * For the avoidance of doubt, this file is still licensed under the terms
 * of OneConfig's Licensing.
 *
 *                 LICENSE NOTICE FOR ADAPTED CODE
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Significant changes (as required by the Apache 2.0):
 * - Removed Fabric "help" command.
 * - Removed unnecessary Fabric event for command registration.
 * - Renamed some classes that use Mixin to use MCP mappings instead.
 * - Refactored to use MCP mappings.
 *
 * As per the terms of the Apache 2.0 License, a copy of the License
 * is found at `src/main/resources/licenses/Fabric-License.txt`.
 */

/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost and FabricMC.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponentUtils;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static cc.polyfrost.oneconfig.utils.commands.ClientCommandManager.DISPATCHER;

public final class ClientCommandInternals {
    private static final Logger LOGGER = LogManager.getLogger(ClientCommandInternals.class);
    private static final char PREFIX = '/';

    /**
     * Executes a client-sided command from a message.
     *
     * @param message the command message
     * @return true if the message should not be sent to the server, false otherwise
     */
    public static boolean executeCommand(String message) {
        if (message.isEmpty()) {
            return false; // Nothing to process
        }

        if (message.charAt(0) != PREFIX) {
            return false; // Incorrect prefix, won't execute anything.
        }

        Minecraft client = Minecraft.getInstance();

        // The interface is implemented on ClientCommandSource with a mixin.
        // noinspection ConstantConditions
        FabricClientCommandSource commandSource = (FabricClientCommandSource) client.getConnection().getSuggestionProvider();

        client.getProfiler().startSection(message);

        try {
            // TODO: Check for server commands before executing.
            //   This requires parsing the command, checking if they match a server command
            //   and then executing the command with the parse results.
            DISPATCHER.execute(message.substring(1), commandSource);
            return true;
        } catch (CommandSyntaxException e) {
            boolean ignored = isIgnoredException(e.getType());

            if (ignored) {
                LOGGER.debug("Syntax exception for client-sided command '{}'", message, e);
                return false;
            }

            LOGGER.warn("Syntax exception for client-sided command '{}'", message, e);
            commandSource.sendError(getErrorMessage(e));
            return true;
        } catch (CommandException e) {
            LOGGER.warn("Error while executing client-sided command '{}'", message, e);
            commandSource.sendError(e.getComponent());
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Error while executing client-sided command '{}'", message, e);
            commandSource.sendError(new StringTextComponent(e.getMessage()));
            return true;
        } finally {
            client.getProfiler().endSection();
        }
    }

    /**
     * Tests whether a command syntax exception with the type
     * should be ignored and the message sent to the server.
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

    // See CommandSuggestor.method_30505. That cannot be used directly as it returns an OrderedText instead of a Text.
    private static ITextComponent getErrorMessage(CommandSyntaxException e) {
        ITextComponent message = TextComponentUtils.toTextComponent(e.getRawMessage());
        String context = e.getContext();

        return context != null ? new TranslationTextComponent("command.context.parse_error", message, context) : message;
    }

    public static void finalizeInit() {
        // noinspection CodeBlock2Expr
        DISPATCHER.findAmbiguities((parent, child, sibling, inputs) -> {
            LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", DISPATCHER.getPath(child), DISPATCHER.getPath(sibling), inputs);
        });
    }

    public static void addCommands(CommandDispatcher<FabricClientCommandSource> target, FabricClientCommandSource source) {
        Map<CommandNode<FabricClientCommandSource>, CommandNode<FabricClientCommandSource>> originalToCopy = new HashMap<>();
        originalToCopy.put(DISPATCHER.getRoot(), target.getRoot());
        copyChildren(DISPATCHER.getRoot(), target.getRoot(), source, originalToCopy);
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
            CommandNode<FabricClientCommandSource> origin,
            CommandNode<FabricClientCommandSource> target,
            FabricClientCommandSource source,
            Map<CommandNode<FabricClientCommandSource>, CommandNode<FabricClientCommandSource>> originalToCopy
    ) {
        for (CommandNode<FabricClientCommandSource> child : origin.getChildren()) {
            if (!child.canUse(source)) continue;

            ArgumentBuilder<FabricClientCommandSource, ?> builder = child.createBuilder();

            // Reset the unnecessary non-completion stuff from the builder
            builder.requires(s -> true); // This is checked with the if check above.

            if (builder.getCommand() != null) {
                builder.executes(context -> 0);
            }

            // Set up redirects
            if (builder.getRedirect() != null) {
                builder.redirect(originalToCopy.get(builder.getRedirect()));
            }

            CommandNode<FabricClientCommandSource> result = builder.build();
            originalToCopy.put(child, result);
            target.addChild(result);

            if (!child.getChildren().isEmpty()) {
                copyChildren(child, result, source, originalToCopy);
            }
        }
    }
}