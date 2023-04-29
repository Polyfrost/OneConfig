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
            name = "Page depressed",
            description = "bg.page.depressed",
            category = "Colors",
            subcategory = "Page background"
    )
    public static OneColor bgPageDepressed = new OneColor(14, 19, 23, 255);

    // border
    @Color(
            name = "Border gray",
            description = "border.border.10%",
            category = "Colors",
            subcategory = "Border"
    )
    public static OneColor borderBorder10 = new OneColor(255, 255, 255, 25);

    @Color(
            name = "Border light",
            description = "border.border.5%",
            category = "Colors",
            subcategory = "Border"
    )
    public static OneColor borderBorder5 = new OneColor(255, 255, 255, 12);

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

    @Color(
            name = "Component pressed",
            description = "component.bg.pressed",
            category = "Colors",
            subcategory = "Component background"
    )
    public static OneColor componentBgPressed = new OneColor(34, 44, 53, 255);

    @Color(
            name = "Component deselected",
            description = "component.bg.deselected",
            category = "Colors",
            subcategory = "Component background"
    )
    public static OneColor componentBgDeselected = new OneColor(26, 34, 41, 0);

    @Color(
            name = "Component disabled",
            description = "component.bg.disabled",
            category = "Colors",
            subcategory = "Component background"
    )
    public static OneColor componentBgDisabled = new OneColor(26, 34, 41, 126);

    // ROUNDING
    @Slider(
            name = "OneConfig UI rounding",
            description = "Rounding of OneConfig UI",
            category = "Interface",
            min = 0f, max = 50f
    )
    public static float oneConfigUIRounding = 20f;


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
