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

import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.KeyBind;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
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
            max = 2f
    )
    public static float customScale = 1f;

    @Dropdown(
            name = "Opening Behavior",
            category = "Behavior",
            subcategory = "GUI Settings",
            description = "Choose which page will show when you open OneConfig",
            options = {
                    "Mods",
                    "Preferences",
                    "Previous page",
                    "Smart reset"
            },
            size = 2
    )
    public static int openingBehavior = 3;

    @Switch(
            name = "Show opening page animation",
            category = "Behavior",
            subcategory = "GUI Settings",
            description = "Whether or not to show the page switch animation when opening OneConfig",
            size = 2
    )
    public static boolean showPageAnimationOnOpen = false;

    @Slider(
            name = "Time before reset",
            category = "Behavior",
            subcategory = "GUI Settings",
            description = "How much time (in seconds) before opening back to the \"Mods\" page",
            min = 5,
            max = 60
    )
    public static int timeUntilReset = 15;

    @Number(
            name = "Search Distance",
            min = 0,
            max = 10,
            category = "Behavior",
            subcategory = "Search",
            description = "The maximum Levenshtein distance to search for similar config names",
            size = 2
    )
    public static int searchDistance = 2;

    @Switch(
            name = "Opening Animation",
            description = "Plays an animation when opening the GUI",
            category = "Animations",
            subcategory = "Global"
    )
    public static boolean guiOpenAnimation = true;

    @Switch(
            name = "Closing Animation",
            description = "Plays an animation when closing the GUI",
            category = "Animations",
            subcategory = "Global"
    )
    public static boolean guiClosingAnimation = true;

    @Slider(
            name = "Opening Time",
            description = "The duration of the opening and closing animations, in seconds",
            category = "Animations",
            subcategory = "Global",
            min = 0.05f,
            max = 2f
    )
    public static float animationTime = 0.6f;

    @Dropdown(
            name = "Opening Type",
            description = "The type of opening/closing animation to use",
            category = "Animations",
            subcategory = "Global",
            options = {"Subtle", "Full"}
    )
    public static int animationType = 0;

    @Switch(
            name = "Show Page Animations",
            description = "Enables or disables the page switch animation",
            category = "Animations",
            subcategory = "Pages"
    )
    public static boolean showPageAnimations = true;

    @Slider(
            name = "Page Animation Duration",
            description = "The duration of the page switch animation, in seconds",
            category = "Animations",
            subcategory = "Pages",
            min = 0.1f,
            max = .6f
    )
    public static float pageAnimationDuration = .3f;

    @Switch(
            name = "Toggle Switch Bounce",
            description = "Enables or disables the bounce animation on toggle switches",
            category = "Animations",
            subcategory = "Toggles"
    )
    public static boolean toggleSwitchBounce = true;

    @Slider(
            name = "Tracker Response Time",
            description = "The time it takes for the slider tracker to move, in milliseconds",
            category = "Animations",
            subcategory = "Sliders",
            min = 0f,
            max = 100f
    )
    public static float trackerResponseDuration = 60;

    @Dropdown(
            name = "Release Channel",
            options = {"Releases", "Pre-Releases"}
    )
    public static int updateChannel = 0;

    @Switch(
            name = "Debug Mode"
    )
    public static boolean DEBUG = false;

    @Button(
            name = "Show First Launch Message",
            description = "Shows the first launch message again",
            text = "Show"
    )
    public void showFirstLaunchMessage() {
        firstLaunch = true;
        oneconfigOpened = false;
        save();
    }

    public static boolean firstLaunch = true;
    public static transient boolean oneconfigOpened = false;

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
        addListener("animationType", () -> {
            if (Preferences.guiOpenAnimation) {
                // Force reset the animation
                OneConfigGui.INSTANCE.isClosed = true;
            }
        });
        addDependency("guiClosingAnimation", "guiOpenAnimation");
        addDependency("timeUntilReset", "Smart Opening Behavior", () -> openingBehavior == 3);
        addDependency("pageAnimationDuration", "showPageAnimations");
        INSTANCE = this;
    }

    public static Preferences getInstance() {
        return INSTANCE == null ? (INSTANCE = new Preferences()) : INSTANCE;
    }
}
