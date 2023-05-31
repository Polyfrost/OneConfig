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

package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.gui.OneUIScreen;

import java.awt.*;

/**
 * A GUI that uses RenderManager, NanoVG, and OneUIScreen to render a simple GUI.
 *
 * @see OneUIScreen
 * @see TestKotlinNanoVGGui_Test
 * @see NanoVGHelper
 */
public class TestNanoVGGui_Test extends OneUIScreen {

    @Override
    public void draw(long vg, float partialTicks, InputHandler inputHandler) {
        NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        long startTime = System.nanoTime();
        nanoVGHelper.drawRect(vg, 0, 0, 100, 100, Color.BLUE.getRGB());
        nanoVGHelper.drawRoundedRect(vg, 305, 305, 100, 100, Color.YELLOW.getRGB(), 8);
        nanoVGHelper.drawText(vg, "Hello!", 100, 100, Color.WHITE.getRGB(), 50, Fonts.BOLD);
        nanoVGHelper.drawLine(vg, 0, 0, 100, 100, 7, Color.PINK.getRGB());
        nanoVGHelper.drawCircle(vg, 200, 200, 50, Color.WHITE.getRGB());
        nanoVGHelper.drawText(vg, (float) (System.nanoTime() - startTime) / 1000000f + "ms", 500, 500, Color.WHITE.getRGB(), 100, Fonts.BOLD);
    }
}
