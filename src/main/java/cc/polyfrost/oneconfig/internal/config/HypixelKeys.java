/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.annotations.HypixelKey;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ChatReceiveEvent;
import cc.polyfrost.oneconfig.events.event.Stage;
import cc.polyfrost.oneconfig.events.event.TickEvent;
import cc.polyfrost.oneconfig.gui.GuiNotifications;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.pages.ModConfigPage;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.asset.Icon;
import cc.polyfrost.oneconfig.utils.Multithreading;
import cc.polyfrost.oneconfig.utils.Notifications;
import cc.polyfrost.oneconfig.utils.TickDelay;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HypixelKeys {
    public static final HypixelKeys INSTANCE = new HypixelKeys();
    private final List<BasicOption> options = new ArrayList<>();
    private static final int apiKeyMessageLength = "Your new API key is ".length();
    private boolean hasSynced = false;

    private HypixelKeys() {
        EventManager.INSTANCE.register(this);
    }

    public void addOption(BasicOption option) {
        options.add(option);
    }

    @Subscribe
    private void onChatReceived(ChatReceiveEvent event) {
        if (Preferences.autoSetHypixelKey) {
            String message = event.getFullyUnformattedMessage();
            if (message.startsWith("Your new API key is ")) {
                String tempApiKey = message.substring(apiKeyMessageLength);
                Multithreading.runAsync(() -> { //run this async as getting from the API normally would freeze minecraft
                    if (!HypixelUtils.INSTANCE.isValidKey(tempApiKey)
                    ) {
                        sendNotification("The API Key was invalid! Please try running the command again.");
                    } else {
                        boolean success = true;
                        for (BasicOption option : options) {
                            try {
                                option.getField().set(option.getParent(), tempApiKey);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                                sendNotification("Unable to set API Key of " + option.getField().getName() + " in " + option.getParent().getClass().getName() + "!");
                                success = false;
                            }
                        }
                        if (success) {
                            sendNotification("Successfully set API Key for " + options.size() + " option" + (options.size() > 1 ? "s" : "") + "!");
                        }
                    }
                });
            }
        }
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.stage != Stage.START || !Platform.getServerPlatform().doesPlayerExist() || !HypixelUtils.INSTANCE.isHypixel()) return;
        if (!hasSynced && Preferences.syncHypixelKeys) {
            syncKeys(false);
            hasSynced = true;
        }
    }

    public void syncKeys(boolean inGui) {
        options.sort(Comparator.comparingInt(o -> o.getField().getAnnotation(HypixelKey.class).priority()));
        String firstValidKey = null;
        for (BasicOption option : options) {
            try {
                String apiKey = (String) option.get();
                if (HypixelUtils.INSTANCE.isValidKey(apiKey)) {
                    firstValidKey = apiKey;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        if (firstValidKey != null) {
            setKeys(firstValidKey);
        } else {
            if (inGui) {
                UChat.say("/api new");
            } else {
                sendNotification("There are mods that require a Hypixel API Key, but none of the keys are valid! Click here to open the preferences menu and sync your API key.", () -> {
                    GuiUtils.displayScreen(OneConfigGui.create());
                    new TickDelay(() -> OneConfigGui.INSTANCE.openPage(new ModConfigPage(Preferences.getInstance().mod.defaultPage, true)), 2);
                });
            }
        }
    }

    public void setAllKeys(String hypixelKey) {
        if (hypixelKey == null) return;
        setKeys(hypixelKey);
    }

    private void setKeys(String hypixelKey) {
        boolean success = true;
        for (BasicOption option : options) {
            try {
                option.getField().set(option.getParent(), hypixelKey);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                sendNotification("Unable to set API Key of " + option.getField().getName() + " in " + option.getParent().getClass().getName() + "! Please contact us at https://polyfrost.cc/discord!");
                success = false;
            }
        }
        if (success) {
            sendNotification("Successfully synced API Key for " + options.size() + " option" + (options.size() > 1 ? "s" : "") + "!");
        }
    }

    private final Icon icon = new Icon("/assets/oneconfig/icons/Key.svg");

    private void sendNotification(String message) {
        if (Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui) {
            GuiNotifications.INSTANCE.sendNotification(message, icon.getSVG());
        } else {
            Notifications.INSTANCE.send("OneConfig Hypixel API Key", message, icon);
        }
    }

    private void sendNotification(String message, Runnable runnable) {
        Notifications.INSTANCE.send("OneConfig Hypixel API Key", message, icon, runnable);
    }
}
