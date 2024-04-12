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

package org.polyfrost.oneconfig.internal.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.ChatReceiveEvent;

import org.polyfrost.oneconfig.internal.commands.RegisterCommandsEvent;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public abstract class NetHandlerPlayClientMixin {

    @Unique
    private ChatReceiveEvent oneconfig$event = null;

    @Inject(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;addChatMessage(Lnet/minecraft/network/MessageType;Lnet/minecraft/text/Text;Ljava/util/UUID;)V"), cancellable = true)
    private void onClientChat(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (oneconfig$event != null && oneconfig$event.cancelled) {
            ci.cancel();
        }
    }

    @Redirect(method = "onGameMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameMessageS2CPacket;getMessage()Lnet/minecraft/text/Text;"))
    private Text onClientChatRedirect(GameMessageS2CPacket packet) {
        if (!packet.isNonChat()) {
            oneconfig$event = new ChatReceiveEvent(packet.getMessage());
            EventManager.INSTANCE.post(oneconfig$event);
            return oneconfig$event.message;
        }
        return packet.getMessage();
    }


    // Command API //
    // Taken from Fabric API under the Apache 2.0 License //
    // Source: https://github.com/FabricMC/fabric/blob/1.20.2/fabric-command-api-v2/src/client/java/net/fabricmc/fabric/mixin/command/client/ClientPlayNetworkHandlerMixin.java //
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Shadow
    @Final
    private net.minecraft.client.network.ClientCommandSource commandSource;

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo info) {
        final CommandDispatcher<ClientCommandSource> dispatcher = new CommandDispatcher<>();
        ClientCommandInternals.setActiveDispatcher(dispatcher);
        EventManager.INSTANCE.post(new RegisterCommandsEvent(dispatcher));
        ClientCommandInternals.finalizeInit();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "onCommandTree", at = @At("RETURN"))
    private void onOnCommandTree(CommandTreeS2CPacket packet, CallbackInfo info) {
        // Add the commands to the vanilla dispatcher for completion.
        // It's done here because both the server and the client commands have
        // to be in the same dispatcher and completion results.
        ClientCommandInternals.addCommands((CommandDispatcher) commandDispatcher, (ClientCommandSource) commandSource);
    }
}