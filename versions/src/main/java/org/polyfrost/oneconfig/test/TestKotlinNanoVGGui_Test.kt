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

package org.polyfrost.oneconfig.test

import org.polyfrost.oneconfig.renderer.font.Fonts
import org.polyfrost.oneconfig.utils.InputHandler
import org.polyfrost.oneconfig.utils.dsl.*
import org.polyfrost.oneconfig.utils.gui.OneUIScreen
import java.awt.Color
import kotlin.system.measureTimeMillis

/**
 * A kotlinified version of [TestNanoVGGui_Test].
 * Uses OneConfig's Kotlin DSL to render instead of RenderManager
 *
 * @see nanoVG
 * @see TestNanoVGGui_Test
 */
class TestKotlinNanoVGGui_Test : OneUIScreen() {

    override fun draw(vg: Long, partialTicks: Float, inputHandler: InputHandler) {
        nanoVG(vg) {
            val millis = measureTimeMillis {
                drawRect(0f, 0f, 100f, 100f, Color.BLUE.rgb)
                drawRoundedRect(
                    305f, 305f, 100f, 100f, 8f, Color.YELLOW.rgb
                )
                drawText(
                    "Hello!", 100f, 100f, Color.WHITE.rgb, 50f, Fonts.BOLD
                )
                drawLine(
                    0f, 0f, 100f, 100f, 7f, Color.PINK.rgb
                )
                drawCircle(
                    200f, 200f, 50f, Color.WHITE.rgb
                )
            }
            drawText(
                millis.toString() + "ms",
                500f,
                500f,
                Color.WHITE.rgb,
                100f,
                Fonts.BOLD
            )
        }
    }
}