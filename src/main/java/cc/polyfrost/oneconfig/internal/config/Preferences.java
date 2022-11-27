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

import cc.polyfrost.oneconfig.config.annotations.*;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.TickDelay;

public class Preferences extends InternalConfig {

    @Dropdown(
            name = "Release Channel",
            options = {"Releases", "Pre-Releases"}
    )
    public static int updateChannel = 0;

    @Switch(
            name = "Debug Mode"
    )
    public static boolean DEBUG = false;

    @KeyBind(
            name = "OneConfig Keybind",
            size = 2
    )
    public static OneKeyBind oneConfigKeyBind = new OneKeyBind(UKeyboard.KEY_RSHIFT);

    @Switch(
            name = "Automatically Detect Hypixel API Key",
            description = "Automatically detect your Hypixel API key from running /api new in chat.",
            subcategory = "Hypixel"
    )
    public static boolean autoSetHypixelKey = true;

    @Switch(
            name = "Sync Hypixel API Keys on Startup",
            description = "Automatically sync your Hypixel API keys across your mod configs on startup.",
            subcategory = "Hypixel"
    )
    public static boolean syncHypixelKeys = true;

    @Button(
            name = "Sync Hypixel API Keys",
            description = "Sync your Hypixel API keys across your mod configs.",
            subcategory = "Hypixel",
            text = "Sync"
    )
    private static void syncHypixelKeys() {
        HypixelKeys.INSTANCE.syncKeys();
    }

    @Text(
            name = "Hypixel API Key",
            description = "Set all options marked as Hypixel API keys by the developer to this value.",
            subcategory = "Hypixel"
    )
    private static String hypixelKey = "";

    @Switch(
            name = "Enable Blur",
            subcategory = "GUI Settings"
    )
    public static boolean enableBlur = true;

    @Switch(
            name = "Use custom GUI scale",
            subcategory = "GUI Settings"
    )
    public static boolean enableCustomScale = false;

    @Slider(
            name = "Custom GUI scale",
            subcategory = "GUI Settings",
            min = 0.5f,
            max = 5f
    )
    public static float customScale = 1f;

    @Exclude
    private static Preferences INSTANCE;

    public Preferences() {
        super("Preferences", "Preferences.json");
        initialize();
        addListener("enableBlur", () -> BlurHandler.INSTANCE.reloadBlur(Platform.getGuiPlatform().getCurrentScreen()));
        registerKeyBind(oneConfigKeyBind, () -> new TickDelay(() -> Platform.getGuiPlatform().setCurrentScreen(OneConfigGui.create()), 1));
        addListener("updateChannel", () -> {
            OneConfigConfig.updateChannel = updateChannel;
            OneConfigConfig.getInstance().save();
        });
        addListener("hypixelKey", () -> HypixelKeys.INSTANCE.setAllKeys(hypixelKey));
        INSTANCE = this;
    }

    @Override
    public void save() {
        hypixelKey = "";
        super.save();
    }

    public static Preferences getInstance() {
        return INSTANCE == null ? (INSTANCE = new Preferences()) : INSTANCE;
    }
}
