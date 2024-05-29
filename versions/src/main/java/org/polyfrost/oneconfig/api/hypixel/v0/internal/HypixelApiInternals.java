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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.universal.UMinecraft;

@ApiStatus.Internal
public final class HypixelApiInternals {
    public static final HypixelApiInternals INSTANCE = new HypixelApiInternals();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");

    private HypixelApiInternals() {
        registerHypixelApi();
    }

    public static void init() {
        //<clinit>
    }


    private void registerHypixelApi() {}
        /*
        //#if FORGE
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            net.minecraft.client.network.NetHandlerPlayClient net = UMinecraft.getMinecraft().getNetHandler();
            if (net == null) return false;
            net.minecraft.network.PacketBuffer buf = new net.minecraft.network.PacketBuffer(Unpooled.buffer());
            packet.write(new PacketSerializer(buf));
            net.addToSendQueue(new net.minecraft.network.play.client.C17PacketCustomPayload(
                    //#if MC<=11202
                    packet.getIdentifier()
                    //#else
                    //$$ new net.minecraft.util.ResourceLocation(packet.getIdentifier())
                    //#endif
                    , buf));
            return true;
        });
        //#else
        //$$
        //#endif
    }

    //#if FORGE
    //#if MC<=11202
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onServerConnect(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
        //#else
        //$$ @net.minecraftforge.eventbus.api.SubscribeEvent
        //$$ public void onServerConnect(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedInEvent event) {
        //#endif
        net.minecraft.network.NetworkManager manager = event.
                //#if MC==10809
                manager;
        //#elseif MC==11202
        //$$ getManager();
        //#else
        //$$ getNetworkManager();
        //#endif
        if (manager == null) return;
        manager.channel().pipeline().addBefore("packet_handler", "hypixel_mod_api_packet_handler", HypixelPacketHandler.INSTANCE);
    }

    @ChannelHandler.Sharable
    private static class HypixelPacketHandler extends SimpleChannelInboundHandler<Packet<?>> {
        private static final HypixelPacketHandler INSTANCE = new HypixelPacketHandler();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg) {
            ctx.fireChannelRead(msg);

            if (!(msg instanceof net.minecraft.network.play.server.S3FPacketCustomPayload)) {
                return;
            }

            net.minecraft.network.play.server.S3FPacketCustomPayload packet = (net.minecraft.network.play.server.S3FPacketCustomPayload) msg;
            // reason: needed for 1.16+
            //noinspection StringOperationCanBeSimplified
            String identifier = packet.getChannelName().toString();
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(identifier)) {
                return;
            }

            try {
                HypixelModAPI.getInstance().handle(identifier, new PacketSerializer(packet.getBufferData()));
            } catch (Exception e) {
                LOGGER.warn("Failed to handle packet {}", identifier, e);
            }
        }
    }
    //#endif
    */
}
