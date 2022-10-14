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

package cc.polyfrost.oneconfig.gui.pages;

import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;

public class CreditsPage extends Page {
    public CreditsPage() {
        super("Credits");
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        nanoVGHelper.drawSvg(vg, SVGs.ONECONFIG_FULL_DARK, x + 15f, y + 20f, 474, 102);
        y -= 32;

        nanoVGHelper.drawText(vg, "Development Team", x + 20, y + 180, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, " - MoonTidez - Founder and lead designer", x + 20, y + 205, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - DeDiamondPro - Founder, Config backend, GUI frontend, HUD", x + 20, y + 220, -1, 12, Fonts.REGULAR);        // +15/line
        nanoVGHelper.drawText(vg, " - nextdaydelivery - GUI frontend, Render Manager, Utilities", x + 20, y + 235, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Wyvest - Gradle, Render Manager, VCAL, Utilities", x + 20, y + 250, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Pauline - Utilities", x + 20, y + 265, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Caledonian - Designer", x + 20, y + 280, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - xtrm - Legacy/Modern hacky compatibility", x + 20, y + 295, -1, 12, Fonts.REGULAR);

        nanoVGHelper.drawText(vg, "Libraries", x + 20, y + 333, -1, 24, Fonts.SEMIBOLD);
        nanoVGHelper.drawText(vg, " - LWJGLTwoPointFive (DJTheRedstoner) - LWJGL2 function provider", x + 20, y + 355, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - #getResourceAsStream (SpinyOwl) - IO Utility and shadow", x + 20, y + 370, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - NanoVG (memononen) - NanoVG Library", x + 20, y + 385, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - UniversalCraft (Essential team) - Multiversioning bindings", x + 20, y + 400, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - https://easings.net/ - Easing functions", x + 20, y + 415, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Quiltflower (Quilt Team) - Gradle decompiler", x + 20, y + 430, -1, 12, Fonts.REGULAR);
        nanoVGHelper.drawText(vg, " - Seraph (Scherso) - Locraw and Multithreading utilities", x + 20, y + 445, -1, 12, Fonts.REGULAR);
    }

    @Override
    public boolean isBase() {
        return true;
    }
}
