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

/*
 * This file is part of PolyUI
 * PolyUI - Fast and lightweight UI framework
 * Copyright (C) 2023 Polyfrost and its contributors.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/polui-jvm>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     PolyUI is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation,
 * AND the simple request that you adequately accredit us if you use PolyUI.
 * See details here <https://github.com/Polyfrost/polyui-jvm/ACCREDITATION.md>.
 *     This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>.
 */

package cc.polyfrost.polyui.renderer.impl

import cc.polyfrost.polyui.PolyUI
import cc.polyfrost.polyui.color.Color
import cc.polyfrost.polyui.property.Settings
import cc.polyfrost.polyui.renderer.Renderer
import cc.polyfrost.polyui.renderer.data.Font
import cc.polyfrost.polyui.renderer.data.Framebuffer
import cc.polyfrost.polyui.renderer.data.PolyImage
import cc.polyfrost.polyui.unit.TextAlign
import cc.polyfrost.polyui.unit.Unit
import cc.polyfrost.polyui.unit.Vec2
import cc.polyfrost.polyui.unit.px
import cc.polyfrost.polyui.utils.clearUsing
import cc.polyfrost.polyui.utils.getResourceStream
import cc.polyfrost.polyui.utils.getResourceStreamNullable
import cc.polyfrost.polyui.utils.toByteBuffer
import org.lwjgl.nanovg.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL2.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.stb.STBImage
import org.lwjgl.stb.STBImageResize
import org.lwjgl.system.MemoryUtil
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max

class NVGRenderer(width: Float, height: Float) : Renderer(width, height) {
    private val nvgPaint: NVGPaint = NVGPaint.malloc()
    private val nvgColor: NVGColor = NVGColor.malloc()
    private val nvgColor2: NVGColor = NVGColor.malloc()
    private val fbos = IdentityHashMap<Framebuffer, NVGLUFramebuffer>()
    private val images = IdentityHashMap<PolyImage, NVGImage>()
    private val fonts = IdentityHashMap<Font, NVGFont>()
    private var vg: Long = -1

    override fun init() {
        vg = nvgCreate(if (settings.useAntialiasing) NVG_ANTIALIAS else 0)
        require(vg != -1L) { "Could not initialize NanoVG" }
    }

    override fun beginFrame() {
        nvgBeginFrame(vg, width, height, pixelRatio)
    }

    override fun endFrame() = nvgEndFrame(vg)

    override fun gblAlpha(alpha: Float) = nvgGlobalAlpha(vg, alpha)

    override fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    override fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    override fun rotate(angleRadians: Double) = nvgRotate(vg, angleRadians.toFloat())

    override fun skewX(angleRadians: Double) = nvgSkewX(vg, angleRadians.toFloat())

    override fun skewY(angleRadians: Double) = nvgSkewY(vg, angleRadians.toFloat())

    override fun push() = nvgSave(vg)

    override fun pop() = nvgRestore(vg)

    override fun pushScissor(x: Float, y: Float, width: Float, height: Float) = nvgScissor(vg, x, y, width, height)

    override fun popScissor() = nvgResetScissor(vg)

    override fun drawFramebuffer(fbo: Framebuffer, x: Float, y: Float, width: Float, height: Float) {
        val framebuffer = fbos[fbo] ?: throw IllegalStateException("unknown framebuffer!")
        drawImage(framebuffer.image(), x, y, width, height, 0)
    }

