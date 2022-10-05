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

//#if MC>=11202
package cc.polyfrost.oneconfig.utils.commands;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.annotations.Description;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cc.polyfrost.oneconfig.utils.commands.CommandManager.*;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

    @Override
    public void createCommand(CommandManager.OCCommand root) {
        //#if MC<=11202
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getCommandName() {
                return root.getMetadata().value();
            }

            @Override
            public String getCommandUsage(ICommandSender sender) {
                return "/" + root.getMetadata().value();
            }

            @Override
            public void
                //#if MC<=10809
            processCommand(ICommandSender sender, String[] args)
            //#else
            //$$ execute(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args)
            //#endif
            {
                try {
                    String[] result = doCommand(args);
                    if (result.length != 0 && result[0] != null) {
                        for (String s : result) {
                            UChat.chat(s);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    UChat.chat(CommandManager.METHOD_RUN_ERROR.replace("@ROOT_COMMAND@", root.getMetadata().value()));
                }
            }

            @Override
            public List<String> getCommandAliases() {
                return Arrays.asList(root.getMetadata().aliases());
            }

            @Override
            public int getRequiredPermissionLevel() {
                return -1;
            }

            @Override
            public List<String>
                //#if MC<=10809
            addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos)
            //#else
            //$$ getTabCompletions(net.minecraft.server.MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos)
            //#endif
            {
                List<String> opts = new ArrayList<>();
                CommandManager.Pair<String[], CommandManager.InternalCommand> command = getCommand(args);
                try {
                    if (command != null) {
                        Parameter currentParam = command.getValue().getUnderlyingMethod().getParameters()[command.getKey().length - 1];
                        String type = currentParam.getType().getSimpleName();
                        boolean isNumeric = type.equalsIgnoreCase("int") || type.equalsIgnoreCase("long") ||
                                type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float") || type.equalsIgnoreCase("integer");

                        Description description = currentParam.isAnnotationPresent(Description.class) ? currentParam.getAnnotation(Description.class) : null;
                        String[] targets = description != null && description.autoCompletesTo().length != 0 ? description.autoCompletesTo() : null;
                        if (targets != null) {
                            opts.addAll(Arrays.asList(targets));
                        } else {
                            // yes.
                            if (isNumeric) opts.add("0");
                            else if (type.equalsIgnoreCase("boolean")) {
                                opts.add("true");
                                opts.add("false");
                            } else if (type.equalsIgnoreCase("string")) {
                                opts.add("String");
                            }
                        }
                    } else {
                        String current = String.join(DELIMITER, args);
                        root.commandsMap.forEach((keys, value) -> {
                            for (String key : keys) {
                                String toAdd;
                                if (key.contains(current)) {
                                    key = key.substring(current.length());
                                    if (key.contains(DELIMITER)) {
                                        key = key.substring(0, key.lastIndexOf(DELIMITER));
                                    }
                                }
                                if (!key.contains(DELIMITER)) toAdd = key;
                                else toAdd = key.substring(0, key.indexOf(DELIMITER));
                                if (!opts.contains(toAdd) && !toAdd.isEmpty()) {
                                    opts.add(toAdd);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                }

                return opts.isEmpty() ? null : opts;
            }

            private String[] doCommand(@NotNull String[] args) {
                if (args.length == 0) {
                    if (root.mainMethod != null) return new String[]{root.mainMethod.invoke()};
                    else return root.helpCommand;
                } else if (args[0].equalsIgnoreCase("help")) {
                    if (args.length == 1) {
                        return root.helpCommand;
                    } else {
                        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                        Pair<String[], CommandManager.InternalCommand> command = getCommand(newArgs);
                        return root.getAdvancedHelp(command == null ? null : command.getValue());
                    }
                } else {
                    CommandManager.Pair<String[], CommandManager.InternalCommand> command = getCommand(args);
                    if (command != null) {
                        return new String[]{command.getValue().invoke(command.getKey())};
                    }
                }
                return new String[]{root.getMetadata().chatColor() + NOT_FOUND_TEXT.replace("@ROOT_COMMAND@", root.getMetadata().value())};
            }

            /**
             * Convert the String[] args into a processable command
             */
            @Nullable
            private CommandManager.Pair<String[], CommandManager.InternalCommand> getCommand(String[] args) {
                // turn the given args into a 'path'
                String argsIn = String.join(DELIMITER, args).toLowerCase();
                for (int i = args.length - 1; i >= 0; i--) {
                    // work backwards to find the first match
                    CommandManager.InternalCommand command = get(root, argsIn);
                    if (command != null) {
                        // create the args for the command
                        String[] newArgs = new String[args.length - i - 1];
                        System.arraycopy(args, i + 1, newArgs, 0, args.length - i - 1);
                        // return the command and the args
                        return new CommandManager.Pair<>(newArgs, command);
                    }
                    // remove the last word
                    int target = argsIn.lastIndexOf(DELIMITER);
                    argsIn = argsIn.substring(0, target == -1 ? argsIn.length() : target);
                }
                return null;
            }
        });
        //#endif
    }


    private static CommandManager.InternalCommand get(CommandManager.OCCommand command, String in) {
        for (String[] ss : command.commandsMap.keySet()) {
            for (String s : ss) {
                if (s.equalsIgnoreCase(in) || s.equalsIgnoreCase(in + DELIMITER + MAIN_METHOD_NAME)) {
                    return command.commandsMap.get(ss);
                }
            }
        }
        return null;
    }
}
//#endif