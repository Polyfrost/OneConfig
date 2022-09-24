/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Description;
import cc.polyfrost.oneconfig.utils.commands.annotations.Greedy;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Map;

import static cc.polyfrost.oneconfig.utils.commands.ClientCommandManager.*;
import static cc.polyfrost.oneconfig.utils.commands.CommandManager.DELIMITER;
import static cc.polyfrost.oneconfig.utils.commands.CommandManager.MAIN_METHOD_NAME;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

    @Override
    void createCommand(CommandManager.OCCommand cmd) {
        try {
            final LiteralArgumentBuilder<FabricClientCommandSource> master = literal(cmd.getMetadata().value());
            if (cmd.mainMethod != null) {
                master.executes(context -> {
                    chat(cmd.mainMethod.invoke());
                    return 1;
                });
            } else {
                master.executes(context -> {
                    chat(cmd.helpCommand);
                    return 1;
                });
            }

            final LiteralCommandNode<FabricClientCommandSource> root = DISPATCHER.register(master);
            //root.addChild(createHelpNode(cmd));
            // register aliases
            for (Map.Entry<String[], CommandManager.InternalCommand> entry : cmd.commandsMap.entrySet()) {
                if (entry.getValue().getName().equals(MAIN_METHOD_NAME)) continue;
                for (String path : entry.getKey()) {
                    root.addChild(createNode(path, entry.getValue()));
                }
            }
            for (String alias : cmd.getMetadata().aliases()) {
                DISPATCHER.register(literal(alias).redirect(root));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register command " + cmd.getMetadata().value(), e);
        }
    }

    @Override
    Collection<String> getPlayerNames() {
        return null;
    }

    private static LiteralCommandNode<FabricClientCommandSource> createNode(String p, CommandManager.InternalCommand cmd) {
        String[] path = p.split(DELIMITER);
        LiteralArgumentBuilder<FabricClientCommandSource> builder = literal(path[0]);
        for (int i = 1; i < path.length; i++) {
            builder.then(literal(path[i]));
        }
        for (Parameter parameter : cmd.getUnderlyingMethod().getParameters()) {
            // TODO this doesn't work (idk why)
            // basically if the cmd is /test hello <arg1> it just does /test <arg1> and merges into the one before??
            // could be to do with the fact i am adding a child in this way and it isnt incremented
            builder.then(argument(getArgName(parameter), parameter.isAnnotationPresent(Greedy.class) ? greedyString() : word()));
        }
        builder.executes(context -> {
            if (cmd.getUnderlyingMethod().getParameterCount() == 0) {
                chat(cmd.invoke());
            } else {
                String[] args = new String[cmd.getUnderlyingMethod().getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    Parameter parameter = cmd.getUnderlyingMethod().getParameters()[i];
                    args[i] = context.getArgument(getArgName(parameter), String.class);
                }
                chat(cmd.invoke(args));
            }
            return 1;
        });
        return builder.build();
    }

    private static String getArgName(Parameter parameter) {
        return parameter.isAnnotationPresent(Description.class) ? parameter.getAnnotation(Description.class).value() : parameter.getType().getSimpleName() + "@" + parameter.getName().hashCode();
    }

    private static LiteralCommandNode<FabricClientCommandSource> createHelpNode(CommandManager.OCCommand cmd) {
        // TODO
        return null;
    }

    private static void chat(String... in) {
        if (in == null) return;
        for (String s : in) {
            UChat.chat(s);
        }
    }
}
