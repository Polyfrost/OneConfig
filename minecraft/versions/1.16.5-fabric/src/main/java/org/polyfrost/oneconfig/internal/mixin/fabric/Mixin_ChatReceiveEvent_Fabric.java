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

package org.polyfrost.oneconfig.internal.mixin.fabric;

import dev.deftu.textile.minecraft.MCText;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPacketListener.class, priority = Integer.MAX_VALUE)
public abstract class Mixin_ChatReceiveEvent_Fabric {
    @Unique private ChatEvent.Receive ocfg$chatEvent = null;

    @Inject(
            method = "handleChat",
            at = @At(
                    value = "INVOKE",
                    //#if MC >= 1.19.2
                    //$$ target = "Lnet/minecraft/client/network/message/MessageHandler;onGameMessage(Lnet/minecraft/text/Text;Z)V"
                    //#else
                    target = "Lnet/minecraft/client/gui/Gui;handleChat(Lnet/minecraft/network/chat/ChatType;Lnet/minecraft/network/chat/Component;Ljava/util/UUID;)V"
                    //#endif
            ),
            cancellable = true
    )
    private void chatCallback(ClientboundChatPacket packet, CallbackInfo ci) {
        if (ocfg$chatEvent != null && ocfg$chatEvent.cancelled) {
            ci.cancel();
        }
    }

    @Redirect(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundChatPacket;getMessage()Lnet/minecraft/network/chat/Component;"))
    private Component modifyMessage(ClientboundChatPacket packet) {
        //@formatter:off
        if (
            //#if MC < 1.17
            !packet.isSystem()
            //#elseif MC > 1.19
            //$$ !packet.comp_906()
            //#else
            //$$ packet.getLocation() == net.minecraft.network.MessageType.CHAT
            //#endif
        ) {
            ocfg$chatEvent = new ChatEvent.Receive(MCText.wrap(packet.getMessage()));
            EventManager.INSTANCE.post(ocfg$chatEvent);
            return MCText.convert(ocfg$chatEvent.getMessage());
        }

        //@formatter:on
        return packet.getMessage();
    }
}
