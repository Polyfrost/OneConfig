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
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.modapi.HypixelModAPI;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import cc.polyfrost.oneconfig.platform.Platform;

public class PartyUtil {
    public static final PartyUtil INSTANCE = new PartyUtil();
    private PartyInfo partyInfo;
    private int tick;

    public void initialize() {
        EventManager.INSTANCE.register(this);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.stage != Stage.START || !Platform.getServerPlatform().doesPlayerExist() || !HypixelUtils.INSTANCE.isHypixel()) {
            return;
        }

        this.tick++;
        if (this.tick >= 100) {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
            tick = 0;
        }
    }

    public void handlePartyPacket(ClientboundPartyInfoPacket packet) {
        partyInfo = new PartyInfo(packet);
    }

    public boolean isInParty() {
        return partyInfo.isInParty();
    }

    public PartyInfo getPartyInfo() {
        return partyInfo;
    }
}
