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

package cc.polyfrost.oneconfig.internal.config;

import cc.polyfrost.oneconfig.config.annotations.HypixelKey;
import cc.polyfrost.oneconfig.config.data.Mod;
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
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HypixelKeys {
    public static final HypixelKeys INSTANCE = new HypixelKeys();
    private final List<Pair<BasicOption, Mod>> options = new ArrayList<>();
    private static final String apiKeyMessage = "Your new API key is ";
    private boolean hasSynced = false;

    private HypixelKeys() {
        EventManager.INSTANCE.register(this);
    }

    public void addOption(BasicOption option, Mod mod) {
        options.add(Pair.of(option, mod));
    }

    @Subscribe
    private void onChatReceived(ChatReceiveEvent event) {
        if (Preferences.autoSetHypixelKey) {
            String message = event.getFullyUnformattedMessage();
            if (message.startsWith(apiKeyMessage)) {
                String tempApiKey = message.substring(apiKeyMessage.length());
                Multithreading.runAsync(() -> { //run this async as getting from the API normally would freeze minecraft
                    if (!HypixelUtils.INSTANCE.isValidKey(tempApiKey)
                    ) {
                        sendNotification("The API Key was invalid! Please try running the command again.", NotificationIcon.KEY);
                    } else {
                        boolean success = true;
                        for (Pair<BasicOption, Mod> option : options) {
                            try {
                                option.getKey().getField().set(option.getKey().getParent(), tempApiKey);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                                sendNotification(String.format("An issue was encountered setting the API key for %s in %s.", option.getKey().name, option.getValue().name), NotificationIcon.ERROR);
                                success = false;
                            }
                        }
                        if (success) {
                            sendNotification(String.format("Successfully set your key for %s option%s", options.size(), (options.size() > 1 ? "s" : "")), NotificationIcon.KEY);
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
        options.sort(Comparator.comparingInt(o -> o.getKey().getField().getAnnotation(HypixelKey.class).priority()));
        String firstValidKey = null;
        for (Pair<BasicOption, Mod> option : options) {
            try {
                String apiKey = (String) option.getKey().get();
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
                sendNotification("Some of your mods require a valid Hypixel API key. Click to open the API key options page.", () -> {
                    GuiUtils.displayScreen(OneConfigGui.create());
                    new TickDelay(() -> OneConfigGui.INSTANCE.openPage(new ModConfigPage(Preferences.getInstance().mod.defaultPage, true)), 2);
                }, 10000f, NotificationIcon.KEY);
            }
        }
    }

    public void setAllKeys(String hypixelKey) {
        if (hypixelKey == null) return;
        setKeys(hypixelKey);
    }

    private void setKeys(String hypixelKey) {
        boolean success = true;
        for (Pair<BasicOption, Mod> option : options) {
            try {
                option.getKey().getField().set(option.getKey().getParent(), hypixelKey);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                sendNotification(String.format("Failed to set key to %s in %s. If this continues, create a support ticket.",
                        option.getValue(), option.getKey().getParent().getClass().getName()), NotificationIcon.ERROR);
                success = false;
            }
        }
//        if (success) {
//            sendNotification(String.format("API key successfully synced to %s option%s.", options.size(), (options.size() > 1 ? "s" : "")), NotificationIcon.KEY);
//        }
    }

    public void testKeys() {
        int invalid = 0;
        int total = 0;
        for (Pair<BasicOption, Mod> option : options) {
            total = options.size();
            try {
                String apiKey = (String) option.getKey().get();
                if (!HypixelUtils.INSTANCE.isValidKey(apiKey)) {
                    invalid++;
                    //sendNotification("The API Key for " + option.getKey().name + " in " + option.getKey().category + "in" + option.getValue().name + " is invalid!");
                }
            } catch (Exception ignored) {
            }
        }
        if (invalid == 0) {
            if (total == 1) {
                sendNotification("Your API key is valid.", NotificationIcon.SUCCESS);
            } else {
                sendNotification(String.format("All %s of your API keys are valid.", total), NotificationIcon.SUCCESS);
            }
        } else {
            if (invalid != total) {
                sendNotification(String.format("You have %s invalid API keys & %s valid ones.", invalid, total), NotificationIcon.GRAY);
            } else {
                sendNotification("All of your API keys are invalid.", NotificationIcon.DANGER);
            }
        }
    }


    private void sendNotification(String message, NotificationIcon icon) {
        if (Platform.getGuiPlatform().getCurrentScreen() instanceof OneConfigGui) {
            GuiNotifications.INSTANCE.sendNotification(message, icon.getIcon().getSVG());
        } else {
            Notifications.INSTANCE.send("Hypixel API key manager", message, icon.getIcon());
        }
    }

    private void sendNotification(String message, Runnable runnable, float time, NotificationIcon icon) {
        Notifications.INSTANCE.send("OneConfig Hypixel API Key", message, icon.getIcon(), time, runnable);
    }

    enum NotificationIcon {
        KEY(new Icon("/assets/oneconfig/icons/KeyN.svg")),
        SUCCESS(new Icon("/assets/oneconfig/icons/successBulb.svg")),
        GRAY(new Icon("/assets/oneconfig/icons/grayBulb.svg")),
        DANGER(new Icon("/assets/oneconfig/icons/dangerBulb.svg")),
        ERROR(new Icon("/assets/oneconfig/icons/errorTriangle.svg"));

        private final Icon icon;

        private Icon getIcon() {return icon;}
        NotificationIcon(Icon icon) {this.icon = icon;}
    }
}
