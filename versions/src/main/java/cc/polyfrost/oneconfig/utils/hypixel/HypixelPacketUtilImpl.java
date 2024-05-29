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

package cc.polyfrost.oneconfig.utils.hypixel;

import cc.polyfrost.oneconfig.events.event.ReceivePacketEvent;
import cc.polyfrost.oneconfig.libs.modapi.HypixelModAPI;
import cc.polyfrost.oneconfig.libs.modapi.packet.HypixelPacket;
import cc.polyfrost.oneconfig.libs.modapi.serializer.PacketSerializer;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
//#if FORGE==1
//#if MC>11202
//$$ import net.minecraft.network.play.client.CCustomPayloadPacket;
//$$ import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
//$$ import net.minecraft.util.ResourceLocation;
//#elseif MC>10809
//$$ import net.minecraft.network.play.client.CPacketCustomPayload;
//$$ import net.minecraft.network.play.server.SPacketCustomPayload;
//#else
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
//#endif
//#endif

public class HypixelPacketUtilImpl implements HypixelPacketUtil {
    @Override
    public boolean sendPacket(HypixelPacket packet) {
        //#if FORGE == 1
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        PacketSerializer serializer = new PacketSerializer(buf);
        packet.write(serializer);
        if (UMinecraft.getNetHandler() == null) return false;
        //#if MC>11202
        //$$ UMinecraft.getNetHandler().sendPacket(new CCustomPayloadPacket(new ResourceLocation(packet.getIdentifier()), buf));
        //#elseif MC>10809
        //$$ UMinecraft.getNetHandler().sendPacket(new CPacketCustomPayload(packet.getIdentifier(), buf));
        //#else
        UMinecraft.getNetHandler().addToSendQueue(new C17PacketCustomPayload(packet.getIdentifier(), buf));
        //#endif
        return true;
        //#else
        //$$ return false;
        //#endif
    }

    @Override
    public void handle(Object event) {
        Object object = ((ReceivePacketEvent) event).packet;
        //#if FORGE==1
        //#if MC>11202
        //$$ if (object instanceof SCustomPayloadPlayPacket) {
        //$$ SCustomPayloadPlayPacket packet = (SCustomPayloadPlayPacket) object;
        //$$ if (!HypixelModAPI.getInstance().getRegistry().isRegistered(packet.getChannelName().toString())) return;
        //$$ HypixelModAPI.getInstance().handle(packet.getChannelName().toString(), new PacketSerializer(packet.getBufferData()));
        //#else
        //#if MC>10809
        //$$ if (object instanceof SPacketCustomPayload) {
        //$$ SPacketCustomPayload packet = (SPacketCustomPayload) object;
        //#else
        if (object instanceof S3FPacketCustomPayload) {
            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) object;
            //#endif
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(packet.getChannelName())) return;

            HypixelModAPI.getInstance().handle(packet.getChannelName(), new PacketSerializer(packet.getBufferData()));
            //#endif
        }
        //#endif
    }

}
