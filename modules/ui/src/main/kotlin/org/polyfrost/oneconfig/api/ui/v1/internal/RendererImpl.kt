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
import org.lwjgl.opengl.GL11
import org.polyfrost.oneconfig.api.ui.v1.api.LwjglApi
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
import java.nio.ByteOrder
import org.polyfrost.polyui.color.PolyColor as Color

class RendererImpl(
    private val isGl3: Boolean,
    private val lwjgl: LwjglApi,
    private val nanoVg: NanoVgApi,
    private val stb: StbApi
) : Renderer {

    private companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger("OneConfig/Renderer")

    }

    @Suppress("unused")
    internal data class NvgFont(
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
    private val fonts = mutableMapOf<Font, NvgFont>()

    private var defaultImageData: ByteArray? = null
    private var defaultImage = 0

    private val images = mutableMapOf<PolyImage, Int>()
    private val svgs = mutableMapOf<PolyImage, Pair<Long, Int2IntMap>>()

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
            val iHandle = nanoVg.createImage(iImage.width, iImage.height, iData.toDirectByteBuffer(), 0)
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
        if (!isGl3) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        }

        nanoVg.beginFrame(width, height, pixelRatio)
        isDrawing = true
    }

    override fun endFrame() {
        if (!isDrawing) throw IllegalStateException("Not drawing")

        nanoVg.endFrame()
        if (!isGl3) {
            GL11.glPopAttrib()
        }

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
        nanoVg.fontFaceId(getOrPopulateFont(font).id)
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
        nanoVg.imagePattern(x, y, width, height, 0f, getOrPopulateImage(image, width, height), 1f, paintAddress)
        if (colorMask != 0) {
            populateNvgColor(colorMask, nanoVg.getPaintColor(paintAddress))
        }

        nanoVg.beginPath()
        nanoVg.roundedRectVarying(x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius)
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
        getOrPopulateImage(image, 0f, 0f)
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
        val text = text.let { if (it.endsWith(" ")) "$it " else it }

        val output = FloatArray(4)
        val loadedFont = getOrPopulateFont(font)
        nanoVg.fontFaceId(loadedFont.id)
        nanoVg.textAlign(nanoVg.constants().NVG_ALIGN_LEFT() or nanoVg.constants().NVG_ALIGN_TOP())
        nanoVg.fontSize(fontSize)
        nanoVg.textBounds(0f, 0f, text, output)

        val width = output[2] - output[0]
        val height = output[3] - output[1]
        return Vec2(width.coerceAtLeast(1f), height.coerceAtLeast(1f)) // Coercing to at least 1x1 for now because this is returning 0 sometimes for some reason and PolyUI crashes when an element has 0 width & height
    }

    private fun getOrPopulateFont(font: Font): NvgFont {
        if (font.loadSync) {
            return getOrPopulateFontSynchronous(font)
        }

        return fonts.getOrPut(font) {
            font.loadAsync(errorHandler = errorHandler) { data ->
                val buffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).put(data).flip() as ByteBuffer

                queue.add {
                    val id = nanoVg.createFont(
                        font.name,
                        buffer
                    )

                    NvgFont(id, buffer)
                }
            }

            defaultFont!!
        }
    }

    private fun getOrPopulateFontSynchronous(font: Font): NvgFont {
        return fonts.getOrPut(font) {
            val data = font.load { errorHandler(it); return@getOrPut defaultFont!! }
            val buffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).put(data).flip() as ByteBuffer
            val id = nanoVg.createFont(font.name, buffer)
            NvgFont(id, buffer)
        }
    }

    private fun getOrPopulateImage(image: PolyImage, width: Float, height: Float): Int {
        if (image.width == 0f || image.height == 0f) {
            delete(image)
        }

        if (image.loadSync) {
            return getOrPopulateImageSynchronous(image, width, height)
        }

        return when (image.type) {
            PolyImage.Type.Raster -> {
                images.getOrPut(image) {
                    image.loadAsync(errorHandler) { data ->
                        val buffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).put(data).flip() as ByteBuffer

                        queue.add {
                            val widthOutput = IntArray(1)
                            val heightOutput = IntArray(1)
                            val result = stb.loadFromMemory(buffer, widthOutput, heightOutput, IntArray(1), 4)
                            image.size = Vec2(widthOutput[0].toFloat(), heightOutput[0].toFloat())
                            nanoVg.createImage(image.width, image.height, result, 0)
                        }
                    }
                    return defaultImage
                }
            }

            PolyImage.Type.Vector -> {
                val (svg, map) = svgs.getOrPut(image) {
                    image.loadAsync(errorHandler) { data ->
                        val buffer = ByteBuffer.allocateDirect(data.size + 1).order(ByteOrder.nativeOrder()).put(data).put(0).flip() as ByteBuffer

                        queue.add {
                            val (address, id) = loadSvg(image, buffer)
                            val map = Int2IntMap(4)
                            map[image.width.hashCode() * 31 + image.height.hashCode()] = id
                            svgs[image] = address to map
                        }
                    }

                    return defaultImage
                }

                if (image.width == 0f || image.height == 0f) {
                    val (svgWidth, svgHeight) = nanoVg.svgBounds(svg)
                    image.size = Vec2(svgWidth, svgHeight)
                }

                map.getOrPut(width.hashCode() * 31 + height.hashCode()) {
                    resizeSvg(svg, width, height, width, height)
                }
            }

            else -> throw NoWhenBranchMatchedException("Please specify image type for $image")
        }
    }

    private fun getOrPopulateImageSynchronous(image: PolyImage, width: Float, height: Float): Int {
        if (image.width == 0f || image.height == 0f) {
            delete(image)
        }

        return images.getOrPut(image) {
            val bytes = image.load { errorHandler(it); return@load defaultImageData!! }

            when (image.type) {
                PolyImage.Type.Raster -> {
                    val buffer = run {
                        val buffer = ByteBuffer.allocateDirect(bytes.size)
                            .order(ByteOrder.nativeOrder())
                            .put(bytes)
                            .flip() as ByteBuffer
                        val widthOutput = IntArray(1)
                        val heightOutput = IntArray(1)
                        val result = stb.loadFromMemory(buffer, widthOutput, heightOutput, IntArray(1), 4)
                        image.size = Vec2(widthOutput[0].toFloat(), heightOutput[0].toFloat())

                        result
                    }

                    nanoVg.createImage(image.width, image.height, buffer, 0)
                }

                PolyImage.Type.Vector -> {
                    val (svgImage, map) = svgs.getOrPut(image) {
                        val buffer = ByteBuffer.allocateDirect(bytes.size + 1) // +1 for null terminator
                            .order(ByteOrder.nativeOrder())
                            .put(bytes)
                            .put(0) // null terminator
                            .flip() as ByteBuffer
                        val (_, id) = loadSvg(image, buffer)
                        return id
                    }

                    if (image.width == 0f || image.height == 0f) {
                        val (svgWidth, svgHeight) = nanoVg.svgBounds(svgImage)
                        image.size = Vec2(svgWidth, svgHeight)
                    }

                    map.getOrPut(width.hashCode() * 31 + height.hashCode()) {
                        resizeSvg(svgImage, width, height, width, height)
                    }
                }

                else -> throw UnsupportedOperationException("Unsupported image type")
            }
        }
    }

    private fun loadSvg(image: PolyImage, data: ByteBuffer): Pair<Long, Int> {
        val (address, svgWidth, svgHeight) = nanoVg.parseSvg(data)

        image.size = Vec2(svgWidth, svgHeight)

        val map = Int2IntMap(4)
        val id = resizeSvg(address, svgWidth, svgHeight, svgWidth, svgHeight)
        map[image.width.hashCode() * 31 + image.height.hashCode()] = id
        svgs[image] = address to map

        return address to id
    }

    private fun resizeSvg(address: Long, svgWidth: Float, svgHeight: Float, width: Float, height: Float): Int {
        val w = (width * 2f).toInt()
        val h = (height * 2f).toInt()

        val dest = lwjgl.memAlloc(w * h * 4)
        val scale = cl1(width / svgWidth, height / svgHeight) * 2f

        nanoVg.rasterizeSvg(address, 0f, 0f, scale, dest, w, h, w * 4)
        return nanoVg.createImage(w.toFloat(), h.toFloat(), dest, 0)
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
