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

package org.polyfrost.oneconfig.internal.mixin.hypixel;

import net.hypixel.modapi.HypixelModAPI;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import org.polyfrost.oneconfig.api.hypixel.v0.internal.HypixelApiInternalsImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CustomPayloadS2CPacket.class)
public class CustomPayloadS2CPacketMixin {
    @Inject(method = "readPayload", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;readUnknownPayload(Lnet/minecraft/util/Identifier;Lnet/minecraft/network/PacketByteBuf;)Lnet/minecraft/network/packet/UnknownCustomPayload;"), cancellable = true)
    private static void oneconfig$captureHypixelPacket(Identifier id, PacketByteBuf buf, CallbackInfoReturnable<CustomPayload> cir) {
        if (HypixelModAPI.getInstance().getRegistry().isRegistered(id.toString())) {
            cir.setReturnValue(new HypixelApiInternalsImpl.Payload(id, buf));
        }
    }
}
