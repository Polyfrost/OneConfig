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
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Unmodifiable;
import org.polyfrost.oneconfig.api.hypixel.v0.internal.HypixelApiInternals;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

import java.util.*;

/**
 * Hypixel API wrapper for OneConfig.
 * <br><br>
 * When this class is first referenced, it will set up the Hypixel API handlers for you. After that, all the methods for sending and receiving packets
 * are available directly from their classes, such as {@link HypixelModAPI#registerHandler(Class, ClientboundPacketHandler)}.
 * <br><br>
 * This class is a simple wrapper around this functionality, providing a simple way to access the Hypixel API.
 *
 * @implNote the actual registration of the packet handlers is done in HypixelApiInternals. Note that this is lazily initialized,
 * <b>and so this class, or HypixelApiInternals, needs to be referenced in order for the packet handlers to be registered.</b>
 */
@SuppressWarnings("unused")
public final class HypixelUtils {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelUtils");
    private static final HypixelUtils INSTANCE = new HypixelUtils();
    private static final HypixelApiInternals internals = ServiceLoader.load(HypixelApiInternals.class, HypixelApiInternals.class.getClassLoader()).iterator().next();
    private PlayerInfo info;
    private PartyInfo partyInfo;
    private Location location;

    private HypixelUtils() {
    }

    @SuppressWarnings("deprecation")
    public static boolean isHypixel() {
        String brand = Platform.player().getServerBrand();
        if (brand == null) return false;
        return brand.toLowerCase().contains("hypixel");
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

    protected static abstract class InfoBase<T extends ClientboundHypixelPacket> {
        protected T packet;

        protected InfoBase() {
            LOGGER.info("Registering {} packet handler", getPacketClass().getSimpleName());
            HypixelModAPI.getInstance().registerHandler(getPacketClass(), this::onPacket);
            update();
        }

        @ApiStatus.Internal
        public abstract void update();

        @Override
        public final String toString() {
            return packet.toString();
        }

        public T getPacket() {
            if (packet == null) update();
            return packet;
        }

        @MustBeInvokedByOverriders
        protected void onPacket(T packet) {
            this.packet = packet;
        }

        protected abstract Class<T> getPacketClass();
    }


    public static final class PartyInfo extends InfoBase<ClientboundPartyInfoPacket> {

        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public boolean isInParty() {
            return getPacket() != null && packet.isInParty();
        }

        public int getPartySize() {
            return getPacket() == null ? 0 : packet.getMemberMap().size();
        }

        @Unmodifiable
        public Map<UUID, ClientboundPartyInfoPacket.PartyMember> getMembers() {
            return getPacket() == null ? Collections.emptyMap() : packet.getMemberMap();
        }

        @Override
        protected Class<ClientboundPartyInfoPacket> getPacketClass() {
            return ClientboundPartyInfoPacket.class;
        }
    }

    public static final class PlayerInfo extends InfoBase<ClientboundPlayerInfoPacket> {
        @Override
        public void update() {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket());
        }

        public Optional<PlayerRank> getRank() {
            return getPacket() == null ? Optional.empty() : Optional.of(packet.getPlayerRank());
        }

        public Optional<PackageRank> getPackageRank() {
            return getPacket() == null ? Optional.empty() : Optional.of(packet.getPackageRank());
        }

        public Optional<MonthlyPackageRank> getMonthlyPackageRank() {
            return getPacket() == null ? Optional.empty() : Optional.of(packet.getMonthlyPackageRank());
        }

        public Optional<String> getPrefix() {
            return getPacket() == null ? Optional.empty() : packet.getPrefix();
        }

        @Override
        protected Class<ClientboundPlayerInfoPacket> getPacketClass() {
            return ClientboundPlayerInfoPacket.class;
        }
    }

    public static final class Location extends InfoBase<ClientboundLocationPacket> {
        private ClientboundLocationPacket last;

        private Location() {
            super();
            HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
        }

        @Override
        public void update() {
            // no-op as event based
        }

        @Override
        protected void onPacket(ClientboundLocationPacket packet) {
            last = this.packet;
            super.onPacket(packet);
            internals.postLocationEvent();
        }

        public Optional<String> getLastServerName() {
            return last == null ? Optional.empty() : Optional.of(last.getServerName());
        }

        public Optional<String> getServerName() {
            return getPacket() == null ? Optional.empty() : Optional.of(packet.getServerName());
        }

        public Optional<String> getLastLobbyName() {
            return last == null ? Optional.empty() : last.getLobbyName();
        }

        public Optional<String> getLobbyName() {
            return getPacket() == null ? Optional.empty() : packet.getLobbyName();
        }

        public Optional<String> getLastMapName() {
            return last == null ? Optional.empty() : last.getMap();
        }

        public Optional<String> getMapName() {
            return getPacket() == null ? Optional.empty() : packet.getMap();
        }

        public Optional<String> getLastMode() {
            return last == null ? Optional.empty() : last.getMode();
        }

        public Optional<String> getMode() {
            return getPacket() == null ? Optional.empty() : packet.getMode();
        }

        public Optional<ServerType> getLastServerType() {
            return last == null ? Optional.empty() : last.getServerType();
        }

        public Optional<ServerType> getServerType() {
            return getPacket() == null ? Optional.empty() : packet.getServerType();
        }

        public boolean wasInLobby() {
            return getLastServerType().orElse(null) instanceof LobbyType;
        }

        public boolean inLobby() {
            return getServerType().orElse(null) instanceof LobbyType;
        }

        public boolean wasInGame() {
            return !wasInLobby();
        }

        public boolean inGame() {
            return !inLobby();
        }

        public Optional<GameType> getLastGameType() {
            return last == null ? Optional.empty() : gameType(last.getServerType());
        }

        public Optional<GameType> getGameType() {
            return getPacket() == null ? Optional.empty() : gameType(packet.getServerType());
        }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<GameType> gameType(Optional<ServerType> type) {
            if (!type.isPresent()) return Optional.empty();
            ServerType t = type.get();
            return t instanceof GameType ? Optional.of((GameType) t) : Optional.empty();
        }

        @Override
        protected Class<ClientboundLocationPacket> getPacketClass() {
            return ClientboundLocationPacket.class;
        }
    }
}
