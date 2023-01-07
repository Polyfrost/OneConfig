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
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.TickDelay;

public class Preferences extends InternalConfig {

    @KeyBind(
            name = "OneConfig Keybind",
            subcategory = "GUI Settings",
            description = "Choose what key opens the OneConfig UI",
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
            description = "Automatically sync your Hypixel API keys across all options marked as Hypixel API keys in OneConfig.",
            subcategory = "Hypixel"
    )
    public static boolean syncHypixelKeys = true;

    @Button(
            name = "Sync Hypixel API Keys",
            description = "Sync your Hypixel API keys across all options marked as Hypixel API keys in OneConfig.",
            subcategory = "Hypixel",
            text = "Sync"
    )
    private static void syncHypixelKeys() {
        HypixelKeys.INSTANCE.syncKeys();
    }

    @Button(
            name = "Remove All Syncable Hypixel API Keys",
            description = "Remove all (and only) fields marked as Hypixel API keys in OneConfig.",
            subcategory = "Hypixel",
            text = "Remove"
    )
    private static void removeAllHypixelKeys() {
        HypixelKeys.INSTANCE.setAllKeys("");
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

    @Number(
            name = "Search Distance",
            min = 0,
            max = 10,
            subcategory = "GUI Settings",
            description = "The maximum Levenshtein distance to search for similar config names."
    )
    public static int searchDistance = 2;

    @Switch(
            name = "Use custom GUI scale",
            subcategory = "GUI Settings"
    )
    public static boolean enableCustomScale = false;

    @Slider(
            name = "Custom GUI scale",
            subcategory = "GUI Settings",
            min = 0.5f,
            max = 2f
    )
    public static float customScale = 1f;

    @Switch(
            name = "Opening Animation",
            description = "Plays an animation when opening the GUI",
            subcategory = "GUI Settings"
    )
    public static boolean guiOpenAnimation = true;

    @Switch(
            name = "Closing Animation",
            description = "Plays an animation when closing the GUI",
            subcategory = "GUI Settings"
    )
    public static boolean guiClosingAnimation = true;

    @Slider(
            name = "Animation Duration",
            description = "The duration of the opening and closing animations, in seconds",
            subcategory = "GUI Settings",
            min = 0.05f,
            max = 2f
    )
    public static float animationTime = 0.6f;

    @Dropdown(
            name = "Animation Type",
            description = "The type of opening/closing animation to use",
            subcategory = "GUI Settings",
            options = {"Subtle", "Full"}
    )
    public static int animationType = 0;

    @Dropdown(
            name = "Release Channel",
            options = {"Releases", "Pre-Releases"}
    )
    public static int updateChannel = 0;

    @Switch(
            name = "Debug Mode"
    )
    public static boolean DEBUG = false;

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
        addListener("animationType", () -> {
            if (Preferences.guiOpenAnimation) {
                // Force reset the animation
                OneConfigGui.INSTANCE.isClosed = true;
            }
        });
        addDependency("guiClosingAnimation", "guiOpenAnimation");
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
