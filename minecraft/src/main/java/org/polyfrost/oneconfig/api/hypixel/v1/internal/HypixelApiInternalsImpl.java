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

import dev.deftu.omnicore.api.OmniResourceLocation;
import dev.deftu.omnicore.api.client.network.OmniClientNetworking;
import dev.deftu.omnicore.api.loader.OmniLoader;
import dev.deftu.omnicore.api.network.codec.StreamCodec;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.PacketRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HypixelLocationEvent;

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
        PacketRegistry registry = HypixelModAPI.getInstance().getRegistry();
        for (String string : registry.getClientboundIdentifiers()) {
            ResourceLocation identifier = OmniResourceLocation.createOrNull(string);
            if (identifier == null) {
                LOGGER.warn("Skipping registration of malformed Hypixel API packet identifier: {}", string);
                continue;
            }

            StreamCodec<ClientboundHypixelPayload, PacketBuffer> codec = ClientboundHypixelPayload.createCodec(identifier);
            OmniClientNetworking.register(identifier, codec, (ctx, payload) -> {
                if (!payload.isSuccessful()) {
                    try {
                        HypixelModAPI.getInstance().handleError(string, payload.getErrorReason());
                    } catch (Exception e) {
                        LOGGER.warn("Failed to handle error for packet {}", string, e);
                    }

                    return;
                }

                try {
                    HypixelModAPI.getInstance().handle(payload.getPacket());
                } catch (Exception e) {
                    LOGGER.warn("Failed to handle packet {}", string, e);
                }
            });
        }

        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            NetHandlerPlayClient net = Minecraft.getMinecraft().getNetHandler();
            if (net == null) {
                if (OmniLoader.isDevelopment()) {
                    LOGGER.warn("dropping packet {} because no net handler is available, retrying in 1s", packet);
                }

                EventDelay.tick(20, () -> HypixelModAPI.getInstance().sendPacket(packet));
                return false;
            }

            OmniClientNetworking.send(new ServerboundHypixelPayload(packet));
            return true;
        });
    }

    @ApiStatus.Internal
    public void postLocationEvent() {
        EventManager.INSTANCE.post(HypixelLocationEvent.INSTANCE);
    }

}
