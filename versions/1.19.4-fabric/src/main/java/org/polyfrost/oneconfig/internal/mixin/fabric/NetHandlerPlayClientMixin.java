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

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatReceiveEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ChatSendEvent;
import org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayNetworkHandler.class)
public class NetHandlerPlayClientMixin {
    @Unique
    private ChatSendEvent ocfg$sendchatevent;

    @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
    private void ocfg$commands$execute(String command, CallbackInfoReturnable<Boolean> cir) {
        if (ClientCommandInternals.executeCommand(command)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void ocfg$commands$execute(String command, CallbackInfo info) {
        if (ClientCommandInternals.executeCommand(command)) {
            info.cancel();
        }
    }

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void ocfg$chatCallback(String message, CallbackInfo ci) {
        ocfg$sendchatevent = new ChatSendEvent(message);
        EventManager.INSTANCE.post(ocfg$sendchatevent);
        if (ocfg$sendchatevent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String ocfg$modifyMessage(String message) {
        return ocfg$sendchatevent.message;
    }

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void ocfg$chatRecieveCallback(ChatMessageS2CPacket packet, CallbackInfo ci) {
        ChatReceiveEvent ev = new ChatReceiveEvent(packet.unsignedContent());
        EventManager.INSTANCE.post(ev);
        if (ev.cancelled) {
            ci.cancel();
        }
    }

}
