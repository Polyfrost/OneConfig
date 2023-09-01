/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.internal.command;
//#if MC<=11202 && FABRIC==1

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHandler;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import java.util.List;

import static net.minecraft.util.EnumChatFormatting.GRAY;
import static net.minecraft.util.EnumChatFormatting.RESET;

/**
 * The class that handles client-side chat commands. You should register any
 * commands that you want handled on the client with this command handler.
 * <p>
 * If there is a command with the same name registered both on the server and
 * client, the client takes precedence!
 * <p>
 * Taken from https://github.com/MinecraftForge/MinecraftForge/tree/1.8.9/ under LGPL 2.1 and the Minecraft Forge Public Licence with additional terms specified in the LICENSE file.
 */
public class ClientCommandHandler extends CommandHandler {
    public static final ClientCommandHandler instance = new ClientCommandHandler();

    public String[] latestAutoComplete = null;

    /**
     * @return 1 if successfully executed, -1 if no permission or wrong usage,
     * 0 if it doesn't exist or it was canceled (it's sent to the server)
     */
    @Override
    public int executeCommand(ICommandSender sender, String message) {
        message = message.trim();

        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        String[] temp = message.split(" ");
        String[] args = new String[temp.length - 1];
        String commandName = temp[0];
        System.arraycopy(temp, 1, args, 0, args.length);
        ICommand icommand = getCommands().get(commandName);

        try {
            if (icommand == null) {
                return 0;
            }

            if (icommand.
                    //#if MC<=10809
                            canCommandSenderUseCommand(sender)
                //#else
                //$$ method_3278(getServer(), sender)
                //#endif
            ) {

                icommand.
                        //#if MC<=10809
                                processCommand(sender, args)
                //#else
                //$$ method_3279(getServer(), sender, args)
                //#endif
                ;
                return 1;
            } else {
                sender.addChatMessage(format("commands.generic.permission"));
            }
        } catch (WrongUsageException wue) {
            sender.addChatMessage(format("commands.generic.usage", format(wue.getMessage(), wue.getErrorObjects())));
        } catch (CommandException ce) {
            sender.addChatMessage(format(ce.getMessage(), ce.getErrorObjects()));
        } catch (Throwable t) {
            sender.addChatMessage(format("commands.generic.exception"));
            t.printStackTrace();
        }

        return -1;
    }

    //Couple of helpers because the mcp names are stupid and long...
    private static ChatComponentTranslation format(String str, Object... args) {
        ChatComponentTranslation ret = new ChatComponentTranslation(str, args);
        ret.getChatStyle().setColor(EnumChatFormatting.RED);
        return ret;
    }

    public void autoComplete(String leftOfCursor) {
        latestAutoComplete = null;

        if (leftOfCursor.charAt(0) == '/') {
            leftOfCursor = leftOfCursor.substring(1);

            Minecraft mc = Minecraft.getMinecraft();
            if (mc.currentScreen instanceof GuiChat) {
                List<String> commands = getTabCompletionOptions(mc.thePlayer, leftOfCursor, mc.thePlayer.getPosition());
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