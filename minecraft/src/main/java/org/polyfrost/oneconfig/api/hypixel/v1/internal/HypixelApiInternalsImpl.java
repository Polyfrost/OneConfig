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

package org.polyfrost.oneconfig.api.hypixel.v1.internal;

import dev.deftu.omnicore.api.loader.OmniLoader;
import io.netty.buffer.Unpooled;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.Minecraft;
//#if MC >= 1.16.5
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
//#else
//$ import net.minecraft.client.network.NetHandlerPlayClient;
//$ import net.minecraft.network.PacketBuffer;
//$ import net.minecraft.network.play.client.C17PacketCustomPayload;
//$ import net.minecraft.network.play.server.S3FPacketCustomPayload;
//#endif
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HypixelLocationEvent;
import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent;

/**
 * Heavily adapted from Hypixel/ForgeModAPI under the MIT licence.
 * <a href="https://github.com/HypixelDev/ForgeModAPI/blob/master/src/main/java/net/hypixel/modapi/forge/ForgeModAPI.java">See here</a>
 */
@ApiStatus.Internal
public final class HypixelApiInternalsImpl implements HypixelApiInternals {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");

    public HypixelApiInternalsImpl() {
        registerHypixelApi();
    }


    private void registerHypixelApi() {
        LOGGER.info("Registering Hypixel API packet handlers");
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            //#if MC >= 1.16.5
            ClientPacketListener netHandler = Minecraft.getInstance().getConnection();
            //#else
            //$ NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
            //#endif
            if (netHandler == null) {
                if (OmniLoader.isDevelopment()) LOGGER.warn("dropping packet {} because no net handler is available, retrying in 1s", packet);
                EventDelay.tick(20, () -> HypixelModAPI.getInstance().sendPacket(packet));
                return false;
            }
            //#if MC >= 1.16.5
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            //#else
            //$ PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            //#endif
            packet.write(new PacketSerializer(buf));
            //#if MC >= 1.16.5
            netHandler.send(new ServerboundCustomPayloadPacket(
                    //#if MC >= 1.20.4
                    //#if FORGE
                    //$$ new net.minecraft.network.protocol.common.custom.DiscardedPayload(
                    //#else
                    new Payload(
                    //#endif
                    //#endif
                    //#if MC >= 1.21.1
                    net.minecraft.resources.ResourceLocation.parse(packet.getIdentifier()),
                    //#elseif MC >= 1.20.4 || MC == 1.16.5
                    //$$ new net.minecraft.resources.ResourceLocation(packet.getIdentifier()),
                    //#endif
                    //#if MC >= 1.20.4
                    buf
                    )
                    //#endif
                    )
            );
            //#else
            //$ netHandler.addToSendQueue(new C17PacketCustomPayload(
            //$         //#if MC <= 1.12.2
            //$         packet.getIdentifier(),
            //$         //#endif
            //$         //#if MC < 1.20.4
            //$         buf
            //$         //#endif
            //$         )
            //$ );
            //#endif
            return true;
        });
        EventManager.register(PacketEvent.Receive.class, (ev) -> {
            //#if MC >= 1.16.5
            if (!(ev.getPacket() instanceof ClientboundCustomPayloadPacket)) {
                return;
            }

            ClientboundCustomPayloadPacket packet = ev.getPacket();
            //#if MC >= 1.21.1
            String identifier = packet.payload().type().id().toString();
            //#elseif MC >= 1.20.6
            //$$ String identifier = packet.comp_1646().getId().comp_2242().toString();
            //#elseif MC >= 1.20.4
            //$$ String identifier = packet.payload().id().toString();
            //#else
            //$$ String identifier = packet.getChannelName().toString();
            //#endif
            //#else
            //$ if (!(ev.getPacket() instanceof S3FPacketCustomPayload)) {
            //$     return;
            //$ }
            //$
            //$ S3FPacketCustomPayload packet = ev.getPacket();
            //$ //noinspection StringOperationCanBeSimplified
            //$ String identifier = packet.getChannelName().toString();
            //#endif
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(identifier)) {
                return;
            }

            try {
                PacketSerializer s = new PacketSerializer(
                        //#if MC >= 1.20.4 && FABRIC || NEOFORGE
                        ((Payload) packet.payload()).data()
                        //#else
                        //$$ packet.getBufferData()
                        //#endif
                );
                HypixelModAPI.getInstance().handle(identifier, s);
            } catch (Exception e) {
                LOGGER.warn("Failed to handle packet {}", identifier, e);
            }
        });
    }

    @ApiStatus.Internal
    public void postLocationEvent() {
        EventManager.INSTANCE.post(HypixelLocationEvent.INSTANCE);
    }

    //#if MC >= 1.20.4 && FABRIC || NEOFORGE
    public static final class Payload implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
        private final net.minecraft.resources.ResourceLocation id;
        private final io.netty.buffer.ByteBuf data;

        public Payload(net.minecraft.resources.ResourceLocation id, io.netty.buffer.ByteBuf data) {
            this.id = id;
            this.data = data.copy();
            data.skipBytes(data.readableBytes());
        }

        public void write(net.minecraft.network.FriendlyByteBuf arg) {
            if (this.data != null) {
                arg.writeBytes(this.data.slice());
            }
        }

        //#if MC >= 1.20.6
        public Type<Payload> type() {
            return new Type<>(this.id);
        }
        //#else
        //$$ public net.minecraft.resources.ResourceLocation id() {
        //$     return this.id;
        //$$ }
        //#endif

        public io.netty.buffer.ByteBuf data() {
            return this.data;
        }

    }
    //#endif
}
