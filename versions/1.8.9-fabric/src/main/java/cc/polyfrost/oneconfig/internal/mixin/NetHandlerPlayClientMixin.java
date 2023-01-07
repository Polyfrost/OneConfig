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

package cc.polyfrost.oneconfig.internal.mixin;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = Integer.MAX_VALUE)
public class NetHandlerPlayClientMixin {

    private static final String TARGET =
            //#if MC<=10809
            "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;)V";
            //#else
            //$$ "Lnet/minecraft/client/gui/hud/InGameHud;method_14471(Lnet/minecraft/util/ChatMessageType;Lnet/minecraft/text/Text;)V";
            //#endif

    @Unique
    private Text oneconfig$newMessage = null;

    @Inject(method = "onChatMessage", at = @At(value = "INVOKE", target = TARGET), cancellable = true)
    private void onClientChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
        if (
                //#if MC<=10809
                packet.getType() == 0
                //#else
                //$$ !packet.isNonChat()
                //#endif
        ) {
            ChatReceiveEvent event = new ChatReceiveEvent(packet.getMessage());
            EventManager.INSTANCE.post(event);
            if (event.isCancelled) {
                ci.cancel();
            }
            oneconfig$newMessage = event.message;
        } else {
            oneconfig$newMessage = null;
        }
    }

    @Redirect(method = "onChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChatMessageS2CPacket;getMessage()Lnet/minecraft/text/Text;"))
    private Text onClientChatRedirect(ChatMessageS2CPacket instance) {
        return oneconfig$newMessage == null ? instance.getMessage() : oneconfig$newMessage;
    }
}