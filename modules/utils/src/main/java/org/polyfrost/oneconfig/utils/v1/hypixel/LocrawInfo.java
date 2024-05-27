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

package org.polyfrost.oneconfig.utils.v1.hypixel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the location of the player in Hypixel.
 *
 * @see HypixelUtils
 */
@SuppressWarnings({"ConstantConditions", "ConstantValue"} /*, reason = "this class is serialized by GSON so values are changed by that."*/)
public class LocrawInfo implements Serializable {
    /**
     * Represents the previous location info that was used.
     */
    @Nullable
    public transient LocrawInfo previous;

    @Nullable
    private final String server = null;
    @NotNull
    private final String mode = "lobby";
    @Nullable
    private final String map = null;
    @Nullable
    private final String gametype = null;
    @NotNull
    private transient final GameType gameType;

    // called by gson
    private LocrawInfo() {
        gameType = GameType.fromString(gametype);
    }

    /**
     * @return The serverID of the server you are currently on, ex: mini121
     */
    @Nullable
    public String getServerId() {
        return server;
    }

    /**
     * @return The GameType of the server as a String.
     */
    @Nullable
    public String getRawGameType() {
        return gametype;
    }

    /**
     * @return The GameMode of the server, ex: solo_insane
     */
    @NotNull
    public String getGameMode() {
        return mode;
    }

    /**
     * @return The GameType of the server as an Enum.
     */
    public @NotNull GameType getGameType() {
        return gameType;
    }

    /**
     * @return The map of the server, ex: Shire.
     */
    @Nullable
    public String getMapName() {
        return map;
    }

    public boolean isInGame() {
        return !"lobby".equals(mode);
    }

    @Override
    public String toString() {
        return "LocrawInfo{" + "serverId='" + server + '\'' + ", gameMode='" + mode + '\'' + ", mapName='" + map + '\'' + ", rawGameType='" + gametype + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocrawInfo that = (LocrawInfo) o;
        return Objects.equals(server, that.server) && Objects.equals(mode, that.mode) && Objects.equals(map, that.map) && Objects.equals(gametype, that.gametype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(server, mode, map, gametype);
    }

    public enum GameType {
        UNKNOWN(""), LIMBO("LIMBO"), BEDWARS("BEDWARS"), SKYWARS("SKYWARS"), PROTOTYPE("PROTOTYPE"), SKYBLOCK("SKYBLOCK"), MAIN("MAIN"), MURDER_MYSTERY("MURDER_MYSTERY"), HOUSING("HOUSING"), ARCADE_GAMES("ARCADE"), BUILD_BATTLE("BUILD_BATTLE"), DUELS("DUELS"), PIT("PIT"), UHC_CHAMPIONS("UHC"), SPEED_UHC("SPEED_UHC"), TNT_GAMES("TNTGAMES"), CLASSIC_GAMES("LEGACY"), COPS_AND_CRIMS("MCGO"), BLITZ_SG("SURVIVAL_GAMES"), MEGA_WALLS("WALLS3"), SMASH_HEROES("SUPER_SMASH"), WARLORDS("BATTLEGROUND"),
        /**
         * @see #getGameMode()
         * @deprecated Moved to a PROTOTYPE gamemode
         */
        @Deprecated
        DROPPER(""),
        WOOL_WARS("WOOL_GAMES"), VAMPIREZ("VAMPIREZ"), PAINTBALL("PAINTBALL"), QUAKE("QUAKECRAFT"), WALLS("WALLS"), TURBO_KART_RACERS("GINGERBREAD"), ARENA("ARENA"), REPLAY("REPLAY");

        private final String serverName;

        GameType(String serverName) {
            this.serverName = serverName;
        }

        public static GameType fromString(String gameType) {
            if (gameType == null) return UNKNOWN;
            for (GameType value : values()) {
                if (value.serverName.equals(gameType)) {
                    return value;
                }
            }

            return UNKNOWN;
        }

        public String getServerName() {
            return serverName;
        }
    }
}
