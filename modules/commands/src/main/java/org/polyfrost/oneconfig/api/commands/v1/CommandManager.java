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

package org.polyfrost.oneconfig.api.commands.v1;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.deftu.omnicore.api.client.commands.OmniClientCommandSource;
import dev.deftu.omnicore.api.client.commands.OmniClientCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.commands.v1.factories.CommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.AnnotationCommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Command;
import org.polyfrost.oneconfig.utils.v1.WrappingUtils;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles the registration of OneConfig commands.
 *
 * @see Command
 */
public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("OneConfig/CommandManger");

    /**
     * The singleton instance of the command manager.
     */
    public static final CommandManager INSTANCE = new CommandManager();
    private final Set<CommandFactory> factories = new HashSet<>();
    private final Map<Class<?>, ArgumentType<?>> argumentTypes = new IdentityHashMap<>();


    private CommandManager() {
        argumentTypes.put(int.class, IntegerArgumentType.integer());
        argumentTypes.put(String.class, StringArgumentType.string());
        argumentTypes.put(String[].class, StringArgumentType.greedyString());
        argumentTypes.put(boolean.class, BoolArgumentType.bool());
        argumentTypes.put(float.class, FloatArgumentType.floatArg());
        argumentTypes.put(double.class, DoubleArgumentType.doubleArg());
        argumentTypes.put(long.class, LongArgumentType.longArg());
        registerFactory(new AnnotationCommandFactory());
    }

    public static void register(LiteralArgumentBuilder<OmniClientCommandSource> builder) {
        register(builder.build());
    }

    public static void register(LiteralCommandNode<OmniClientCommandSource> node) {
        OmniClientCommands.register(node);
    }

    public static boolean register(Object obj) {
        LiteralCommandNode<OmniClientCommandSource>[] nodes = INSTANCE.create(obj);
        if (nodes == null) {
            return false;
        }

        for (LiteralCommandNode<OmniClientCommandSource> node : nodes) {
            if (node == null) {
                LOGGER.warn("Command node for object {} is null, skipping registration", obj);
                continue;
            }

            OmniClientCommands.register(node);
//            LOGGER.info("Registered command node for object {}: {}", obj, node.getName());
        }

        return true;
    }

    public LiteralCommandNode<OmniClientCommandSource>[] create(Object obj) {
        for (CommandFactory factory : factories) {
            try {
                LiteralCommandNode<OmniClientCommandSource>[] nodes = factory.create(obj);
                if (nodes != null) {
                    return nodes;
                }
            } catch (Exception e) {
                LOGGER.error("Command factory {} failed with an exception trying to create commands for object {}", factory, obj, e);
            }
        }

        return null;
    }

    public static LiteralArgumentBuilder<OmniClientCommandSource> literal(String name) {
        return OmniClientCommands.literal(name);
    }

    public static <T> RequiredArgumentBuilder<OmniClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        if (type == null) {
            throw new NullPointerException("Can't get argument type for argument '" + name + "' as type is null");
        }

        return OmniClientCommands.argument(name, type);
    }

    /**
     * Register a factory which can be used to create commands from objects in the {@link #create(Object)} method.
     */
    public void registerFactory(CommandFactory factory) {
        factories.add(factory);
    }

    public <T> void registerArgumentType(Class<T> type, ArgumentType<T> argumentType) {
        argumentTypes.put(type, argumentType);
    }

    @SuppressWarnings("unchecked")
    public static <T> ArgumentType<T> getArgumentType(Class<T> type) {
        return (ArgumentType<T>) INSTANCE.argumentTypes.get(WrappingUtils.getUnwrapped(type));
    }

}
