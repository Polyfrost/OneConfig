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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import org.polyfrost.compose.backend.asComposeImageBitmap
import org.polyfrost.oneconfig.api.notifications.v1.SvgMask

val LocalUiOversample = staticCompositionLocalOf { 1f }

private class Raster(val image: ImageBitmap?)

private val svgRasters = ConcurrentHashMap<String, Raster>()

private val svgSizes = ConcurrentHashMap<String, Size>()

private val svgRoot = Regex("<svg\\b[^>]*>")
private fun svgAttr(el: String, name: String): Float? =
    Regex("\\s$name=\"([0-9.]+)(px)?\"").find(el)?.groupValues?.get(1)?.toFloatOrNull()

class OversampledSvgPainter(
    bytes: ByteArray,
    private val oversample: Float,
    private val cacheKey: String? = null,
    density: Float = 1f,
) : Painter() {
    private val svg = String(bytes, Charsets.UTF_8)

    private fun measure(): Size {
        val root = svgRoot.find(svg)?.value ?: return Size.Unspecified
        val w = svgAttr(root, "width")
        val h = svgAttr(root, "height")
        if (w != null && h != null && w > 0f && h > 0f) return Size(w, h)
        val view = Regex("viewBox\\s*=\\s*\"([^\"]+)\"").find(root)?.groupValues?.get(1)
            ?.trim()?.split(Regex("[\\s,]+"))?.mapNotNull { it.toFloatOrNull() }
        return if (view != null && view.size >= 4 && view[2] > 0f && view[3] > 0f) Size(view[2], view[3]) else Size.Unspecified
    }

    private val defaultSize: Size =
        if (cacheKey == null) measure() else svgSizes.getOrPut(cacheKey, ::measure)

    // like Compose's SVG painter, unitless sizes are density-independent pixels
    override val intrinsicSize: Size = if (defaultSize.isSpecified) defaultSize * density else defaultSize

    private var alpha = 1f
    private var colorFilter: ColorFilter? = null
    private var cached: ImageBitmap? = null
    private var cachedKey = ""

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
        val px = max(1, ceil(max(size.width, size.height) * max(1f, oversample)).toInt())
        // a white mask only stands in for the original colors when the filter replaces them
        val mask = (colorFilter as? BlendModeColorFilter)?.blendMode == BlendMode.SrcIn
        val key = "$px@$mask"
        var img = cached
        if (img == null || cachedKey != key) {
            val shared = cacheKey
            img = if (shared == null) rasterize(px, mask)
            else svgRasters.getOrPut("$shared@$key") { Raster(rasterize(px, mask)) }.image
            cached = img
            cachedKey = key
        }
        img ?: return
        drawImage(
            image = img,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(img.width, img.height),
            dstSize = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = FilterQuality.Medium,
        )
    }

    private fun rasterize(px: Int, mask: Boolean): ImageBitmap? =
        SvgMask.rasterize(svg, px, if (mask) -1 else null)?.asComposeImageBitmap()
}
