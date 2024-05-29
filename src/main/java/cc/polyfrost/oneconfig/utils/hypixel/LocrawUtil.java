/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * Co-author: lyndseyy (Lyndsey Winter) <https://github.com/lyndseyy>
 * Co-author: asbyth <cyronize@gmail.com> (deleted GitHub account)
 * Co-author: Moire9 (Moiré) <https://github.com/Moire9> (non-copyrightable contribution)
 * Co-author: Sk1er (Mitchell Katz) <https://github.com/Sk1er>
 * Co-author: Cubxity <https://github.com/Cubxity> (non-copyrightable contribution)
 * Co-author: UserTeemu <https://github.com/UserTeemu> (non-copyrightable contribution)
 * Co-author: PyICoder (Befell) <https://github.com/PyICoder> (non-copyrightable contribution)
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
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import cc.polyfrost.oneconfig.events.event.ChatSendEvent;
import cc.polyfrost.oneconfig.events.event.LocrawEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.events.event.WorldLoadEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.modapi.HypixelModAPI;
import cc.polyfrost.oneconfig.libs.modapi.handler.ClientboundPacketHandler;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import cc.polyfrost.oneconfig.libs.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.TickDelay;
import com.google.gson.Gson;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * An easy way to interact with the Hypixel Locraw API.
 * </p>
 * Modified from Hytilities by Sk1erLLC
 * <a href="https://github.com/Sk1erLLC/Hytilities/blob/master/LICENSE">https://github.com/Sk1erLLC/Hytilities/blob/master/LICENSE</a>
 */
public class LocrawUtil {
    public static final LocrawUtil INSTANCE = new LocrawUtil();

    private final Gson GSON = new Gson();
    private LocrawInfo locrawInfo;
    private LocrawInfo lastLocrawInfo;
    private boolean listening;
    private int tick;
    private boolean playerSentCommand = false;
    private boolean inGame = false;

    void initialize() {
        EventManager.INSTANCE.register(this);
    }

    private void sendLocraw(boolean delay) {
        new TickDelay(() -> {
            this.listening = true;
            //#if FORGE==1 && MC<=10809
//            HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
            //#else
            UChat.say("/locraw");
            //#endif
        }, (delay ? 20 : 0));
    }

    public void handleLocationPacket(ClientboundLocationPacket packet) {
        locrawInfo = new LocrawInfo(packet.getServerName(), packet.getMode().orElse("null"), packet.getMap().orElse("null"), packet.getServerType().map(Object::toString).orElse("null"));
        inGame = !locrawInfo.getGameMode().equals("lobby");
        EventManager.INSTANCE.post(new LocrawEvent(locrawInfo));
        listening = false;
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.stage != Stage.START || !Platform.getServerPlatform().doesPlayerExist() || !HypixelUtils.INSTANCE.isHypixel()) {
            return;
        }

        this.tick++;
        if (this.tick == 40 || this.tick % 520 == 0) {
            sendLocraw(false);
        }
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        if (locrawInfo != null) {
            lastLocrawInfo = locrawInfo;
        }
        locrawInfo = null;
        tick = 0;
    }

    @Subscribe
    private void onMessageSent(ChatSendEvent event) {
        if (event.message.startsWith("/locraw") && !this.listening) {
            playerSentCommand = true;
        }
    }

    @Subscribe
    private void onMessageReceived(ChatReceiveEvent event) {
        try {
            // Had some false positives while testing, so this is here just to be safe.
            final String msg = event.getFullyUnformattedMessage();
            if (msg.startsWith("You are sending too many commands! Please try again in a few seconds.")) {
                sendLocraw(true);
                return;
            }
            if (msg.startsWith("{")) {
                // Parse the json, and make sure that it's not null.
                this.locrawInfo = GSON.fromJson(msg, LocrawInfo.class);
                if (locrawInfo != null) {
                    // Gson does not want to parse the GameType, as some stuff is different so this
                    // is just a way around that to make it properly work :)
                    this.locrawInfo.setGameType(LocrawInfo.GameType.getFromLocraw(locrawInfo.getRawGameType()));
                    // If your gamemode does not return "lobby", boolean inGame is true, otherwise false.
                    inGame = !locrawInfo.getGameMode().equals("lobby");

                    // Stop listening for locraw and cancel the message.
                    if (!this.playerSentCommand) {
                        event.isCancelled = true;
                    }
                    EventManager.INSTANCE.post(new LocrawEvent(locrawInfo));
                    this.playerSentCommand = false;
                    this.listening = false;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns whether the player is in game.
     *
     * @return Whether the player is in game.
     */
    public boolean isInGame() {
        return inGame;
    }

    /**
     * Returns the current {@link LocrawInfo}.
     *
     * @return The current {@link LocrawInfo}.
     * @see LocrawInfo
     */
    @Nullable
    public LocrawInfo getLocrawInfo() {
        return locrawInfo;
    }

    /**
     * Returns the previous {@link LocrawInfo}.
     *
     * @return The previous {@link LocrawInfo}.
     * @see LocrawInfo
     */
    @Nullable
    public LocrawInfo getLastLocrawInfo() {
        return lastLocrawInfo;
    }
}
