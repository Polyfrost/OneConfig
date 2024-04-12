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

package org.polyfrost.oneconfig.internal.commands;

import com.mojang.authlib.GameProfile;
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.commands.CommandManager;
import org.polyfrost.oneconfig.api.commands.CommandTree;
import org.polyfrost.oneconfig.api.commands.Executable;
import org.polyfrost.oneconfig.api.commands.Node;
import org.polyfrost.oneconfig.api.commands.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.internal.PlatformCommandManager;
import org.polyfrost.oneconfig.api.events.invoke.EventHandler;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

@ApiStatus.Internal
public class PlatformCommandManagerImpl implements PlatformCommandManager {
    private static final Map<Class<?>, Supplier<ArgumentType<?>>> argTypeMap = new HashMap<>();
    private static List<CommandNode<ClientCommandSource>> nodes = new ArrayList<>();


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

        EventHandler.of(RegisterCommandsEvent.class, e -> {
            for (CommandNode<ClientCommandSource> n : nodes) {
                e.dispatcher.getRoot().addChild(n);
            }
            nodes = null;
        }).register();
    }

    @Override
    public boolean createCommand(CommandTree command) {
        ArgumentBuilder<ClientCommandSource, ?> master = literal(command.name());
        // master.then(literal("help").executes(context -> {
        //     UChat.say(command.helpString());
        //     return 0;
        // }).build());
        _create(master, command.getDedupedCommands().values());
        // build, and wait to be added to the dispatcher
        nodes.add(master.build());
        return true;
    }

    private static <S extends ClientCommandSource> void _create(ArgumentBuilder<S, ?> parent, Collection<List<Node>> nodesCollection) {
        for (List<Node> nodes : nodesCollection) {
            for (Node n : nodes) {
                String[] names = n.names();
                ArgumentBuilder<S, ?> builder = names[0].isEmpty() ? parent : literal(names[0]);
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
                if(!builder.equals(parent)) parent.then(self);
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
                if (p.arity == 1) return parser.parse(reader.readStringUntil(' '));
                else {
                    Object[] out = new Object[p.arity];
                    for (int i = 0; i < p.arity; i++) {
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
