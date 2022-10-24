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
import cc.polyfrost.oneconfig.utils.StringUtils;
import cc.polyfrost.oneconfig.utils.commands.annotations.Description;
import cc.polyfrost.oneconfig.utils.commands.arguements.PlayerArgumentParser;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.*;

import static cc.polyfrost.oneconfig.utils.commands.CommandManager.*;

public class PlatformCommandManagerImpl extends PlatformCommandManager {

    static {
        INSTANCE.addParser(new PlayerArgumentParser());
    }

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
                        Description description = currentParam.isAnnotationPresent(Description.class) ? currentParam.getAnnotation(Description.class) : null;
                        String[] targets = description != null && description.autoCompletesTo().length != 0 ? description.autoCompletesTo() : null;
                        if (targets != null) {
                            opts.addAll(Arrays.asList(targets));
                        }
                        opts.addAll(INSTANCE.parsers.get(currentParam.getType()).complete(args[args.length - 1], currentParam));
                    } else {
                        opts.addAll(getApplicableOptsFor(args));
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
                //TODO: better fix lmao
                return new String[] { root.mainMethod.invoke(args) };
                //return new String[]{root.getMetadata().chatColor() + NOT_FOUND_TEXT.replace("@ROOT_COMMAND@", root.getMetadata().value())};
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
                    argsIn = StringUtils.substringToLastIndexOf(argsIn, DELIMITER);
                }
                return null;
            }

            private Collection<String> getApplicableOptsFor(String[] args) {
                // isn't it amazing when you come to a somewhat elegant solution to a problem
                final Set<String> opts = new HashSet<>();
                final String current = String.join(DELIMITER, args);
                root.commandsMap.keySet().forEach(paths -> {
                    for (String p : paths) {
                        if (p.endsWith(MAIN_METHOD_NAME)) continue;
                        if (!p.startsWith(current)) continue;
                        final String[] split = p.split(DELIMITER);
                        if (args.length - 1 < split.length) {
                            final String s = split[args.length - 1];
                            if (s.isEmpty()) continue;
                            opts.add(s);
                        }
                    }
                });
                // remove main when it was mainMethod of a command
                opts.remove("main");
                return opts;
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