    override fun text(
        font: Font,
        x: Float,
        y: Float,
        text: String,
        color: Color,
        fontSize: Float,
        textAlign: TextAlign
    ) {
        if (color === Color.TRANSPARENT) return
        nvgBeginPath(vg)
        nvgFontSize(vg, fontSize)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, textAlign(textAlign))
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
        bottomRightRadius: Float
    ) {
        val img = getImage(image)
        nvgImagePattern(vg, x, y, width, height, 0f, img.id, 1f, nvgPaint)
        if (colorMask != 0) {
            nvgRGBA(
                (colorMask shr 16 and 0xFF).toByte(),
                (colorMask shr 8 and 0xFF).toByte(),
                (colorMask and 0xFF).toByte(),
                (colorMask shr 24 and 0xFF).toByte(),
                nvgPaint.innerColor()
            )
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
            bottomLeftRadius
        )
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun drawImage(img: Int, x: Float, y: Float, width: Float, height: Float, colorMask: Int = 0) {
        nvgImagePattern(vg, x, y, width, height, 0f, img, 1f, nvgPaint)
        if (colorMask != 0) {
            nvgRGBA(
                (colorMask shr 16 and 0xFF).toByte(),
                (colorMask shr 8 and 0xFF).toByte(),
                (colorMask and 0xFF).toByte(),
                (colorMask shr 24 and 0xFF).toByte(),
                nvgPaint.innerColor()
            )
        }
        nvgBeginPath(vg)
        nvgRect(vg, x, y, width, height)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    override fun createFramebuffer(width: Float, height: Float): Framebuffer {
        val f = Framebuffer(width, height)
        fbos[f] = nvgluCreateFramebuffer(
            vg,
            width.toInt(),
            height.toInt(),
            0
        ) ?: throw ExceptionInInitializerError("Could not create: $f (possibly an invalid sized layout?)")
        return f
    }

    override fun delete(fbo: Framebuffer?) {
        fbos.remove(fbo).also {
            if (it == null) {
                PolyUI.LOGGER.error("Framebuffer not found when deleting it, already cleaned?")
                return
            }
            nvgluDeleteFramebuffer(vg, it)
        }
    }

    override fun delete(font: Font?) {
        fonts.remove(font)
    }

    override fun delete(image: PolyImage?) {
        images.remove(image).also {
            if (it != null) {
                nvgDeleteImage(vg, it.id)
                MemoryUtil.memFree(it.data)
            }
        }
    }

    override fun bindFramebuffer(fbo: Framebuffer?) {
        if (fbo == null) return
        nvgEndFrame(vg)
        nvgluBindFramebuffer(vg, fbos[fbo] ?: throw NullPointerException("Cannot bind: $fbo does not exist!"))
        glViewport(0, 0, fbo.width.toInt(), fbo.height.toInt())
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        nvgBeginFrame(vg, fbo.width, fbo.height, pixelRatio)
    }

    override fun unbindFramebuffer(fbo: Framebuffer?) {
        nvgEndFrame(vg)
        nvgluBindFramebuffer(vg, null)
        glViewport(0, 0, width.toInt(), height.toInt())
        nvgBeginFrame(vg, width, height, pixelRatio)
    }

    override fun initImage(image: PolyImage) {
        getImage(image)
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
        bottomRightRadius: Float
    ) {
        if (color === Color.TRANSPARENT) return
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
            bottomLeftRadius
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
        bottomRightRadius: Float
    ) {
        if (color === Color.TRANSPARENT) return
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
            bottomLeftRadius
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
        if (color === Color.TRANSPARENT) return
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
        radius: Float
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
    override fun textBounds(font: Font, text: String, fontSize: Float, textAlign: TextAlign): Vec2<Unit.Pixel> {
        // nanovg trims single whitespace, so add an extra one (lol)
        var text = text
        if (text.endsWith(' ')) {
            text += ' '
        }
        val out = FloatArray(4)
        nvgFontFaceId(vg, getFont(font).id)
        nvgTextAlign(vg, textAlign(textAlign))
        nvgFontSize(vg, fontSize)
        nvgTextBounds(vg, 0f, 0f, text, out)
        return Vec2(out[2].px, out[3].px)
    }

    private fun textAlign(textAlign: TextAlign): Int {
        return when (textAlign) {
            TextAlign.Left -> NVG_ALIGN_LEFT or NVG_ALIGN_TOP
            TextAlign.Center -> NVG_ALIGN_CENTER or NVG_ALIGN_TOP
            TextAlign.Right -> NVG_ALIGN_RIGHT or NVG_ALIGN_TOP
        }
    }

    private fun color(color: Color) {
        if (color is Color.Gradient) {
            nvgRGBA(color.r.toByte(), color.g.toByte(), color.b.toByte(), color.a.toByte(), nvgColor)
            nvgRGBA(
                color.color2.r.toByte(),
                color.color2.g.toByte(),
                color.color2.b.toByte(),
                color.color2.a.toByte(),
                nvgColor2
            )
        } else {
            nvgRGBA(color.r.toByte(), color.g.toByte(), color.b.toByte(), color.a.toByte(), nvgColor)
        }
    }

    private fun color(
        color: Color,
        x: Float,
        y: Float,
        width: Float,
        height: Float
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
                nvgPaint
            )

            is Color.Gradient.Type.TopLeftToBottomRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y + height,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.LeftToRight -> nvgLinearGradient(
                vg,
                x,
                y,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.BottomLeftToTopRight -> nvgLinearGradient(
                vg,
                x,
                y + height,
                x + width,
                y,
                nvgColor,
                nvgColor2,
                nvgPaint
            )

            is Color.Gradient.Type.Radial -> {
                nvgRadialGradient(
                    vg,
                    // smart-cast impossible: [x] is a public API property declared in different module (bit cringe)
                    if ((color.type as Color.Gradient.Type.Radial).centerX == -1f) x + (width / 2f) else (color.type as Color.Gradient.Type.Radial).centerX,
                    if ((color.type as Color.Gradient.Type.Radial).centerY == -1f) y + (height / 2f) else (color.type as Color.Gradient.Type.Radial).centerY,
                    (color.type as Color.Gradient.Type.Radial).innerRadius,
                    (color.type as Color.Gradient.Type.Radial).outerRadius,
                    nvgColor,
                    nvgColor2,
                    nvgPaint
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
                nvgPaint
            )
        }
        return true
    }

    private fun getFont(font: Font): NVGFont {
        return fonts[font] ?: run {
            val data =
                getResourceStreamNullable(font.resourcePath)?.toByteBuffer()
                    ?: if (settings.resourcePolicy == Settings.ResourcePolicy.WARN) {
                        getResourceStream(
                            DefaultFont.resourcePath
                        ).also {
                            PolyUI.LOGGER.warn(
                                "Failed to get font: {}, falling back to default font!",
                                font.resourcePath
                            )
                        }.toByteBuffer()
                    } else {
                        throw ExceptionInInitializerError("Failed to get font: ${font.resourcePath}")
                    }
            val ft = nvgCreateFontMem(vg, font.name, data, 0)
            NVGFont(ft, data).also { fonts[font] = it }
        }
    }

    private fun getImage(image: PolyImage): NVGImage {
        return images[image] ?: run {
            val stream = getResourceStreamNullable(image.resourcePath)
                ?: if (settings.resourcePolicy == Settings.ResourcePolicy.WARN) {
                    getResourceStream(DefaultImage.resourcePath)
                        .also {
                            PolyUI.LOGGER.warn(
                                "Failed to get image: {}, falling back to default image!",
                                image.resourcePath
                            )
                        }
                } else {
                    throw ExceptionInInitializerError("Failed to get image: ${image.resourcePath}")
                }
            val data: ByteBuffer
            when (image.type) {
                PolyImage.Type.PNG, PolyImage.Type.JPEG, PolyImage.Type.BMP -> {
                    val w = IntArray(1)
                    val h = IntArray(1)
                    data = STBImage.stbi_load_from_memory(
                        stream.toByteBuffer(),
                        w,
                        h,
                        IntArray(1),
                        4
                    ).also {
                        if (it == null) {
                            throw Exception("Failed to initialize image: $image")
                        }
                        if (image.width == -1f || image.height == -1f) {
                            val sh = image.height != -1f
                            val sw = image.width != -1f
                            if (!sw) {
                                if (!sh) {
                                    image.width = w[0].toFloat()
                                    image.height = h[0].toFloat()
                                    return@also
                                } else {
                                    // !sw, sh
                                    val ratio = image.height / h[0].toFloat()
                                    image.width = w[0].toFloat() * ratio
                                }
                            } else {
                                // !sh, sw
                                val ratio = image.width / w[0].toFloat()
                                image.height = h[0].toFloat() * ratio
                            }
                        }
                        PolyUI.LOGGER.info("resizing image ${image.resourcePath}: ${w[0]}x${h[0]} -> ${image.width}x${image.height}")
                        STBImageResize.stbir_resize_uint8(
                            it, w[0], h[0],
                            0, it,
                            image.width.toInt(), image.height.toInt(),
                            0, 4
                        )
                    } ?: throw Exception("Failed to initialize image: $image")
                }

                PolyImage.Type.SVG -> {
                    val d = InputStreamReader(stream).readText() as CharSequence
                    val svg =
                        NanoSVG.nsvgParse(d, "px", 96f) ?: throw Exception("Failed to open SVG: $image (invalid data?)")
                    val raster = NanoSVG.nsvgCreateRasterizer()
                    val scale = if (image.width != -1f || image.height != -1f) {
                        max(image.width / svg.width(), image.height / svg.height())
                    } else {
                        1f
                    }
                    image.width = (svg.width() * scale).toInt().toFloat()
                    image.height = (svg.height() * scale).toInt().toFloat()
                    data = MemoryUtil.memAlloc(image.width.toInt() * image.height.toInt() * 4)
                    NanoSVG.nsvgRasterize(
                        raster, svg,
                        0f, 0f,
                        scale, data,
                        image.width.toInt(), image.height.toInt(),
                        image.width.toInt() * 4
                    )
                    NanoSVG.nsvgDeleteRasterizer(raster)
                    NanoSVG.nsvgDelete(svg)
                }
            }
            val img = nvgCreateImageRGBA(vg, image.width.toInt(), image.height.toInt(), 0, data)
            NVGImage(img, image.width, image.height, data).also { images[image] = it }
        }
    }

    override fun cleanup() {
        fbos.keys.clearUsing { delete(it) }
        images.keys.clearUsing { delete(it) }
        fonts.keys.clearUsing { delete(it) }
        nvgColor.free()
        nvgPaint.free()
        vg = -1
    }

    // used to ensure that the data is not discarded by the GC
    data class NVGImage(val id: Int, val width: Float, val height: Float, val data: ByteBuffer)
    data class NVGFont(val id: Int, val data: ByteBuffer)
}
