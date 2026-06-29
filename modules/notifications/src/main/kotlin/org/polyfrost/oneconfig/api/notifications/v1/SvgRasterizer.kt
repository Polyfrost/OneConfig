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

package org.polyfrost.oneconfig.api.notifications.v1

import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.Surface
import org.jetbrains.skia.svg.SVGDOM
import java.util.concurrent.ConcurrentHashMap

internal object SvgRasterizer {
    private val cache = ConcurrentHashMap<String, Optional>()

    private class Optional(val image: Image?)

    fun get(iconName: String, pixelSize: Int): Image? = getByPath("/assets/oneconfig/ico/$iconName.svg", pixelSize)

    fun getByPath(resourcePath: String, pixelSize: Int): Image? {
        val size = pixelSize.coerceIn(1, 512)
        return cache.getOrPut("$resourcePath@$size") { Optional(rasterize(resourcePath, size)) }.image
    }

    private fun rasterize(resourcePath: String, size: Int): Image? = runCatching {
        val bytes = readResource(resourcePath) ?: return null
        val dom = SVGDOM(Data.makeFromBytes(bytes))
        val root = dom.root
        val intrinsicW = root?.width?.value?.takeIf { it > 0f } ?: size.toFloat()
        val intrinsicH = root?.height?.value?.takeIf { it > 0f } ?: size.toFloat()
        dom.setContainerSize(intrinsicW, intrinsicH)
        val scale = size / maxOf(intrinsicW, intrinsicH)
        val w = maxOf(1, Math.round(intrinsicW * scale))
        val h = maxOf(1, Math.round(intrinsicH * scale))
        val surface = Surface.makeRasterN32Premul(w, h)
        surface.canvas.scale(scale, scale)
        dom.render(surface.canvas)
        surface.makeImageSnapshot()
    }.getOrNull()

    private fun readResource(path: String): ByteArray? {
        val loaders = listOf(
            SvgRasterizer::class.java.classLoader,
            Thread.currentThread().contextClassLoader,
        )
        for (loader in loaders) {
            val stream = loader?.getResourceAsStream(path.removePrefix("/"))
                ?: SvgRasterizer::class.java.getResourceAsStream(path)
            if (stream != null) return stream.use { it.readBytes() }
        }
        return null
    }
}
