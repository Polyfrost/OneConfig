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

import cc.polyfrost.oneconfig.config.annotations.Color;
import cc.polyfrost.oneconfig.config.annotations.Exclude;
import cc.polyfrost.oneconfig.config.annotations.Slider;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;

public class Themes extends InternalConfig {

    // Config options

    // COLORS

    // bg.page
    @Color(
            name = "Page default",
            description = "bg.page.page",
            category = "Colors",
            subcategory = "Page background"
    )
    public static OneColor bgPageDefault = new OneColor(17, 23, 28, 255);

    @Color(
            name = "Page elevated",
            description = "bg.page.elevated",
            category = "Colors",
            subcategory = "Page background"
    )
    public static OneColor bgPageElevated = new OneColor(26, 34, 41, 255);

    @Color(
            name = "Sidebar overlay",
            description = "bg.page.sidebar",
            category = "Colors",
            subcategory = "Page background"
    )
    public static OneColor bgSidebarOverlay = new OneColor(17, 23, 28, 80);

    // border
    @Color(
            name = "Border gray",
            description = "border.border.10%",
            category = "Colors",
            subcategory = "Border"
    )
    public static OneColor borderBorder10 = new OneColor(255, 255, 255, 25);

    // component.bg
    @Color(
            name = "Component background",
            description = "component.bg.bg",
            category = "Colors",
            subcategory = "Component background"
    )
    public static OneColor componentBgBg = new OneColor(23, 31, 37, 255);

    @Color(
            name = "Component hover",
            description = "component.bg.elevated",
            category = "Colors",
            subcategory = "Component background"
    )
    public static OneColor componentBgElevated = new OneColor(23, 31, 37, 255);


    @Switch(
            name = "OneConfig logo",
            description = "Toggle the visibility of the OneConfig logo",
            category = "Interface"
    )
    public static boolean oneConfigLogo = true;

    // ROUNDING
    @Slider(
            name = "OneConfig UI rounding",
            description = "Rounding of OneConfig UI",
            category = "Interface",
            min = 0f, max = 50f
    )
    public static float oneConfigUIRounding = 20f;

    @Switch(
            name = "Dissosciate Sidebar",
            description = "Dissosciate the sidebar from the main page",
            category = "Interface"
    )
    public static boolean dissosciateSidebar = false;

    @Slider(
            name = "Separation Width",
            description = "Width of the separation between the sidebar and the main page",
            category = "Interface",
            min = 0f, max = 100f
    )
    public static float separationWidth = 20f;

    @Slider(
            name = "Backdrop Intensity",
            description = "Intensity of the backdrop",
            category = "Interface",
            min = 0f, max = 100f
    )
    public static float backdropIntensity = 64f;

    @Slider(
            name = "Backdrop Spread",
            description = "Spread of the backdrop",
            category = "Interface",
            min = 0f, max = 100f
    )
    public static float backdropSpread = 0f;

    @Color(
            name = "Backdrop Color",
            description = "The backdrop color",
            category = "Interface"
    )
    public static OneColor backdropColor = new OneColor(0, 0, 0, 63);

    // Initialize the config

    @Exclude
    private static Themes INSTANCE;

    public Themes() {
        super("Themes", "themes.json");
        initialize();

        INSTANCE = this;
    }

    public static Themes getInstance() {
        return INSTANCE == null ? (INSTANCE = new Themes()) : INSTANCE;
    }
}
