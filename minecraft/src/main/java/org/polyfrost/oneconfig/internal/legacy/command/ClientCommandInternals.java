/*
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
 */

package org.polyfrost.oneconfig.internal.legacy.command;

//? if = 1.8.9 {
/*import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ClientCommandInternals {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCommandInternals.class);
    private static CommandDispatcher<FabricClientCommandSource> activeDispatcher;

    public static void initializeDispatcher() {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        activeDispatcher = dispatcher;
        ClientCommandRegistrationCallback.EVENT.invoke(dispatcher, null);
        finalizeInit();
    }

    /^*
     * Executes a client-sided command. Callers should ensure that this is only called
     * on slash-prefixed messages and the slash needs to be removed before calling.
     *
     * @param command the command with slash removed
     * @return true if the command should not be sent to the server, false otherwise
     ^/
    public static boolean executeCommand(String command) {
        FabricClientCommandSource source = new FabricClientCommandSource() {};

        try {
            // TODO: Check for server commands before executing.
            //   This requires parsing the command, checking if they match a server command
            //   and then executing the command with the parse results.
            activeDispatcher.execute(command, source);
            return true;
        } catch (CommandSyntaxException e) {
            boolean ignored = isIgnoredException(e.getType());

            if (ignored) {
                LOGGER.debug("Syntax exception for client-sided command '{}'", command, e);
                return false;
            }

            LOGGER.warn("Syntax exception for client-sided command '{}'", command, e);
            source.sendError(getErrorMessage(e));
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error while executing client-sided command '{}'", command, e);
            source.sendError(Component.nullToEmpty(e.getMessage()));
            return true;
        }
    }

    public static CompletableFuture<String[]> getCompletions(String text) {
        if (!text.startsWith("/")) {
            return CompletableFuture.completedFuture(new String[0]);
        }

        String command = text.substring(1);
        FabricClientCommandSource source = new FabricClientCommandSource() {
        };
        return activeDispatcher.getCompletionSuggestions(activeDispatcher.parse(command, source))
                .thenApply(suggestions -> convertSuggestions(activeDispatcher, source, command, suggestions));
    }

    public static String[] mergeSuggestions(String[] serverSuggestions, String[] clientSuggestions) {
        if (clientSuggestions.length == 0) return serverSuggestions;

        LinkedHashMap<String, String> merged = new LinkedHashMap<>();
        for (String suggestion : clientSuggestions) merged.put(suggestion.toLowerCase(Locale.ROOT), suggestion);
        for (String suggestion : serverSuggestions) merged.putIfAbsent(suggestion.toLowerCase(Locale.ROOT), suggestion);
        return merged.values().toArray(new String[0]);
    }

    private static String[] convertSuggestions(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            FabricClientCommandSource source,
            String command,
            Suggestions suggestions
    ) {
        ArrayList<String> converted = new ArrayList<>();
        int wordStart = command.lastIndexOf(' ') + 1;
        boolean root = wordStart == 0;
        for (Suggestion suggestion : suggestions.getList()) {
            String applied = suggestion.apply(command);
            // 1.8.9 can only replace the current word
            if (!command.regionMatches(0, applied, 0, wordStart)) continue;
            var child = dispatcher.getRoot().getChild(applied);
            if (root && (child == null || !child.canUse(source))) continue;

            String replacement = root ? "/" + applied : applied.substring(wordStart);
            if (replacement.indexOf(' ') < 0) converted.add(replacement);
        }
        return converted.toArray(new String[0]);
    }

    /^*
     * Tests whether a command syntax exception with the type
     * should be ignored and the command sent to the server.
     *
     * @param type the exception type
     * @return true if ignored, false otherwise
     ^/
    private static boolean isIgnoredException(CommandExceptionType type) {
        BuiltInExceptionProvider builtins = CommandSyntaxException.BUILT_IN_EXCEPTIONS;

        // Only ignore unknown commands and node parse exceptions.
        // The argument-related dispatcher exceptions are not ignored because
        // they will only happen if the user enters a correct command.
        return type == builtins.dispatcherUnknownCommand() || type == builtins.dispatcherParseException();
    }

    private static Component getErrorMessage(CommandSyntaxException e) {
        return Component.literal(e.getMessage());
    }

    /^*
     * Runs final initialization tasks such as {@link CommandDispatcher#findAmbiguities(AmbiguityConsumer)}
     * on the command dispatcher.
     ^/
    public static void finalizeInit() {
        // noinspection CodeBlock2Expr
        activeDispatcher.findAmbiguities((parent, child, sibling, inputs) -> {
            LOGGER.warn("Ambiguity between arguments {} and {} with inputs: {}", activeDispatcher.getPath(child), activeDispatcher.getPath(sibling), inputs);
        });
    }

}
*///?}
