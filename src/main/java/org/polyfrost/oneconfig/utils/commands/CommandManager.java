/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.utils.commands;

import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.factories.CommandFactory;
import org.polyfrost.oneconfig.api.commands.factories.annotated.AnnotationCommandFactory;
import org.polyfrost.oneconfig.api.commands.factories.builder.BuilderFactory;
import org.polyfrost.oneconfig.api.commands.factories.builder.CommandBuilder;
import org.polyfrost.oneconfig.api.commands.factories.dsl.CommandDSL;
import org.polyfrost.oneconfig.api.commands.factories.dsl.DSLFactory;
import org.polyfrost.oneconfig.api.commands.internal.CommandTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Handles the registration of OneConfig commands.
 *
 * @see org.polyfrost.oneconfig.api.commands.factories.annotated.annotations.Command
 */
public class CommandManager {
    /**
     * The singleton instance of the command manager.
     */
    public static final CommandManager INSTANCE = new CommandManager();
    private static final PlatformCommandManager platform = ServiceLoader.load(PlatformCommandManager.class, PlatformCommandManager.class.getClassLoader()).iterator().next();
    private final Set<CommandFactory> factories = new HashSet<>();
    /**
     * use {@link #registerParser(ArgumentParser)} to register a parser
     */
    @ApiStatus.Internal
    public final List<ArgumentParser<?>> parsers = new ArrayList<>();

    /**
     * Register a factory which can be used to create commands from objects in the {@link #create(Object)} method.
     */
    public void registerFactory(CommandFactory factory) {
        factories.add(factory);
    }

    /**
     * Register a parser which can be used to parse arguments needed by commands.
     */
    public void registerParser(ArgumentParser<?> parser) {
        parsers.add(parser);
    }

    private CommandManager() {
        Arrays.stream(ArgumentParser.defaultParsers).forEach(this::registerParser);
        registerFactory(new DSLFactory());
        registerFactory(new AnnotationCommandFactory());
        registerFactory(new BuilderFactory());
    }

    /**
     * Create a command from the provided object.
     * <br>
     * The details of this process are down to the registered {@link CommandFactory} instances.
     *
     * @return true if a command was created, false otherwise (meaning no factory was able to process the given object into a command)
     */
    public boolean create(Object obj) {
        return createTree(obj) != null;
    }

    public static CommandDSL dsl(String... aliases) {
        return new CommandDSL(INSTANCE.parsers, aliases);
    }

    public static CommandBuilder builder(String... aliases) {
        return new CommandBuilder(INSTANCE.parsers, aliases);
    }

    public static boolean registerCommand(Object obj) {
        return INSTANCE.create(obj);
    }

    /**
     * Create a command from the given object
     * <br>
     * Marked as internal as CommandTree is internal API and should not be used directly.
     *
     * @see #create(Object)
     */
    @ApiStatus.Internal
    public CommandTree createTree(Object obj) {
        for (CommandFactory f : factories) {
            CommandTree t = f.create(parsers, obj);
            if (t != null) {
                t.init();
                platform.createCommand(t);
                return t;
            }
        }
        return null;
    }
}
