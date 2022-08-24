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
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.ClientCommandHandler;

import java.util.*;
import java.util.stream.Collectors;

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
                    String[] result = root.doCommand(args);
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
                return root.getTabCompletionOptions(args);
            }
        });
        //#else
        //#endif
    }

    @Override
    List<String> getPlayerNames() {
        if(Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.sendQueue != null) {
            // is it sad that I know how to do this off by heart now?
            return Minecraft.getMinecraft().thePlayer.sendQueue.getPlayerInfoMap().stream().map(info -> info.getGameProfile().getName()).collect(Collectors.toList());
        } else return null;
    }
}
//#endif