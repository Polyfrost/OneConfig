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

import dev.deftu.omnicore.client.render.OmniTextureManager
import dev.deftu.omnicore.common.OmniLoader
import org.apache.logging.log4j.LogManager
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.LwjglApi
import org.polyfrost.oneconfig.api.ui.v1.api.NanoVgApi
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.color.PolyColor
import org.polyfrost.polyui.data.Font
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.renderer.Renderer
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.utils.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.polyfrost.polyui.color.PolyColor as Color

class RendererImpl(
    private val isGl3: Boolean,
    private val lwjgl: LwjglApi,
    private val vg: NanoVgApi,
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

    private var mcVersion = -1
    private var isDrawing = false

    private var paintAddress = -1L
        get() {
            if (field == -1L) {
                field = vg.createPaint()
            }

            return field
        }

    private var color1Address = -1L
        get() {
            if (field == -1L) {
                field = vg.createColor()
            }

            return field
        }

    private var color2Address = -1L
        get() {
            if (field == -1L) {
                field = vg.createColor()
            }

            return field
        }

    private var defaultFont: NvgFont? = null
    private val fonts = mutableMapOf<Font, NvgFont>()

    private var defaultImageData: ByteArray? = null
    private var defaultImage = 0

    private val images = mutableMapOf<PolyImage, Int>()
    private val svgs = mutableMapOf<PolyImage, Pair<NanoVgApi.SVG, Int2IntMap>>()

    private var prevProgram = -1
    private var prevTexture = -1
    private var prevTextureBinding = -1
    private var prevVao = -1

    private val lineHeight = FloatArray(1)

    private val queue = ArrayList<() -> Unit>()

    private val errorHandler: (Throwable) -> Unit = { LOGGER.error("failed to load resource!", it) }

    override fun init() {
        if (mcVersion == -1) {
            mcVersion = OmniLoader.paddedMinecraftVersion
        }

        vg.maybeSetup()

        if (defaultFont == null) {
            val font = PolyUI.defaultFonts.regular
            val fdata = font.load().toDirectByteBuffer()
            val fit = NvgFont(vg.createFont(font.name, fdata), fdata)
            this.defaultFont = fit
            fonts[font] = fit
        }

        if (defaultImage == 0) {
            val iImage = PolyUI.defaultImage
            val iData = iImage.load()
            val iHandle = vg.createImage(iImage.size.x, iImage.size.y, iData.toDirectByteBuffer(), 0)
            require(iHandle != 0) { "NanoVG failed to initialize default image" }
            defaultImageData = iData
            images[iImage] = iHandle
            this.defaultImage = iHandle
        }
    }

    override fun beginFrame(width: Float, height: Float, pixelRatio: Float) {
        if (isDrawing) throw IllegalStateException("Already drawing")

        if (mcVersion >= 1_16_05) {
            prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
            prevVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING)
            prevTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
            prevTextureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }

        queue.fastRemoveIfReversed { it(); true }

        if (mcVersion <= 1_12_02) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
        }

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        vg.beginFrame(width, height, pixelRatio)
        isDrawing = true
    }

    override fun endFrame() {
        if (!isDrawing) return

        vg.endFrame()

        if (mcVersion <= 1_12_02) {
            GL11.glPopAttrib()
        }

        Platform.gl().updateGameRenderStateAlongsideNanoVG()

        if (mcVersion >= 1_16_05) {
            if (prevProgram != -1) {
                GL20.glUseProgram(prevProgram)
            }
            if (prevTexture != -1) {
                OmniTextureManager.setActiveTexture(prevTexture)
                OmniTextureManager.bindTexture(prevTextureBinding)
            }
            if (prevVao != -1) {
                GL30.glBindVertexArray(prevVao)
            }
        }

        isDrawing = false
    }

    override fun globalAlpha(alpha: Float) = vg.globalAlpha(alpha)

    override fun translate(x: Float, y: Float) = vg.translate(x, y)

    override fun scale(sx: Float, sy: Float, px: Float, py: Float) = vg.scale(sx, sy)

    override fun rotate(angleRadians: Double, px: Float, py: Float) = vg.rotate(angleRadians.toFloat())

    override fun skewX(angleRadians: Double, px: Float, py: Float) = vg.skewX(angleRadians.toFloat())

    override fun skewY(angleRadians: Double, px: Float, py: Float) = vg.skewY(angleRadians.toFloat())

    override fun transformsWithPoint() = false

    override fun push() = vg.save()

    override fun pop() = vg.restore()

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) = vg.scissor(x, y, width, height)

    override fun pushScissorIntersecting(x: Float, y: Float, width: Float, height: Float) = vg.intersectScissor(x, y, width, height)

    override fun popScissor() = vg.resetScissor()

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
    ) {
        if (color.transparent) return
        // todo (nextday): what the fuck is going on here

        val fontId = getOrPopulateFont(font).id
        vg.fontFaceId(fontId)
        vg.textAlign(vg.constants().NVG_ALIGN_LEFT() or vg.constants().NVG_ALIGN_TOP())
        vg.fontSize(fontSize)

        val ascender = FloatArray(1)
        val descender = FloatArray(1)
        val lineHeight = FloatArray(1)
        vg.textMetrics(ascender, descender, lineHeight)

        val baselineY = y + (lineHeight[0] - ascender[0]) / 2f

        // Draw background fill if needed
        val (width, _) = textBounds(font, text, fontSize)
        vg.beginPath()
        populateFillOrColor(color, x, y - lineHeight[0] / 2f, width, lineHeight[0])

        vg.text(x, baselineY, text)
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
        vg.imagePattern(x, y, width, height, 0f, getOrPopulateImage(image, width, height), 1f, paintAddress)
        if (colorMask != 0) {
            populateNvgColor(colorMask, vg.getPaintColor(paintAddress))
        }

        vg.beginPath()
        vg.roundedRectVarying(x, y, width, height, topLeftRadius, topRightRadius, bottomRightRadius, bottomLeftRadius)
        vg.fillPaint(paintAddress)
        vg.fill()
    }

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image).also {
            if (it != null) {
                vg.deleteImage(it)
                return
            }
        }
        svgs.remove(image).also {
            if (it != null) {
                vg.deleteSvg(it.first.address)
                it.second.forEach { _, handle ->
                    vg.deleteImage(handle)
                }
            }
        }
    }

    override fun initImage(image: PolyImage, size: Vec2) {
        getOrPopulateImage(image, size.x, size.y)
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
        vg.beginPath()
        vg.roundedRectVarying(
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
        vg.fill()
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
        vg.beginPath()
        vg.roundedRectVarying(
            x,
            y,
            width,
            height,
            topLeftRadius,
            topRightRadius,
            bottomRightRadius,
            bottomLeftRadius,
        )
        vg.strokeWidth(lineWidth)
        populateStrokeColor(color, x, y, width, height)
        vg.stroke()
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float) {
        if (color.transparent) return
        vg.beginPath()
        vg.moveTo(x1, y1)
        vg.lineTo(x2, y2)
        vg.strokeWidth(width)
        populateStrokeColor(color, x1, y1, x2, y2)
        vg.stroke()
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
        vg.boxGradient(paintAddress, x - spread, y - spread, width + spread * 2f, height + spread * 2f, radius + spread, blur, color1Address, color2Address)
        vg.beginPath()
        vg.roundedRect(x - spread, y - spread - blur, width + spread * 2f + blur * 2f, height + spread * 2f + blur * 2f, radius + spread)
        vg.roundedRect(x, y, width, height, radius)
        vg.pathWinding(vg.constants().NVG_HOLE())
        vg.fillPaint(paintAddress)
        vg.fill()
    }

    @Suppress("NAME_SHADOWING")
    override fun textBounds(font: Font, text: String, fontSize: Float): Vec2 {
        vg.fontFaceId(getOrPopulateFont(font).id)
        vg.fontSize(fontSize)

        vg.textAlign(vg.constants().NVG_ALIGN_LEFT() or vg.constants().NVG_ALIGN_TOP())
        val width = vg.textBounds(0f, 0f, text, null)

        lineHeight[0] = fontSize.coerceAtLeast(1f)
        vg.textMetrics(null, null, lineHeight)

        return Vec2(width.coerceAtLeast(1f), lineHeight[0]) // Coercing to at least 1 x fontSize for now because this is returning 0 sometimes for some reason and PolyUI crashes when an element has 0 width & height
    }

    private fun getOrPopulateFont(font: Font): NvgFont {
        if (font.loadSync) {
            return getOrPopulateFontSynchronous(font)
        }

        return fonts.getOrPut(font) {
            font.loadAsync(errorHandler = errorHandler) { data ->
                val buffer = ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).put(data).flip() as ByteBuffer

                queue.add {
                    val id = vg.createFont(
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
            val id = vg.createFont(font.name, buffer)
            NvgFont(id, buffer)
        }
    }

    private fun getOrPopulateImage(image: PolyImage, width: Float, height: Float): Int {
        if (image.loadSync || (width == 0f && height == 0f)) return getOrPopulateImageSynchronous(image, width, height)
        return when (image.type) {
            PolyImage.Type.Vector -> {
                val (svg, map) = svgs[image] ?: run {
                    image.loadAsync(errorHandler) {
                        queue.add { loadSvg(image, it.toDirectByteBufferNT()) }
                    }
                    return defaultImage
                }
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { resizeSvg(svg, width, height) }
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

    private fun getOrPopulateImageSynchronous(image: PolyImage, width: Float, height: Float): Int {
        return when (image.type) {
            PolyImage.Type.Vector -> {
                val (svg, map) = svgs[image] ?: return loadSvg(image, image.load { errorHandler(it); defaultImageData!! }.toDirectByteBufferNT())
                if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width, svg.height))
                map.getOrPut(width.hashCode() * 31 + height.hashCode()) { resizeSvg(svg, width, height) }
            }

            PolyImage.Type.Raster -> {
                images.getOrPut(image) { loadImage(image, image.load { errorHandler(it); defaultImageData!! }.toDirectByteBuffer()) }
            }

            else -> throw NoWhenBranchMatchedException("Please specify image type for $image")
        }
    }

    private fun loadImage(image: PolyImage, data: ByteBuffer): Int {
        val w = IntArray(1)
        val h = IntArray(1)
        val d = stb.loadFromMemory(data, w, h, IntArray(1), 4) ?: throw IllegalStateException("Failed to load image ${image.resourcePath}: ${stb.failureReason()}")
        if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(w[0].toFloat(), h[0].toFloat()))
        return vg.createImage(w[0].toFloat(), h[0].toFloat(), d, 0)
    }

    private fun loadSvg(image: PolyImage, data: ByteBuffer): Int {
        val svg = vg.parseSvg(data)
        val map = Int2IntMap(4)
        if (!image.size.isPositive) PolyImage.setImageSize(image, Vec2(svg.width, svg.height))
        val o = resizeSvg(svg, svg.width, svg.height)
        map[image.size.hashCode()] = o
        svgs[image] = svg to map
        return o
    }

    private fun resizeSvg(svg: NanoVgApi.SVG, width: Float, height: Float): Int {
        val wi = ((if (width == 0f) svg.width else width) * 2f).toInt()
        val hi = ((if (height == 0f) svg.height else height) * 2f).toInt()
        val dst = lwjgl.memAlloc(wi * hi * 4)
        val scale = cl1(width / svg.width, height / svg.height) * 2f
        vg.rasterizeSvg(svg.address, 0f, 0f, scale, dst, wi, hi, wi * 4)
        return vg.createImage(wi.toFloat(), hi.toFloat(), dst, 0)
    }

    private fun populateNvgColor(argb: Int, colorAddress: Long) {
        vg.rgba(colorAddress, argb)
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
            is Color.Gradient.Type.TopToBottom -> vg.linearGradient(
                paintAddress,
                x,
                y,
                x,
                y + height,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> vg.linearGradient(
                paintAddress,
                x,
                y,
                x + width,
                y + height,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.LeftToRight -> vg.linearGradient(
                paintAddress,
                x,
                y,
                x + width,
                y,
                color1Address,
                color2Address
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> vg.linearGradient(
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
                vg.radialGradient(
                    paintAddress,
                    if (type.centerX == -1f) x + (width / 2f) else type.centerX,
                    if (type.centerY == -1f) y + (height / 2f) else type.centerY,
                    type.innerRadius,
                    type.outerRadius,
                    color1Address,
                    color2Address
                )
            }

            is PolyColor.Gradient.Type.Box -> vg.boxGradient(
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
            vg.fillPaint(paintAddress)
        } else {
            vg.fillColor(color1Address)
        }
    }

    private fun populateStrokeColor(color: Color, x: Float, y: Float, width: Float, height: Float) {
        if (populateColor(color, x, y, width, height)) {
            vg.strokePaint(paintAddress)
        } else {
            vg.strokeColor(color1Address)
        }
    }

    // asm: renderer is persistent
    override fun cleanup() {}
}
