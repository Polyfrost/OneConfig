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

package org.polyfrost.oneconfig.api.ui.v1.internal

import org.apache.logging.log4j.LogManager
import org.lwjgl.system.MemoryUtil
import org.polyfrost.oneconfig.api.ui.v1.api.NanoVgApi
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi
import org.polyfrost.universal.UGraphics
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*
import java.nio.ByteBuffer
import java.util.IdentityHashMap
import org.polyfrost.polyui.color.PolyColor as Color

class RendererImpl(
    private val nanoVg: NanoVgApi,
    private val stb: StbApi
) : Renderer {

    private companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger("OneConfig/Renderer")

    }

    @Suppress("unused")
    internal class NvgFont(
        val id: Int,
        val buffer: ByteBuffer
    )

    private var isDrawing = false

    private var paintAddress = -1L
        get() {
            if (field == -1L) {
                field = nanoVg.createPaint()
            }

            return field
        }

    private var color1Address = -1L
        get() {
            if (field == -1L) {
                field = nanoVg.createColor()
            }

            return field
        }

    private var color2Address = -1L
        get() {
            if (field == -1L) {
                field = nanoVg.createColor()
            }

            return field
        }

    private var defaultFont: NvgFont? = null
    private val fonts = IdentityHashMap<Font, NvgFont>()

    private var defaultImageData: ByteArray? = null
    private var defaultImage = 0

    private val images = HashMap<PolyImage, Int>()
    private val svgs = HashMap<PolyImage, Pair<Long, Int2IntMap>>()

    private val queue = ArrayList<() -> Unit>()

    private val errorHandler: (Throwable) -> Unit = { LOGGER.error("failed to load resource!", it) }

    override fun init() {
        nanoVg.maybeSetup()

        if (defaultFont == null) {
            val font = PolyUI.defaultFonts.regular
            val fdata = font.load().toDirectByteBuffer()
            val fit = NvgFont(nanoVg.createFont(font.name, fdata), fdata)
            this.defaultFont = fit
            fonts[font] = fit
        }

        if (defaultImage == 0) {
            val iImage = PolyUI.defaultImage
            val iData = iImage.load()
            val iHandle = nanoVg.createImage(iImage.width, iImage.height, iData.toDirectByteBuffer())
            require(iHandle != 0) { "NanoVG failed to initialize default image" }
            defaultImageData = iData
            images[iImage] = iHandle
            this.defaultImage = iHandle
        }
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        if (isDrawing) throw IllegalStateException("Already drawing")

        queue.fastRemoveIfReversed { it(); true }
        UGraphics.disableAlpha()
//        if(!gl3) {
//            glPushAttrib(GL_ALL_ATTRIB_BITS)
//        }
        nanoVg.beginFrame(width, height, pixelRatio)
        isDrawing = true
    }

    override fun endFrame() {
        if (!isDrawing) throw IllegalStateException("Not drawing")

        nanoVg.endFrame()
//        if(!gl3) {
//            glPopAttrib()
//        }
        isDrawing = false
    }

    override fun globalAlpha(alpha: Float) = nanoVg.globalAlpha(alpha)

    override fun translate(x: Float, y: Float) = nanoVg.translate(x, y)

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) = nanoVg.scale(sx, sy)

    override fun rotate(angleRadians: Double, px: Float, py: Float) = nanoVg.rotate(angleRadians.toFloat())

    override fun skewX(angleRadians: Double, px: Float, py: Float) = nanoVg.skewX(angleRadians.toFloat())

    override fun skewY(angleRadians: Double, px: Float, py: Float) = nanoVg.skewY(angleRadians.toFloat())

    override fun transformsWithPoint() = false

    override fun push() = nanoVg.save()

    override fun pop() = nanoVg.restore()

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) = nanoVg.scissor(x, y, width, height)

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) = nanoVg.intersectScissor(x, y, width, height)

    override fun popScissor() = nanoVg.resetScissor()

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
    ) {
        if (color.transparent) return

        nanoVg.beginPath()
        nanoVg.fontSize(fontSize)
        nanoVg.fontFaceId(getFont(font))
        nanoVg.textAlign(nanoVg.constants().NVG_ALIGN_LEFT() or nanoVg.constants().NVG_ALIGN_TOP())
        populateFillOrColor(color, x, y, 0f, 0f)
        nanoVg.fillColor(color1Address)
        nanoVg.text(x, y, text)
    }

    override fun image(
        image: PolyImage,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        colorMask: Int,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    ) {
        nanoVg.imagePattern(x, y, width, height, 0f, getImage(image, width, height), 1f, paintAddress)
        if (colorMask != 0) {
            populateNvgColor(colorMask, nanoVg.getPaintColor(paintAddress))
        }

        nanoVg.beginPath()
        nanoVg.roundedRectVarying(
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        nanoVg.fillPaint(paintAddress)
        nanoVg.fill()
    }

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image).also {
            if (it != null) {
                nanoVg.deleteImage(it)
                return
            }
        }
        svgs.remove(image).also {
            if (it != null) {
                nanoVg.deleteSvg(it.first)
                it.second.forEach { _, handle ->
                    nanoVg.deleteImage(handle)
                }
            }
        }
    }

    override fun initImage(image: PolyImage) {
        getImage(image, 0f, 0f)
    }

    override fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    ) {
        if (color.transparent) return
        // note: nvg checks params and draws classic rect if 0, so we don't need to
        nanoVg.beginPath()
        nanoVg.roundedRectVarying(
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        populateFillOrColor(color, x, y, width, height)
        nanoVg.fill()
    }

    override fun hollowRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: Color,
        lineWidth: Float,
        topLeftRadius: Float,
        topRightRadius: Float,
        bottomLeftRadius: Float,
        bottomRightRadius: Float,
    ) {
        if (color.transparent) return
        nanoVg.beginPath()
        nanoVg.roundedRectVarying(
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        nanoVg.strokeWidth(lineWidth)
        populateStrokeColor(color, x, y, width, height)
        nanoVg.stroke()
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (color.transparent) return
        nanoVg.beginPath()
        nanoVg.moveTo(x1, y1)
        nanoVg.lineTo(x2, y2)
        nanoVg.strokeWidth(width)
        populateStrokeColor(color, x1, y1, x2, y2)
        nanoVg.stroke()
    }

    override fun dropShadow(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        blur: Float,
        spread: Float,
        radius: Float,
    ) {
        nanoVg.boxGradient(paintAddress, x - spread, y - spread, width + spread * 2f, height + spread * 2f, radius + spread, blur, color1Address, color2Address)
        nanoVg.beginPath()
        nanoVg.roundedRect(x - spread, y - spread - blur, width + spread * 2f + blur * 2f, height + spread * 2f + blur * 2f, radius + spread)
        nanoVg.roundedRect(x, y, width, height, radius)
        nanoVg.pathWinding(nanoVg.constants().NVG_HOLE())
        nanoVg.fillPaint(paintAddress)
        nanoVg.fill()
    }

    @Suppress("NAME_SHADOWING")
    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        // nanovg trims single whitespace, so add an extra one (lol)
        var text = text
        if (text.endsWith(' ')) {
            text += ' '
        }
        val out = FloatArray(4)
        nanoVg.fontFaceId(getFont(font))
        nanoVg.textAlign(nanoVg.constants().NVG_ALIGN_TOP() or nanoVg.constants().NVG_ALIGN_LEFT())
        nanoVg.fontSize(fontSize)
        nanoVg.textBounds(0f, 0f, text, out)
        val w = out[2] - out[0]
        val h = out[3] - out[1]
        return Vec2(w, h)
    }

    private fun getFont(font: Font): Int {
        if (font.loadSync) return getFontSync(font)
        return fonts.getOrPut(font) {
            font.loadAsync(errorHandler = errorHandler) { data ->
                val it = data.toDirectByteBuffer()
                queue.add { fonts[font] = NvgFont(nanoVg.createFont(font.name, it), it) }
            }
            defaultFont!!
        }.id
    }

    private fun getFontSync(font: Font): Int {
        return fonts.getOrPut(font) {
            val data = font.load { errorHandler(it); return@getOrPut defaultFont!! }.toDirectByteBuffer()
            NvgFont(nanoVg.createFont(font.name, data), data)
        }.id
    }

    private fun getImage(image: PolyImage, width: Float, height: Float): Int {
        if (image.loadSync) return getImageSync(image, width, height)
        return when (image.type) {
            PolyImage.Type.Vector -> {
                val (svg, map) = svgs.getOrPut(image) {
                    image.loadAsync(errorHandler) {
                        queue.add { svgLoad(image, it.toDirectByteBufferNT()) }
                    }
                    return defaultImage
                }
                val svgSize = nanoVg.svgBounds(svg)
                if (!image.size.isPositive) image.size = Vec2(svgSize[0], svgSize[1])
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { svgResize(svg, width, height, width, height) }
            }

            PolyImage.Type.Raster -> {
                images.getOrPut(image) {
                    image.loadAsync(errorHandler) {
                        queue.add { images[image] = loadImage(image, it.toDirectByteBuffer()) }
                    }
                    defaultImage
                }
            }

            else -> throw NoWhenBranchMatchedException("Please specify image type for $image")
        }
    }

    private fun getImageSync(image: PolyImage, width: Float, height: Float): Int {
        return when (image.type) {
            PolyImage.Type.Vector -> {
                val (svg, map) = svgs[image] ?: return svgLoad(image, image.load { errorHandler(it); defaultImageData!! }.toDirectByteBufferNT())
                val svgSize = nanoVg.svgBounds(svg)
                if (!image.size.isPositive) image.size = Vec2(svgSize[0], svgSize[1])
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { svgResize(svg, width, height, width, height) }
            }

            PolyImage.Type.Raster -> {
                images.getOrPut(image) { loadImage(image, image.load { errorHandler(it); defaultImageData!! }.toDirectByteBuffer()) }
            }

            else -> throw NoWhenBranchMatchedException("Please specify image type for $image")
        }
    }

    private fun svgLoad(image: PolyImage, data: ByteBuffer): Int {
        val svg = nanoVg.parseSvg(data)
        image.size = Vec2(svg.width, svg.height)

        val map = Int2IntMap(4)
        val id = svgResize(svg.handle, svg.width, svg.height, svg.width, svg.height)
        map[image.size.hashCode()] = id
        svgs[image] = svg.handle to map

        return id
    }

    private fun svgResize(handle: Long, svgWidth: Float, svgHeight: Float, width: Float, height: Float): Int {
        val wi = (width * 2f).toInt()
        val hi = (height * 2f).toInt()
        val dst = MemoryUtil.memAlloc(wi * hi * 4)
        val scale = cl1(width / svgWidth, height / svgHeight) * 2f
        nanoVg.rasterizeSvg(handle, 0f, 0f, wi, hi, scale, wi * 4, dst)
        return nanoVg.createImage(wi.toFloat(), hi.toFloat(), dst)
    }

    private fun loadImage(image: PolyImage, data: ByteBuffer): Int {
        val w = IntArray(1)
        val h = IntArray(1)
        val d = stb.loadFromMemory(data, w, h, IntArray(1), 4)
        image.size = Vec2(w[0].toFloat(), h[0].toFloat())
        return nanoVg.createImage(w[0].toFloat(), h[0].toFloat(), d)
    }

    private fun populateNvgColor(argb: Int, colorAddress: Long) {
        nanoVg.rgba(colorAddress, argb)
    }

    private fun populateStaticColor(color: Color) {
        populateNvgColor(color.argb, color1Address)
        if (color is Color.Gradient) {
            populateNvgColor(color.argb2, color2Address)
        }
    }

    private fun populateColor(
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): Boolean {
        populateStaticColor(color)
        if (color !is Color.Gradient) return false

        when (color.type) {
            is Color.Gradient.Type.TopToBottom -> nanoVg.linearGradient(
                paintAddress,
                x,
                y,
                x,
                y + height,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> nanoVg.linearGradient(
                paintAddress,
                x,
                y,
                x + width,
                y + height,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.LeftToRight -> nanoVg.linearGradient(
                paintAddress,
                x,
                y,
                x + width,
                y,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> nanoVg.linearGradient(
                paintAddress,
                x,
                y + height,
                x + width,
                y,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.Radial -> {
                val type = color.type as Color.Gradient.Type.Radial
                nanoVg.radialGradient(
                    paintAddress,
                    if (type.centerX == -1f) x + (width / 2f) else type.centerX,
                    if (type.centerY == -1f) y + (height / 2f) else type.centerY,
                    type.innerRadius,
                    type.outerRadius,
                    color1Address,
                    color2Address
                )
            }

            is PolyColor.Gradient.Type.Box -> nanoVg.boxGradient(
                paintAddress,
                x,
                y,
                width,
                height,
                (color.type as PolyColor.Gradient.Type.Box).radius,
                (color.type as PolyColor.Gradient.Type.Box).feather,
                color1Address,
                color2Address
            )
        }
        return true
    }

    private fun populateFillOrColor(color: Color, x: Float, y: Float, width: Float, height: Float) {
        if (populateColor(color, x, y, width, height)) {
            nanoVg.fillPaint(paintAddress)
        } else {
            nanoVg.fillColor(color1Address)
        }
    }

    private fun populateStrokeColor(color: Color, x: Float, y: Float, width: Float, height: Float) {
        if (populateColor(color, x, y, width, height)) {
            nanoVg.strokePaint(paintAddress)
        } else {
            nanoVg.strokeColor(color1Address)
        }
    }

    // asm: renderer is persistent
    override fun cleanup() {}
}
