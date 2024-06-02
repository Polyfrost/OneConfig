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

package org.polyfrost.oneconfig.api.hypixel.v0;

import net.hypixel.data.rank.MonthlyPackageRank;
import net.hypixel.data.rank.PackageRank;
import net.hypixel.data.rank.PlayerRank;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.LobbyType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.ClientboundHypixelPacket;
import net.hypixel.modapi.packet.impl.VersionedPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.hypixel.v0.internal.HypixelApiInternals;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

/**
 * Hypixel API wrapper for OneConfig.
 * <br><br>
 * When this class is first referenced, it will set up the Hypixel API handlers for you. After that, all the methods for sending and receiving packets
 * are available directly from their classes, such as {@link HypixelModAPI#registerHandler(ClientboundPacketHandler)}.
 * <br><br>
 * This class is a simple wrapper around this functionality, providing a simple way to access the Hypixel API.
 *
 * @implNote the actual registration of the packet handlers is done in HypixelApiInternals. Note that this is lazily initialized,
 * <b>and so this class, or HypixelApiInternals, needs to be referenced in order for the packet handlers to be registered.</b>
 */
@SuppressWarnings("unused")
public final class HypixelAPI {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");
    private static final HypixelAPI INSTANCE = new HypixelAPI();
    private static final HypixelApiInternals internals = ServiceLoader.load(HypixelApiInternals.class, HypixelApiInternals.class.getClassLoader()).iterator().next();
    private PlayerInfo info;
    private PartyInfo partyInfo;
    private Location location;

    private HypixelAPI() {
    }

    public static PlayerInfo getPlayerInfo() {
        return INSTANCE.info == null ? INSTANCE.info = new PlayerInfo() : INSTANCE.info;
    }

    public static PartyInfo getPartyInfo() {
        return INSTANCE.partyInfo == null ? INSTANCE.partyInfo = new PartyInfo() : INSTANCE.partyInfo;
    }

    public static Location getLocation() {
        return INSTANCE.location == null ? INSTANCE.location = new Location() : INSTANCE.location;
    }

    public static abstract class InfoBase<T extends VersionedPacket & ClientboundHypixelPacket> {
        protected T packet;
        @ApiStatus.Internal
        public abstract void update();

        @Override
        public final String toString() {
            return packet.toString();
        }
    }


    public static final class PartyInfo extends InfoBase<ClientboundPartyInfoPacket> {
        private PartyInfo() {
            LOGGER.info("Registering party info packet handler");
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                    PartyInfo.this.packet = packet;
                }
            });
            update();
        }

        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public boolean isInParty() {
            return packet.isInParty();
        }

        public int getPartySize() {
            return getMembers().size();
        }

        public Map<UUID, ClientboundPartyInfoPacket.PartyMember> getMembers() {
            return packet.getMemberMap();
        }
    }

    public static final class PlayerInfo extends InfoBase<ClientboundPlayerInfoPacket> {
        private PlayerInfo() {
            LOGGER.info("Registering player info handler");
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
                    PlayerInfo.this.packet = packet;
                }
            });
            update();
        }

        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public PlayerRank getRank() {
            return packet.getPlayerRank();
        }

        public PackageRank getPackageRank() {
            return packet.getPackageRank();
        }

        public MonthlyPackageRank getMonthlyPackageRank() {
            return packet.getMonthlyPackageRank();
        }

        public Optional<String> getPrefix() {
            return packet.getPrefix();
        }
    }

    public static final class Location extends InfoBase<ClientboundLocationPacket> {
        private Location() {
            LOGGER.info("Registering location packet handler");
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onLocationEvent(ClientboundLocationPacket packet) {
                    Location.this.packet = packet;
                    // cannot access the EventManager from here, so we do it like this instead.
                    internals.postLocationEvent();
                }
            });
            HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
        }

        @Override
        public void update() {
            // no-op as event based
        }

        public Optional<String> getLobbyName() {
            return packet.getLobbyName();
        }

        public Optional<String> getMapName() {
            return packet.getMap();
        }

        public Optional<String> getMode() {
            return packet.getMode();
        }

        public Optional<ServerType> getServerType() {
            return packet.getServerType();
        }

        public boolean isLobby() {
            Optional<ServerType> type = getServerType();
            return type.isPresent() && type.get() instanceof LobbyType;
        }

        public boolean isGame() {
            return !isLobby();
        }

        public Optional<GameType> getGameType() {
            Optional<ServerType> type = getServerType();
            if (!type.isPresent()) return Optional.empty();
            ServerType t = type.get();
            return t instanceof GameType ? Optional.of((GameType) t) : Optional.empty();
        }
    }
}
