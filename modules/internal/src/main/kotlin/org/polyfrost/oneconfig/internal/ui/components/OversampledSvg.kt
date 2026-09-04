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
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.svg.SVGPreserveAspectRatio
import org.jetbrains.skia.svg.SVGPreserveAspectRatioAlign
import kotlin.math.ceil
import kotlin.math.max

val LocalUiOversample = staticCompositionLocalOf { 1f }

// the painter and its DOM cannot be shared, but the bitmap they produce can: it is immutable and
// the tint is a draw time filter, so one raster serves every call site whatever colour it draws
private val svgRasters = ConcurrentHashMap<String, ImageBitmap>()

// rasterising overwrites the width and height it is read from, so the size is kept separately
// and only the first painter for an icon pays for it
private val svgSizes = ConcurrentHashMap<String, Size>()

class OversampledSvgPainter(
    private val bytes: ByteArray,
    private val oversample: Float,
    /** identifies the source so rasters and sizes can be shared, null disables sharing */
    private val cacheKey: String? = null,
) : Painter() {
    // parsed on first use, not on construction: a scroll back builds a new painter, and a cached
    // raster needs no document at all
    private val dom: SVGDOM by lazy {
        SVGDOM(Data.makeFromBytes(bytes)).also { parsed ->
            val r = parsed.root ?: return@also
            if (r.viewBox != null) return@also
            val w = r.width.withUnit(SVGLengthUnit.PX).value
            val h = r.height.withUnit(SVGLengthUnit.PX).value
            if (w > 0f && h > 0f) r.viewBox = Rect.makeXYWH(0f, 0f, w, h)
        }
    }

    private fun measure(): Size {
        val r = dom.root
        val w = r?.width?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        val h = r?.height?.withUnit(SVGLengthUnit.PX)?.value ?: 0f
        return if (w > 0f && h > 0f) Size(w, h) else Size.Unspecified
    }

    private val defaultSize: Size =
        if (cacheKey == null) measure() else svgSizes.getOrPut(cacheKey, ::measure)

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
            val shared = cacheKey
            img = if (shared == null) rasterize(pw, ph)
            else svgRasters.getOrPut("$shared@${pw}x$ph") { rasterize(pw, ph) }
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
        val root = dom.root
        root?.width = SVGLength(pw.toFloat(), SVGLengthUnit.PX)
        root?.height = SVGLength(ph.toFloat(), SVGLengthUnit.PX)
        root?.preserveAspectRatio = SVGPreserveAspectRatio(SVGPreserveAspectRatioAlign.NONE)
        dom.render(surface.canvas)
        val image = surface.makeImageSnapshot().toComposeImageBitmap()
        surface.close()
        return image
    }
}
