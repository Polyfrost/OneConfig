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

import dev.deftu.textile.minecraft.MCTextHolder;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class NetHandlerPlayClientMixin {
    @Unique
    private ChatEvent.Send ocfg$sendChatEvent;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void chatCallback(String message, CallbackInfo ci) {
        ocfg$sendChatEvent = new ChatEvent.Send(message);
        EventManager.INSTANCE.post(ocfg$sendChatEvent);
        if (ocfg$sendChatEvent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String modifyMessage(String message) {
        return ocfg$sendChatEvent.message;
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void chatReceiveCallback(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ChatEvent.Receive ev = new ChatEvent.Receive(MCTextHolder.convertFromVanilla(packet.comp_1103()));
        EventManager.INSTANCE.post(ev);
        if (ev.cancelled) {
            ci.cancel();
        }
    }

}
