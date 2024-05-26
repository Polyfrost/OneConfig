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

package org.polyfrost.oneconfig.api.commands.v1.internal;
//#if MC<=11202

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.command.CommandException;
import net.minecraft.server.command.CommandRegistry;
import net.minecraft.command.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.List;

import static net.minecraft.util.Formatting.GRAY;
import static net.minecraft.util.Formatting.RESET;

/**
 * The class that handles client-side chat commands. You should register any
 * commands that you want handled on the client with this command handler.
 * <p>
 * If there is a command with the same name registered both on the server and
 * client, the client takes precedence!
 * <p>
 * Taken from <a href="https://github.com/MinecraftForge/MinecraftForge/tree/1.8.9/">...</a> under LGPL 2.1 and the Minecraft Forge Public Licence with additional terms specified in the LICENSE file.
 */
public class ClientCommandHandler extends CommandRegistry {
    public static final ClientCommandHandler instance = new ClientCommandHandler();

    public String[] latestAutoComplete = null;

    /**
     * @return 1 if successfully executed, -1 if no permission or wrong usage,
     * 0 if it doesn't exist or it was canceled (it's sent to the server)
     */
    @Override
    public int execute(CommandSource sender, String message) {
        message = message.trim();

        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        String[] temp = message.split(" ");
        String[] args = new String[temp.length - 1];
        String commandName = temp[0];
        System.arraycopy(temp, 1, args, 0, args.length);
        Command icommand = getCommandMap().get(commandName);

        try {
            if (icommand == null) {
                return 0;
            }

            if (icommand.
                    //#if MC<=10809
                            isAccessible(sender)
                //#else
                //$$ method_3278(getServer(), sender)
                //#endif
            ) {

                icommand.
                        //#if MC<=10809
                                execute(sender, args)
                //#else
                //$$ method_3279(getServer(), sender, args)
                //#endif
                ;
                return 1;
            } else {
                sender.sendMessage(format("commands.generic.permission"));
            }
        } catch (IncorrectUsageException wue) {
            sender.sendMessage(format("commands.generic.usage", format(wue.getMessage(), wue.getArgs())));
        } catch (CommandException ce) {
            sender.sendMessage(format(ce.getMessage(), ce.getArgs()));
        } catch (Throwable t) {
            sender.sendMessage(format("commands.generic.exception"));
            t.printStackTrace();
        }

        return -1;
    }

    //Couple of helpers because the mcp names are stupid and long...
    private static TranslatableText format(String str, Object... args) {
        TranslatableText ret = new TranslatableText(str, args);
        ret.getStyle().setFormatting(Formatting.RED);
        return ret;
    }

    public void autoComplete(String leftOfCursor) {
        latestAutoComplete = null;

        if (leftOfCursor.charAt(0) == '/') {
            leftOfCursor = leftOfCursor.substring(1);

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof ChatScreen) {
                List<String> commands = getCompletions(mc.player, leftOfCursor, mc.player.getBlockPos());
                if (commands != null && !commands.isEmpty()) {
                    if (leftOfCursor.indexOf(' ') == -1) {
                        for (int i = 0; i < commands.size(); i++) {
                            commands.set(i, GRAY + "/" + commands.get(i) + RESET);
                        }
                    } else {
                        for (int i = 0; i < commands.size(); i++) {
                            commands.set(i, GRAY + commands.get(i) + RESET);
                        }
                    }

                    latestAutoComplete = commands.toArray(new String[0]);
                }
            }
        }
    }

    //#if MC>10809
    //$$ @Override
    //$$ protected net.minecraft.server.MinecraftServer getServer() {
    //$$     return net.minecraft.client.MinecraftClient.getInstance().getServer();
    //$$ }
    //#endif
}
//#endif