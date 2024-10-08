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
import org.lwjgl.nanovg.NSVGImage
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL2
import org.lwjgl.nanovg.NanoVGGL2.NVG_ANTIALIAS
import org.lwjgl.nanovg.NanoVGGL3
import org.lwjgl.opengl.GL11.*
import org.lwjgl.stb.STBImage.stbi_failure_reason
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil
import org.polyfrost.universal.UGraphics
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*
import java.nio.ByteBuffer
import java.util.IdentityHashMap
import org.polyfrost.polyui.color.PolyColor as Color

object RendererImpl : Renderer {
    @JvmStatic
    private val LOGGER = LogManager.getLogger("OneConfig/Renderer")
    private val nvgPaint: NVGPaint = NVGPaint.malloc()
    private val nvgColor: NVGColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()
    private val images = HashMap<PolyImage, Int>()
    private val svgs = HashMap<PolyImage, Pair<NSVGImage, Int2IntMap>>()
    private val fonts = IdentityHashMap<Font, NVGFont>()
    private var defaultFont: NVGFont? = null
    private var defaultImageData: ByteArray? = null
    private var defaultImage = 0
    private var vg: Long = 0L
    private var raster: Long = 0L
    private var drawing = false
    private val queue = ArrayList<() -> Unit>()

    // ByteBuffer.of("px\u0000")
    private val PIXELS: ByteBuffer = MemoryUtil.memAlloc(3).put(112).put(120).put(0).flip() as ByteBuffer
    private val errorHandler: (Throwable) -> Unit = { LOGGER.error("failed to load resource!", it) }
    var gl3 = false

    override fun init() {
        if (vg == 0L) {
            vg = if(gl3) NanoVGGL3.nvgCreate(NVG_ANTIALIAS) else NanoVGGL2.nvgCreate(NVG_ANTIALIAS)
        }
        if (raster == 0L) raster = nsvgCreateRasterizer()
        require(vg != 0L) { "Could not initialize NanoVG" }
        require(raster != 0L) { "Could not initialize NanoSVG" }

        if (defaultFont == null) {
            val font = PolyUI.defaultFonts.regular
            val fdata = font.load().toDirectByteBuffer()
            val fit = NVGFont(nvgCreateFontMem(vg, font.name, fdata, false), fdata)
            this.defaultFont = fit
            fonts[font] = fit
        }

        if (defaultImage == 0) {
            val iImage = PolyUI.defaultImage
            val iData = iImage.load()
            val iHandle = nvgCreateImageRGBA(vg, iImage.width.toInt(), iImage.height.toInt(), 0, iData.toDirectByteBuffer())
            require(iHandle != 0) { "NanoVG failed to initialize default image" }
            defaultImageData = iData
            images[iImage] = iHandle
            this.defaultImage = iHandle
        }
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        if (drawing) throw IllegalStateException("Already drawing")
        queue.fastRemoveIfReversed { it(); true }
        UGraphics.disableAlpha()
        if(!gl3) {
            glPushAttrib(GL_ALL_ATTRIB_BITS)
        }
        nvgBeginFrame(vg, width, height, pixelRatio)
        drawing = true
    }

    override fun endFrame() {
        if (!drawing) throw IllegalStateException("Not drawing")
        nvgEndFrame(vg)
        if(!gl3) {
            glPopAttrib()
        }
        drawing = false
    }

    override fun globalAlpha(alpha: Float) = nvgGlobalAlpha(vg, alpha)

