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

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ReceivePacketEvent;
import cc.polyfrost.oneconfig.internal.utils.Deprecator;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.modapi.HypixelModAPI;
import cc.polyfrost.oneconfig.libs.modapi.handler.ClientboundPacketHandler;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import cc.polyfrost.oneconfig.platform.Platform;

import java.util.Locale;

/**
 * Various utilities for Hypixel.
 */
public class HypixelUtils {
    public static final HypixelUtils INSTANCE = new HypixelUtils();

    public void initialize() {
        LocrawUtil.INSTANCE.initialize();
        PartyUtil.INSTANCE.initialize();
        HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
            @Override
            public void onLocationEvent(ClientboundLocationPacket packet) {
                LocrawUtil.INSTANCE.handleLocationPacket(packet);
            }

            @Override
            public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                PartyUtil.INSTANCE.handlePartyPacket(packet);
            }
        });
        EventManager.INSTANCE.register(this);
    }

    @Subscribe
    private void onPacket(ReceivePacketEvent event) {
        HypixelPacketUtil.getInstance().handle(event);
    }

    /**
     * Checks whether the player is on Hypixel.
     *
     * @return Whether the player is on Hypixel.
     * @see <a href="https://canary.discord.com/channels/864592657572560958/945075920664928276/978649312013725747">this discord message from discord.gg/essential</a>
     */
    public boolean isHypixel() {
        if (!Platform.getServerPlatform().inMultiplayer()) return false;

        String serverBrand = Platform.getServerPlatform().getServerBrand();

        if (serverBrand == null) return false;

        return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
    }

    /**
     * @deprecated Hypixel has drastically changed their API, and `/api new` no longer works. <a href="https://hypixel.net/threads/hypixel-developer-dashboard-public-api-changes-june-2023.5364455/">See Hypixel's API changes here.</a>
     */
    @Deprecated
    public boolean isValidKey(String key) {
        Deprecator.markDeprecated();
        return false;
    }

    /**
     * Returns whether the player is in game.
     *
     * @return Whether the player is in game.
     * @deprecated Moved to {@link LocrawUtil}
     */
    @Deprecated
    public boolean isInGame() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.isInGame();
    }

    /**
     * Returns the current {@link LocrawInfo}.
     *
     * @return The current {@link LocrawInfo}.
     * @deprecated Moved to {@link LocrawUtil}
     * @see LocrawInfo
     */
    @Deprecated
    public LocrawInfo getLocrawInfo() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.getLocrawInfo();
    }

    /**
     * Returns the previous {@link LocrawInfo}.
     *
     * @return The previous {@link LocrawInfo}.
     * @deprecated Moved to {@link LocrawUtil}
     * @see LocrawInfo
     */
    @Deprecated
    public LocrawInfo getPreviousLocraw() {
        Deprecator.markDeprecated();
        return LocrawUtil.INSTANCE.getLastLocrawInfo();
    }
}
