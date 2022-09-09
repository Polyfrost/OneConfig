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

package cc.polyfrost.oneconfig.internal.assets;

import cc.polyfrost.oneconfig.renderer.AssetLoader;
import cc.polyfrost.oneconfig.renderer.SVG;

/**
 * An enum of SVGs used in OneConfig.
 *
 * @see cc.polyfrost.oneconfig.renderer.RenderManager#drawSvg(long, String, float, float, float, float, int)
 * @see AssetLoader
 */
public class SVGs {
    public static final SVG ONECONFIG = new SVG("/assets/oneconfig/icons/OneConfig.svg");
    public static final SVG ONECONFIG_OFF = new SVG("/assets/oneconfig/icons/OneConfigOff.svg");
    public static final SVG COPYRIGHT_FILL = new SVG("/assets/oneconfig/icons/CopyrightFill.svg");
    public static final SVG APERTURE_FILL = new SVG("/assets/oneconfig/icons/ApertureFill.svg");
    public static final SVG ARROWS_CLOCKWISE_BOLD = new SVG("/assets/oneconfig/icons/ArrowsClockwiseBold.svg");
    public static final SVG FADERS_HORIZONTAL_BOLD = new SVG("/assets/oneconfig/icons/FadersHorizontalBold.svg");
    public static final SVG GAUGE_FILL = new SVG("/assets/oneconfig/icons/GaugeFill.svg");
    public static final SVG GEAR_SIX_FILL = new SVG("/assets/oneconfig/icons/GearSixFill.svg");
    public static final SVG MAGNIFYING_GLASS_BOLD = new SVG("/assets/oneconfig/icons/MagnifyingGlassBold.svg");
    public static final SVG NOTE_PENCIL_BOLD = new SVG("/assets/oneconfig/icons/NotePencilBold.svg");
    public static final SVG PAINT_BRUSH_BROAD_FILL = new SVG("/assets/oneconfig/icons/PaintBrushBroadFill.svg");
    public static final SVG USER_SWITCH_FILL = new SVG("/assets/oneconfig/icons/UserSwitchFill.svg");
    public static final SVG X_CIRCLE_BOLD = new SVG("/assets/oneconfig/icons/XCircleBold.svg");
    public static final SVG CARET_LEFT = new SVG("/assets/oneconfig/icons/CaretLeftBold.svg");
    public static final SVG CARET_RIGHT = new SVG("/assets/oneconfig/icons/CaretRightBold.svg");
    public static final SVG INFO_ARROW = new SVG("/assets/oneconfig/icons/InfoArrow.svg");

    // OLD ICONS
    public static final SVG BOX = new SVG("/assets/oneconfig/old-icons/Box.svg");
    public static final SVG CHECKBOX_TICK = new SVG("/assets/oneconfig/old-icons/CheckboxTick.svg");
    public static final SVG CHECK_CIRCLE = new SVG("/assets/oneconfig/old-icons/CheckCircle.svg");
    public static final SVG CHEVRON_DOWN = new SVG("/assets/oneconfig/old-icons/ChevronDown.svg");
    public static final SVG CHEVRON_UP = new SVG("/assets/oneconfig/old-icons/ChevronUp.svg");
    public static final SVG COPY = new SVG("/assets/oneconfig/old-icons/Copy.svg");
    public static final SVG DROPDOWN_LIST = new SVG("/assets/oneconfig/old-icons/DropdownList.svg");
    public static final SVG ERROR = new SVG("/assets/oneconfig/old-icons/Error.svg");
    public static final SVG EYE = new SVG("/assets/oneconfig/old-icons/Eye.svg");
    public static final SVG EYE_OFF = new SVG("/assets/oneconfig/old-icons/EyeOff.svg");
    public static final SVG HEART_FILL = new SVG("/assets/oneconfig/old-icons/HeartFill.svg");
    public static final SVG HEART_OUTLINE = new SVG("/assets/oneconfig/old-icons/HeartOutline.svg");
    public static final SVG HELP_CIRCLE = new SVG("/assets/oneconfig/old-icons/HelpCircle.svg");
    public static final SVG HISTORY = new SVG("/assets/oneconfig/old-icons/History.svg");
    public static final SVG INFO_CIRCLE = new SVG("/assets/oneconfig/old-icons/InfoCircle.svg");
    public static final SVG KEYSTROKE = new SVG("/assets/oneconfig/old-icons/Keystroke.svg");
    public static final SVG PASTE = new SVG("/assets/oneconfig/old-icons/Paste.svg");
    public static final SVG POP_OUT = new SVG("/assets/oneconfig/old-icons/PopOut.svg");
    public static final SVG WARNING = new SVG("/assets/oneconfig/old-icons/Warning.svg");
}
