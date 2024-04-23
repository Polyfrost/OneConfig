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

package org.polyfrost.oneconfig.internal.mixin.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.play.server.SCommandListPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.internal.commands.RegisterCommandsEvent;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Command API //
    // Modified from Fabric API under the Apache 2.0 License //
    // Source: https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/mixin/command/client/ClientPlayNetworkHandlerMixin.java //
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Shadow
    @Final
    private net.minecraft.client.multiplayer.ClientSuggestionProvider clientSuggestionProvider;

    @Inject(method = "handleJoinGame", at = @At("RETURN"))
    private void onGameJoin(SJoinGamePacket packet, CallbackInfo info) {
        final CommandDispatcher<ClientCommandSource> dispatcher = new CommandDispatcher<>();
        ClientCommandInternals.setActiveDispatcher(dispatcher);
        EventManager.INSTANCE.post(new RegisterCommandsEvent(dispatcher));
        ClientCommandInternals.finalizeInit();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "handleCommandList", at = @At("RETURN"))
    private void onOnCommandTree(SCommandListPacket packet, CallbackInfo info) {
        // Add the commands to the vanilla dispatcher for completion.
        // It's done here because both the server and the client commands have
        // to be in the same dispatcher and completion results.
        ClientCommandInternals.addCommands((CommandDispatcher) this.commandDispatcher, (ClientCommandSource) this.clientSuggestionProvider);
    }
}
