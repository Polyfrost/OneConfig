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

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.HypixelLocationEvent;

@ApiStatus.Internal
public final class HypixelApiInternalsImpl implements HypixelApiInternals {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");

    private volatile boolean onHypixel = false;

    public HypixelApiInternalsImpl() {
        LOGGER.info("Registering Hypixel API hello/disconnect handlers");
        HypixelModAPI.getInstance().createHandler(ClientboundHelloPacket.class, p -> onHypixel = true);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onHypixel = false);
    }

    @Override
    public boolean isOnHypixel() {
        return onHypixel;
    }

    @Override
    @ApiStatus.Internal
    public void postLocationEvent() {
        EventManager.INSTANCE.post(HypixelLocationEvent.INSTANCE);
    }
}
