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
//#if MC<11900

import net.minecraft.client.entity.EntityPlayerSP;
import org.polyfrost.oneconfig.api.events.EventManager;
import org.polyfrost.oneconfig.api.events.event.ChatSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin {
    @Unique
    private ChatSendEvent oneconfig$sendchatevent;

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void sendChatMessage(String message, CallbackInfo ci) {
        //#if MC>=11600 && FABRIC
        //$$ if (org.polyfrost.oneconfig.internal.libs.fabric.ClientCommandInternals.executeCommand(message)) {
        //$$     ci.cancel();
        //$$ }
        //#endif
        oneconfig$sendchatevent = new ChatSendEvent(message);

        EventManager.INSTANCE.post(oneconfig$sendchatevent);

        if (oneconfig$sendchatevent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public String modifyMessage(String message) {
        return oneconfig$sendchatevent.message;
    }
}
//#endif