    override fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) = nvgScale(vg, sx, sy)

    override fun rotate(angleRadians: Double, px: Float, py: Float) = nvgRotate(vg, angleRadians.toFloat())

    override fun skewX(angleRadians: Double, px: Float, py: Float) = nvgSkewX(vg, angleRadians.toFloat())

    override fun skewY(angleRadians: Double, px: Float, py: Float) = nvgSkewY(vg, angleRadians.toFloat())

    override fun transformsWithPoint() = false

    override fun push() = nvgSave(vg)

    override fun pop() = nvgRestore(vg)

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) = nvgScissor(vg, x, y, width, height)

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) = nvgIntersectScissor(vg, x, y, width, height)

    override fun popScissor() = nvgResetScissor(vg)

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
    ) {
        if (color.transparent) return
        nvgBeginPath(vg)
        nvgFontSize(vg, fontSize)
        nvgFontFaceId(vg, getFont(font))
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y, text)
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
        nvgImagePattern(vg, x, y, width, height, 0f, getImage(image, width, height), 1f, nvgPaint)
        if (colorMask != 0) {
            nvgARGB(colorMask, nvgPaint.innerColor())
        }
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image).also {
            if (it != null) {
                nvgDeleteImage(vg, it)
                return
            }
        }
        svgs.remove(image).also {
            if (it != null) {
                nsvgDelete(it.first)
                it.second.forEach { _, handle ->
                    nvgDeleteImage(vg, handle)
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
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        if (color(color, x, y, width, height)) {
            nvgFillPaint(vg, nvgPaint)
        } else {
            nvgFillColor(vg, nvgColor)
        }
        nvgFill(vg)
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
        nvgBeginPath(vg)
        nvgRoundedRectVarying(
            vg,
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        nvgStrokeWidth(vg, lineWidth)
        if (color(color, x, y, width, height)) {
            nvgStrokePaint(vg, nvgPaint)
        } else {
            nvgStrokeColor(vg, nvgColor)
        }
        nvgStroke(vg)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (color.transparent) return
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, width)
        if (color(color, x1, y1, x2, y2)) {
            nvgStrokePaint(vg, nvgPaint)
        } else {
            nvgStrokeColor(vg, nvgColor)
        }
        nvgStroke(vg)
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
        nvgBoxGradient(vg, x - spread, y - spread, width + spread * 2f, height + spread * 2f, radius + spread, blur, nvgColor, nvgColor2, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x - spread, y - spread - blur, width + spread * 2f + blur * 2f, height + spread * 2f + blur * 2f, radius + spread)
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgPathWinding(vg, NVG_HOLE)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    @Suppress("NAME_SHADOWING")
    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        // nanovg trims single whitespace, so add an extra one (lol)
        var text = text
        if (text.endsWith(' ')) {
            text += ' '
        }
        val out = FloatArray(4)
        nvgFontFaceId(vg, getFont(font))
        nvgTextAlign(vg, NVG_ALIGN_TOP or NVG_ALIGN_LEFT)
        nvgFontSize(vg, fontSize)
        nvgTextBounds(vg, 0f, 0f, text, out)
        val w = out[2] - out[0]
        val h = out[3] - out[1]
        return Vec2(w, h)
    }

    private fun color(color: Color) {
        nvgARGB(color.argb, nvgColor)
        if (color is Color.Gradient) {
            nvgARGB(color.argb2, nvgColor2)
        }
    }

    private fun nvgARGB(argb: Int, ptr: NVGColor) {
        nvgRGBA(
            (argb shr 16 and 0xFF).toByte(),
            (argb shr 8 and 0xFF).toByte(),
            (argb and 0xFF).toByte(),
            (argb shr 24 and 0xFF).toByte(),
            ptr
        )

    }

    private fun color(
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): Boolean {
        color(color)
        if (color !is Color.Gradient) return false
        when (color.type) {
            is Color.Gradient.Type.TopToBottom -> nvgLinearGradient(
                vg,
                x,
                y,
                x,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.LeftToRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> nvgLinearGradient(
                vg,
                x,
                y + height,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )

            is Color.Gradient.Type.Radial -> {
                val type = color.type as Color.Gradient.Type.Radial
                nvgRadialGradient(
                    vg,
                    if (type.centerX == -1f) x + (width / 2f) else type.centerX,
                    if (type.centerY == -1f) y + (height / 2f) else type.centerY,
                    type.innerRadius,
                    type.outerRadius,
                    nvgColor,
                    nvgColor2,
                    nvgPaint,
                )
            }

            is Color.Gradient.Type.Box -> nvgBoxGradient(
                vg,
                x,
                y,
                width,
                height,
                (color.type as Color.Gradient.Type.Box).radius,
                (color.type as Color.Gradient.Type.Box).feather,
                nvgColor,
                nvgColor2,
                nvgPaint,
            )
        }
        return true
    }

    private fun getFont(font: Font): Int {
        if (font.loadSync) return getFontSync(font)
        return fonts.getOrPut(font) {
            font.loadAsync(errorHandler = errorHandler) { data ->
                val it = data.toDirectByteBuffer()
                queue.add { fonts[font] = NVGFont(nvgCreateFontMem(vg, font.name, it, false), it) }
            }
            defaultFont!!
        }.id
    }

    private fun getFontSync(font: Font): Int {
        return fonts.getOrPut(font) {
            val data = font.load { errorHandler(it); return@getOrPut defaultFont!! }.toDirectByteBuffer()
            NVGFont(nvgCreateFontMem(vg, font.name, data, false), data)
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
                if (!image.size.isPositive) image.size = Vec2(svg.width(), svg.height())
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { svgResize(svg, width, height) }
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
                if (!image.size.isPositive) image.size = Vec2(svg.width(), svg.height())
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { svgResize(svg, width, height) }
            }

            PolyImage.Type.Raster -> {
                images.getOrPut(image) { loadImage(image, image.load { errorHandler(it); defaultImageData!! }.toDirectByteBuffer()) }
            }

            else -> throw NoWhenBranchMatchedException("Please specify image type for $image")
        }
    }

    private fun svgLoad(image: PolyImage, data: ByteBuffer): Int {
        val svg = nsvgParse(data, PIXELS, 96f) ?: throw IllegalStateException("Failed to parse SVG: ${image.resourcePath}")
        image.size = Vec2(svg.width(), svg.height())
        val map = Int2IntMap(4)
        val o = svgResize(svg, svg.width(), svg.height())
        map[image.size.hashCode()] = o
        svgs[image] = svg to map
        return o
    }

    private fun svgResize(svg: NSVGImage, width: Float, height: Float): Int {
        val wi = (width * 2f).toInt()
        val hi = (height * 2f).toInt()
        val dst = MemoryUtil.memAlloc(wi * hi * 4)
        val scale = cl1(width / svg.width(), height / svg.height()) * 2f
        nsvgRasterize(raster, svg, 0f, 0f, scale, dst, wi, hi, wi * 4)
        return nvgCreateImageRGBA(vg, wi, hi, 0, dst)
    }

    private fun loadImage(image: PolyImage, data: ByteBuffer): Int {
        val w = IntArray(1)
        val h = IntArray(1)
        val d = stbi_load_from_memory(data, w, h, IntArray(1), 4) ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stbi_failure_reason()}")
        image.size = Vec2(w[0].toFloat(), h[0].toFloat())
        return nvgCreateImageRGBA(vg, w[0], h[0], 0, d)
    }

    // asm: renderer is persistent
    override fun cleanup() {}

    private data class NVGFont(val id: Int, val data: ByteBuffer)
}
