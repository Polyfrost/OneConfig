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

package org.polyfrost.oneconfig.api.hypixel.v0.internal;

import io.netty.buffer.Unpooled;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.PlatformDeclaration;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HypixelLocationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ReceivePacketEvent;

/**
 * Heavily adapted from Hypixel/ForgeModAPI under the MIT licence.
 * <a href="https://github.com/HypixelDev/ForgeModAPI/blob/master/src/main/java/net/hypixel/modapi/forge/ForgeModAPI.java">See here</a>
 */
@ApiStatus.Internal
@PlatformDeclaration
public final class HypixelApiInternals {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");

    private HypixelApiInternals() {}

    static {
        registerHypixelApi();
    }

    public static void init() {
        // <clinit>
    }


    private static void registerHypixelApi() {
        LOGGER.info("Registering Hypixel API packet handlers");
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            NetHandlerPlayClient net = Minecraft.getMinecraft().getNetHandler();
            if (net == null) {
                LOGGER.warn("dropping packet {} because no net handler is available, retrying in 1s", packet);
                EventDelay.tick(20, () -> HypixelModAPI.getInstance().sendPacket(packet));
                return false;
            }
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            packet.write(new PacketSerializer(buf));
            net.addToSendQueue(new C17PacketCustomPayload(
                            //#if MC>12000
                            //#if FORGE
                            //$$ new net.minecraft.network.protocol.common.custom.DiscardedPayload(
                            //#else
                            //$$ new Payload(
                            //#endif
                            //#endif
                            //#if MC<=11202
                            packet.getIdentifier(),
                            //#else
                            //$$ new net.minecraft.util.ResourceLocation(packet.getIdentifier()),
                            //#endif
                            buf
                            //#if MC>12000
                            //$$ )
                            //#endif
                    )
            );
            return true;
        });
        EventManager.register(ReceivePacketEvent.class, (ev) -> {
            if (!(ev.packet instanceof S3FPacketCustomPayload)) {
                return;
            }

            S3FPacketCustomPayload packet = (S3FPacketCustomPayload) ev.packet;
            //#if MC>12000
            //$$ String identifier = packet.payload().id().toString();
            //#else
            //noinspection StringOperationCanBeSimplified
            String identifier = packet.getChannelName().toString();
            //#endif
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(identifier)) {
                return;
            }

            try {
                PacketSerializer s = new PacketSerializer(
                        //#if MC>12000 && FABRIC
                        //$$ ((Payload) packet.payload()).data()
                        //#else
                        packet.getBufferData()
                        //#endif
                );
                HypixelModAPI.getInstance().handle(identifier, s);
            } catch (Exception e) {
                LOGGER.warn("Failed to handle packet {}", identifier, e);
            }
        });
    }

    @ApiStatus.Internal
    public static void postLocationEvent() {
        EventManager.INSTANCE.post(HypixelLocationEvent.INSTANCE);
    }

    //#if MC>12000 && FABRIC
    //$$ public static final class Payload implements net.minecraft.network.packet.CustomPayload {
    //$$     private final net.minecraft.util.Identifier id;
    //$$     private final io.netty.buffer.ByteBuf data;
    //$$
    //$$     public Payload(net.minecraft.util.Identifier id, io.netty.buffer.ByteBuf data) {
    //$$         this.id = id;
    //$$         this.data = data.copy();
    //$$         data.skipBytes(data.readableBytes());
    //$$     }
    //$$
    //$$     public void write(net.minecraft.network.PacketByteBuf arg) {
    //$$         if (this.data != null) {
    //$$             arg.writeBytes(this.data.slice());
    //$$         }
    //$$     }
    //$$
    //$$     public net.minecraft.util.Identifier id() {
    //$$         return this.id;
    //$$     }
    //$$
    //$$     public io.netty.buffer.ByteBuf data() {
    //$$         return this.data;
    //$$     }
    //$$
    //$$ }
    //#endif
}
