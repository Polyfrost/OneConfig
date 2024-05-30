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

package org.polyfrost.oneconfig.hypixel.v0;

import net.hypixel.data.rank.MonthlyPackageRank;
import net.hypixel.data.rank.PackageRank;
import net.hypixel.data.rank.PlayerRank;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.LobbyType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hypixel API wrapper for OneConfig.
 * <br>
 * When OneConfig is loaded, it handles loading of the Hypixel API for you. As long as OneConfig is initialized, all the methods for sending and receiving packets
 * are available directly from their classes.
 * <br>
 * This class is a simple wrapper around this functionality, providing a simple way to access the Hypixel API.
 */
@SuppressWarnings("unused")
public final class HypixelAPI {
    private static final HypixelAPI INSTANCE = new HypixelAPI();
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

    public static abstract class InfoBase {
        public abstract void update();
    }


    public static final class PartyInfo extends InfoBase {
        private ClientboundPartyInfoPacket p;

        private PartyInfo() {
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                    p = packet;
                }
            });
            update();
        }

        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public boolean isInParty() {
            return p.isInParty();
        }

        public int getPartySize() {
            return getMembers().size();
        }

        public Map<UUID, ClientboundPartyInfoPacket.PartyMember> getMembers() {
            return p.getMemberMap();
        }
    }

    public static final class PlayerInfo extends InfoBase {
        private ClientboundPlayerInfoPacket p;

        private PlayerInfo() {
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
                    p = packet;
                }
            });
            update();
        }

        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public PlayerRank getRank() {
            return p.getPlayerRank();
        }

        public PackageRank getPackageRank() {
            return p.getPackageRank();
        }

        public MonthlyPackageRank getMonthlyPackageRank() {
            return p.getMonthlyPackageRank();
        }

        public Optional<String> getPrefix() {
            return p.getPrefix();
        }
    }

    public static final class Location extends InfoBase {
        private ClientboundLocationPacket p;

        private Location() {
            HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
                @Override
                public void onLocationEvent(ClientboundLocationPacket packet) {
                    p = packet;
                }
            });
            update();
        }

        @Override
        public void update() {
        }

        public Optional<String> getLobbyName() {
            return p.getLobbyName();
        }

        public Optional<String> getMapName() {
            return p.getMap();
        }

        public Optional<String> getMode() {
            return p.getMode();
        }

        public Optional<ServerType> getServerType() {
            return p.getServerType();
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
