/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

//#if FORGE==1
package org.polyfrost.oneconfig.internal.mixin;

import org.polyfrost.oneconfig.events.EventManager;
import org.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EventBus.class)
public class EventBusMixin {

    @Inject(method = "post", at = @At(value = "HEAD"), remap = false)
    private void postReceiveEvent(Event e, CallbackInfoReturnable<Boolean> cir) {
        if(!(e instanceof ClientChatReceivedEvent)) return;
        ClientChatReceivedEvent event = (ClientChatReceivedEvent) e;
        if (event.type == 0) {
            ChatReceiveEvent customEvent = new ChatReceiveEvent(event.message);
            EventManager.INSTANCE.post(customEvent);
            event.message = customEvent.message;
            if (customEvent.isCancelled) {
                e.setCanceled(true);
            }
        }
    }
}
//#endif