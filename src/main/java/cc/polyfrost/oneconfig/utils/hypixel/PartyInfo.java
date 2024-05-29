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

import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyInfo {
    private final Map<UUID, ClientboundPartyInfoPacket.PartyRole> memberMap;
    private final UUID leader;
    private final List<UUID> moderators;
    private final List<UUID> members;

    public PartyInfo(ClientboundPartyInfoPacket packet) {
        this.memberMap = packet.getMemberMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getRole()));
        this.members = new ArrayList<>(packet.getMembers());
        this.leader = packet.getLeader().orElse(null);
        this.moderators = new ArrayList<>(members).stream().filter(member -> getMemberRole(member) == ClientboundPartyInfoPacket.PartyRole.MOD).collect(Collectors.toList());
    }

    public ClientboundPartyInfoPacket.PartyRole getMemberRole(UUID uuid) {
        if (!members.contains(uuid)) return null;
        return memberMap.get(uuid);
    }

    @Override
    public String toString() {
        return "PartyInfo{" + "leader='" + leader + '\'' + ", moderators='" + moderators + '\'' + ", members='" + memberMap + '\'' + '}';
    }

    public UUID getLeader() {
        return leader;
    }

    public List<UUID> getMembers() {
        return members;
    }

    public List<UUID> getModerators() {
        return moderators;
    }
}
