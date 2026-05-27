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

package org.polyfrost.oneconfig.api.commands.v1.factories.annotated;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.commands.v1.factories.CommandFactory;
import org.polyfrost.oneconfig.api.platform.v1.commands.ClientCommandSource;
import org.polyfrost.oneconfig.utils.v1.MHUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.polyfrost.oneconfig.api.commands.v1.CommandManager.*;

@SuppressWarnings("LoggingSimilarMessage")
public class AnnotationCommandFactory implements CommandFactory {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/BrigaiderTranslator");

    @Override
    public LiteralCommandNode<ClientCommandSource>[] create(@NotNull Object obj) {
        Class<?> cls = obj.getClass();
        Command command = cls.getAnnotation(Command.class);
        if (command == null) {
            return null;
        }

        StringBuilder help = new StringBuilder();
        String primaryName = command.value().length == 0 ? cls.getSimpleName() : command.value()[0];
        help.append("Help for /").append(primaryName);
        if (command.value().length != 0) {
            help.append(" (");
            String[] aliases = command.value();
            for (int i = 0; i < aliases.length; i++) {
                help.append(aliases[i]);
                if (i < aliases.length - 1) {
                    help.append(", ");
                }
            }

            help.append(')');
        }

        LiteralArgumentBuilder<ClientCommandSource> builder = literal(primaryName);
        setup(true, builder, obj, help, "  /" + primaryName);

        @SuppressWarnings("unchecked")
        LiteralCommandNode<ClientCommandSource>[] nodes = new LiteralCommandNode[Math.max(1, command.value().length + 1)];

        builder.then(literal("help").executes((ctx) -> ctx.getSource().sendText(help.toString())));

        nodes[0] = builder.build();
        for (int i = 1; i < command.value().length; i++) {
            nodes[i] = literal(command.value()[i]).redirect(nodes[0]).build();
        }

        return nodes;
    }

    private void setup(boolean isRoot, LiteralArgumentBuilder<ClientCommandSource> tree, Object obj, StringBuilder help, String stem) {
        // Flag to check if the root help message's formatting has been set up
        // We do it this way so that it can be conditional, if there are no additional subcommands
        boolean isRootFormattingSetup = false;

        // Flag to check if the root @Executor has been set up on the tree itself
        // This is necessary as the root command is not a subcommand, but rather the main command itself, thus meaning that we cannot define it multiple times
        boolean isRootCommandSetup = false;

        // Firstly, iterate through each declared class in our object and find all classes annotated with @Command
        // Recursively set up their subcommands
        for (Class<?> clz : obj.getClass().getDeclaredClasses()) {
            Command command = clz.getAnnotation(Command.class);
            if (command == null) {
                continue;
            }

            String[] aliases = command.value();
            String primaryName = aliases.length == 0 ? clz.getSimpleName() : aliases[0];

            LiteralArgumentBuilder<ClientCommandSource> subTree = literal(primaryName);
            setup(false, subTree, MHUtils.instantiate(clz, true), help, stem + " " + primaryName);
        }

        // Now, go through the methods of this class and find all methods annotated with @Command
        // These are their own subcommands executed by the command tree
        for (Method method : obj.getClass().getDeclaredMethods()) {
            Handler handler = method.getAnnotation(Handler.class);
            if (handler == null) {
                continue;
            }

            MHUtils.setAccessible(method);

            String[] aliases = handler.value();
            String primaryName = aliases.length == 0 ? method.getName() : aliases[0];

            boolean isSectionApplied = false;
            StringBuilder currentSection = new StringBuilder();
            currentSection.append(stem).append(' ').append(primaryName); // Append the stem of our command and the primary subcommand name

            ArgumentBuilder<ClientCommandSource, ?> theMethod;
            if (method.getParameterCount() > 0) {
                String[] paramNames = new String[method.getParameterCount()];
                Class<?>[] paramTypes = method.getParameterTypes();
                Parameter[] params = method.getParameters();
                paramNames[0] = params[0].getName();

                ArgumentType<?> argumentType = getArgumentType(paramTypes[0]);
                if (argumentType == null) {
                    LOGGER.error("Failed to find argument type for parameter {} of method {} in class {}",
                            paramNames[0], method.getName(), obj.getClass().getName());
                    continue; // Skip this method if we can't find the argument type
                }

                theMethod = argument(paramNames[0], argumentType);

                // Start of parameters in help message
                if (!isRootFormattingSetup && isRoot) {
                    help.append(':').append('\n');
                    isRootFormattingSetup = true;
                }

                isSectionApplied = true;
                help.append(currentSection);
                help.append('<');
                help.append(paramNames[0]);

                Param annotation = params[0].getAnnotation(Param.class);
                if (annotation != null) {
                    help.append(": ").append(annotation.value());
                }

                for (int i = 1; i < paramNames.length; i++) {
                    ArgumentType<?> paramType = getArgumentType(paramTypes[i]);
                    if (paramType == null) {
                        LOGGER.error("Failed to find argument type for parameter {} of method {} in class {}",
                                params[i].getName(), method.getName(), obj.getClass().getName());
                        continue; // Skip this parameter if we can't find the argument type
                    }

                    Parameter p = params[i];
                    String name = p.getName();
                    help.append(", ").append(name);
                    annotation = p.getAnnotation(Param.class);
                    if (annotation != null) {
                        help.append(": ").append(annotation.value());
                    }

                    paramNames[i] = name;
                    theMethod = theMethod.then(argument(paramNames[i], paramType));
                }

                help.append('>');
                theMethod.executes((ctx) -> {
                    Object[] args = new Object[paramTypes.length];
                    for (int i = 0; i < paramTypes.length; i++) {
                        args[i] = ctx.getArgument(paramNames[i], paramTypes[i]);
                    }

                    try {
                        method.invoke(obj, args);
                        return 1;
                    } catch (Exception e) {
                        LOGGER.error("Failed to execute command!", e);
                        return ctx.getSource().replyError(e);
                    }
                });

                tree.then(literal(method.getName()).then(theMethod));
                for (String s : handler.value()) {
                    tree.then(literal(s).then(theMethod));
                }
            } else {
                if (!isRootCommandSetup && aliases.length == 0) {
                    // If this is the root command (no aliases & no parameters), we set it up directly on the tree

                    isRootCommandSetup = true; // Mark that the root command has been set up

                    // We don't need to append this subtree to the help message, as it is the root command

                    tree.executes((ctx) -> {
                        try {
                            method.invoke(obj);
                            return 1;
                        } catch (Exception e) {
                            LOGGER.error("Failed to execute command!", e);
                            return ctx.getSource().replyError(e);
                        }
                    });
                } else {
                    // Otherwise, we create a new subcommand (along with aliases) for this method
                    if (!isRootFormattingSetup && isRoot) {
                        help.append(':').append('\n');
                        isRootFormattingSetup = true;
                    }

                    isSectionApplied = true;
                    help.append(currentSection);

                    theMethod = literal(method.getName());
                    theMethod.executes((ctx) -> {
                        try {
                            method.invoke(obj);
                            return 1;
                        } catch (Exception e) {
                            LOGGER.error("Failed to execute command!", e);
                            return ctx.getSource().replyError(e);
                        }
                    });

                    tree.then(theMethod);

                    for (String s : handler.value()) {
                        tree.then(literal(s).then(theMethod));
                    }
                }
            }

            if (isSectionApplied) {
                String description = handler.description();
                if (!description.isEmpty()) {
                    help.append(": ").append(description);
                } else {
                    help.append(" (no description provided)");
                }

                help.append('\n');
            }
        }
    }

}
