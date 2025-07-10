package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.nanovg.NSVGImage
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL2
import org.lwjgl.nanovg.NanoVGGL3
import org.polyfrost.oneconfig.api.ui.v1.api.NanoVgApi
import java.nio.ByteBuffer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private typealias JBoolean = java.lang.Boolean

class NanoVgImpl(
    private val isOpenGl3: JBoolean
) : NanoVgApi {

    private companion object {

        // ByteBuffer.of("px\u0000")
        private val PIXELS = ByteBuffer.allocateDirect(3).put(112).put(120).put(0).flip() as ByteBuffer

    }

    object ConstantsImpl : NanoVgApi.Constants {

        override fun NVG_ROUND() = NVG_ROUND

        override fun NVG_ALIGN_LEFT() = NVG_ALIGN_LEFT

        override fun NVG_ALIGN_TOP() = NVG_ALIGN_TOP

        override fun NVG_HOLE() = NVG_HOLE

        override fun NVG_IMAGE_FLIPY() = NVG_IMAGE_FLIPY

    }

    private var handle: Long = -1L
    private var svgHandle: Long = -1L

    override fun constants() = ConstantsImpl

    override fun handle() = handle

    override fun svgHandle() = svgHandle

    override fun maybeSetup() {
        if (handle == -1L) {
            val handle = when (isOpenGl3.booleanValue()) {
                true -> NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS)
                false -> NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS)
            }

            if (handle == 0L /* NULL */) {
                throw IllegalStateException("Failed to create NanoVG context")
            }

            this.handle = handle
        }

        if (svgHandle == -1L) {
            val svgHandle = nsvgCreateRasterizer()
            if (svgHandle == 0L /* NULL */) {
                throw IllegalStateException("Failed to create NanoSVG context")
            }

            this.svgHandle = svgHandle
        }
    }

    override fun beginFrame(width: Float, height: Float, scale: Float) {
        nvgBeginFrame(handle, width, height, scale)
    }

    override fun endFrame() {
        nvgEndFrame(handle)
    }

    override fun globalAlpha(alpha: Float) {
        nvgGlobalAlpha(handle, alpha)
    }

    override fun translate(x: Float, y: Float) {
        nvgTranslate(handle, x, y)
    }

    override fun scale(x: Float, y: Float) {
        nvgScale(handle, x, y)
    }

    override fun rotate(angle: Float) {
        nvgRotate(handle, angle)
    }

    override fun skewX(angle: Float) {
        nvgSkewX(handle, angle)
    }

    override fun skewY(angle: Float) {
        nvgSkewY(handle, angle)
    }

    override fun save() {
        nvgSave(handle)
    }

    override fun restore() {
        nvgRestore(handle)
    }

    override fun createPaint() = NVGPaint.malloc().address()

    override fun fillPaint(address: Long) {
        nvgFillPaint(handle, NVGPaint.create(address))
    }

    override fun getPaintColor(address: Long) = NVGPaint.create(address).innerColor().address()

    override fun createColor() = NVGColor.malloc().address()

    override fun fillColor(address: Long) {
        nvgFillColor(handle, NVGColor.create(address))
    }

    override fun rgba(address: Long, rgba: Int) {
        nvgRGBA(
            (rgba shr 16 and 0xFF).toByte(),
            (rgba shr 8 and 0xFF).toByte(),
            (rgba and 0xFF).toByte(),
            (rgba shr 24 and 0xFF).toByte(),
            NVGColor.create(address)
        )
    }

    override fun beginPath() {
        nvgBeginPath(handle)
    }

    override fun pathWinding(winding: Int) {
        nvgPathWinding(handle, winding)
    }

    override fun fill() {
        nvgFill(handle)
    }

    override fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        nvgRoundedRect(handle, x, y, w, h, r)
    }

    override fun roundedRectVarying(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        tl: Float,
        tr: Float,
        br: Float,
        bl: Float
    ) {
        nvgRoundedRectVarying(handle, x, y, w, h, tl, tr, br, bl)
    }

    override fun lineJoin(join: Int) {
        nvgLineJoin(handle, join)
    }

    override fun lineCap(cap: Int) {
        nvgLineCap(handle, cap)
    }

    override fun stroke() {
        nvgStroke(handle)
    }

    override fun strokeWidth(width: Float) {
        nvgStrokeWidth(handle, width)
    }

    override fun strokePaint(address: Long) {
        nvgStrokePaint(handle, NVGPaint.create(address))
    }

    override fun strokeColor(address: Long) {
        nvgStrokeColor(handle, NVGColor.create(address))
    }

    override fun moveTo(x: Float, y: Float) {
        nvgMoveTo(handle, x, y)
    }

    override fun lineTo(x: Float, y: Float) {
        nvgLineTo(handle, x, y)
    }

    override fun createFont(name: String, buffer: ByteBuffer): Int {
        val clz = Class.forName("org.lwjgl.nanovg.NanoVG")
        try {
            val method = clz.getDeclaredMethod("nvgCreateFontMem", Long::class.java, CharSequence::class.java, ByteBuffer::class.java, Boolean::class.java)
            return method.invoke(null, handle, name, buffer, false) as Int
        } catch (e: NoSuchMethodException) {
            try {
                // This is required because some time between LWJGL 3.2.3 and 3.3.0, the signature of nvgCreateFontMem changed
                // from using an int to represent a bool to simply using a boolean. This is a workaround to make the code compatible
                // with both versions of LWJGL.
                val method = clz.getDeclaredMethod("nvgCreateFontMem", Long::class.java, CharSequence::class.java, ByteBuffer::class.java, Int::class.java)
                return method.invoke(null, handle, name, buffer, 0) as Int
            } catch (e: NoSuchMethodException) {
                // Just prints all of the methods to see what's available
                clz.declaredMethods.forEach { println(it) }

                throw IllegalStateException("Failed to find nvgCreateFontMem method")
            }
        }
    }

    override fun fontSize(size: Float) {
        nvgFontSize(handle, size)
    }

    override fun fontFaceId(id: Int) {
        nvgFontFaceId(handle, id)
    }

    override fun textAlign(align: Int) {
        nvgTextAlign(handle, align)
    }

    override fun text(x: Float, y: Float, text: String) {
        nvgText(handle, x, y, text)
    }

    override fun textBounds(x: Float, y: Float, text: String, bounds: FloatArray?): Float {
        return nvgTextBounds(handle, x, y, text, bounds)
    }

    override fun textMetrics(ascender: FloatArray?, descender: FloatArray?, lineh: FloatArray?) {
        nvgTextMetrics(handle, ascender, descender, lineh)
    }

    override fun createImage(width: Float, height: Float, buffer: ByteBuffer, flags: Int): Int {
        return nvgCreateImageRGBA(handle, width.toInt(), height.toInt(), flags, buffer)
    }

    override fun scissor(x: Float, y: Float, w: Float, h: Float) {
        nvgScissor(handle, x, y, w, h)
    }

    override fun intersectScissor(x: Float, y: Float, w: Float, h: Float) {
        nvgIntersectScissor(handle, x, y, w, h)
    }

    override fun resetScissor() {
        nvgResetScissor(handle)
    }

    override fun imagePattern(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        angle: Float,
        image: Int,
        alpha: Float,
        address: Long
    ) {
        nvgImagePattern(handle, x, y, w, h, angle, image, alpha, NVGPaint.create(address))
    }

    override fun linearGradient(address: Long, x0: Float, y0: Float, x1: Float, y1: Float, startColor: Long, endColor: Long) {
        nvgLinearGradient(handle, x0, y0, x1, y1, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
    }

    override fun radialGradient(address: Long, cx: Float, cy: Float, inr: Float, outr: Float, startColor: Long, endColor: Long) {
        nvgRadialGradient(handle, cx, cy, inr, outr, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
    }

    override fun boxGradient(
        address: Long,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        r: Float,
        f: Float,
        startColor: Long,
        endColor: Long
    ) {
        nvgBoxGradient(handle, x, y, w, h, r, f, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
    }

    override fun deleteImage(address: Int) {
        nvgDeleteImage(handle, address)
    }

    override fun parseSvg(data: ByteBuffer): NanoVgApi.SVG {
        val result = nsvgParse(data, PIXELS, 96f) ?: throw IllegalStateException("Failed to parse SVG data")
        return NanoVgApi.SVG(result.address(), result.width(), result.height())
    }

    override fun deleteSvg(address: Long) {
        nsvgDelete(NSVGImage.create(address))
    }

    override fun rasterizeSvg(
        address: Long,
        x: Float,
        y: Float,
        scale: Float,
        data: ByteBuffer,
        w: Int,
        h: Int,
        stride: Int
    ) {
        nsvgRasterize(svgHandle, NSVGImage.create(address), x, y, scale, data, w, h, stride)
    }

}
