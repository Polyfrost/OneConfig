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

package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.entity.EntityPlayerSP;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class Mixin_ChatSendEvent {
    @Unique private ChatEvent.Send ocfg$chatEvent;

    @Inject(
            //#if MC >= 1.21.2
            //$$ method = "displayClientMessage",
            //#elseif MC >= 1.19.2
            //$$ method = "sendSystemMessage",
            //#else
            method = "sendChatMessage",
            //#endif
            at = @At("HEAD"),
            cancellable = true
    )
    public void chatCallback(
            //#if MC < 1.19
            String message,
            //#else
            //$$ net.minecraft.network.chat.Component text,
            //#endif
            //#if MC >= 1.21.2
            //$$ boolean toHud,
            //#endif
            CallbackInfo ci
    ) {
        //#if MC >= 1.21.2
        //$$ if (toHud) return;
        //#endif

        //#if MC >= 1.19
        //$$ String message = text.getString();
        //#endif
        //#if MC >= 1.16
        //$$ // if (org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals.executeCommand(message)) {
        //$$ //     ci.cancel();
        //$$ // }
        //#endif

        ocfg$chatEvent = new ChatEvent.Send(message);

        EventManager.INSTANCE.post(ocfg$chatEvent);

        if (ocfg$chatEvent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(
            //#if MC >= 1.21.2
            //$$ method = "displayClientMessage",
            //#elseif MC >= 1.19.2
            //$$ method = "sendSystemMessage",
            //#else
            method = "sendChatMessage",
            //#endif
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    public String modifyMessage(String message) {
        return ocfg$chatEvent.message;
    }
}
