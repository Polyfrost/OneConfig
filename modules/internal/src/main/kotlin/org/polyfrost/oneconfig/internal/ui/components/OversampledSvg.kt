/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Data
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import kotlin.math.ceil
import kotlin.math.max

val LocalUiOversample = staticCompositionLocalOf { 1f }

class OversampledSvgPainter(
    bytes: ByteArray,
    private val oversample: Float,
) : Painter() {
    private val dom = SVGDOM(Data.makeFromBytes(bytes))
    private val root = dom.root

    private val defaultSize: Size = run {
        val w = root?.width?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        val h = root?.height?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        if (w > 0f && h > 0f) Size(w, h) else Size.Unspecified
    }

    init {
        if (root?.viewBox == null && defaultSize.isSpecified) {
            root?.viewBox = Rect.makeXYWH(0f, 0f, defaultSize.width, defaultSize.height)
        }
    }

    override val intrinsicSize: Size get() = defaultSize

    private var alpha = 1f
    private var colorFilter: ColorFilter? = null
    private var cached: ImageBitmap? = null
    private var cachedKey = -1L

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        if (size.width <= 0f || size.height <= 0f) return
        val over = max(1f, oversample)
        val pw = max(1, ceil(size.width * over).toInt())
        val ph = max(1, ceil(size.height * over).toInt())
        val key = pw.toLong() shl 32 or ph.toLong()
        var img = cached
        if (img == null || cachedKey != key) {
            img = rasterize(pw, ph)
            cached = img
            cachedKey = key
        }
        drawImage(
            image = img,
            dstSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = FilterQuality.Medium,
        )
    }

    private fun rasterize(pw: Int, ph: Int): ImageBitmap {
        val surface = Surface.makeRasterN32Premul(pw, ph)
        root?.width = SVGLength(pw.toFloat(), SVGLengthUnit.PX)
        root?.height = SVGLength(ph.toFloat(), SVGLengthUnit.PX)
        root?.preserveAspectRatio = SVGPreserveAspectRatio(SVGPreserveAspectRatioAlign.NONE)
        dom.render(surface.canvas)
        val image = surface.makeImageSnapshot().toComposeImageBitmap()
        surface.close()
        return image
    }
}
