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
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.polyfrost.oneconfig.internal.utils.ComponentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayClient.class, priority = Integer.MAX_VALUE)
public abstract class Mixin_ChatReceiveEvent_Fabric {

    //@formatter:off
    @Unique
    private static final String ONECONFIG$METHOD_TARGET =
            //#if MC<=10809
            "Lnet/minecraft/client/gui/GuiNewChat;printChatMessage(Lnet/minecraft/util/IChatComponent;)V";
            //#else
            //$$ "Lnet/minecraft/client/gui/GuiIngame;addChatMessage(Lnet/minecraft/util/text/ChatType;Lnet/minecraft/util/text/ITextComponent;)V";
            //#endif
    //@formatter:on

    @Unique
    private ChatEvent.Receive ocfg$chatEvent = null;

    @Inject(method = "handleChat", at = @At(value = "INVOKE", target = ONECONFIG$METHOD_TARGET), cancellable = true)
    private void chatCallback(S02PacketChat packet, CallbackInfo ci) {
        if (ocfg$chatEvent != null && ocfg$chatEvent.cancelled) {
            ci.cancel();
        }
    }

    @Redirect(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/S02PacketChat;getChatComponent()Lnet/minecraft/util/IChatComponent;"))
    private IChatComponent modifyMessage(S02PacketChat packet) {
        //@formatter:off
        if (
            //#if MC <= 1.8.9
            packet.getType() == 0
            //#else
            //$$ !packet.isSystem()
            //#endif
        ) {
            IChatComponent component = packet.getChatComponent();
            if (Boolean.getBoolean("oneconfig.debug.chat")) {
                System.out.println("Chat message received:\n" + ComponentHelper.prettyPrint(component));
            }

            ocfg$chatEvent = new ChatEvent.Receive(MCText.wrap(component));
            EventManager.INSTANCE.post(ocfg$chatEvent);
            return MCText.convert(ocfg$chatEvent.getMessage());
        }

        //@formatter:on
        return packet.getChatComponent();
    }
}