package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.nanovg.NSVGImage
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG
import org.lwjgl.nanovg.NanoVG
import org.lwjgl.nanovg.NanoVGGL2
import org.lwjgl.nanovg.NanoVGGL3
import org.lwjgl.system.MemoryUtil
import org.polyfrost.oneconfig.api.ui.v1.api.NanoVgApi
import java.nio.ByteBuffer

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private typealias JBoolean = java.lang.Boolean

class NanoVgImpl(
    private val isOpenGl3: JBoolean
) : NanoVgApi {


    private companion object {

        // ByteBuffer.of("px\u0000")
        private val PIXELS: ByteBuffer = MemoryUtil.memAlloc(3).put(112).put(120).put(0).flip() as ByteBuffer

    }

    object NanoVgConstantsImpl : NanoVgApi.NanoVgConstants {

        override fun NVG_ROUND(): Int {
            return NanoVG.NVG_ROUND
        }

        override fun NVG_ALIGN_LEFT(): Int {
            return NanoVG.NVG_ALIGN_LEFT
        }

        override fun NVG_ALIGN_TOP(): Int {
            return NanoVG.NVG_ALIGN_TOP
        }

        override fun NVG_HOLE(): Int {
            return NanoVG.NVG_HOLE
        }

    }

    private var handle: Long = -1
    private var svgHandle: Long = -1

    override fun constants(): NanoVgApi.NanoVgConstants {
        return NanoVgConstantsImpl
    }

    override fun handle(): Long {
        return handle
    }

    override fun svgHandle(): Long {
        return svgHandle
    }

    override fun maybeSetup() {
        if (handle == -1L) {
            val handle = when (isOpenGl3.booleanValue()) {
                true -> NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS)
                false -> NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS)
            }

            if (handle == MemoryUtil.NULL) {
                throw IllegalStateException("Failed to create NanoVG context")
            }

            this.handle = handle
            println("Created NVG context: $handle")
        }

        if (svgHandle == -1L) {
            val svgHandle = NanoSVG.nsvgCreateRasterizer()
            if (svgHandle == MemoryUtil.NULL) {
                throw IllegalStateException("Failed to create NanoSVG context")
            }

            this.svgHandle = svgHandle
            println("Created NSVG rasterizer: $svgHandle")
        }
    }

    override fun beginFrame(width: Float, height: Float, scale: Float) {
        NanoVG.nvgBeginFrame(handle, width, height, scale)
    }

    override fun endFrame() {
        NanoVG.nvgEndFrame(handle)
    }

    override fun globalAlpha(alpha: Float) {
        NanoVG.nvgGlobalAlpha(handle, alpha)
    }

    override fun translate(x: Float, y: Float) {
        NanoVG.nvgTranslate(handle, x, y)
    }

    override fun scale(x: Float, y: Float) {
        NanoVG.nvgScale(handle, x, y)
    }

    override fun rotate(angle: Float) {
        NanoVG.nvgRotate(handle, angle)
    }

    override fun skewX(angle: Float) {
        NanoVG.nvgSkewX(handle, angle)
    }

    override fun skewY(angle: Float) {
        NanoVG.nvgSkewY(handle, angle)
    }

    override fun save() {
        NanoVG.nvgSave(handle)
    }

    override fun restore() {
        NanoVG.nvgRestore(handle)
    }

    override fun createPaint(): Long {
        return NVGPaint.malloc().address()
    }

    override fun fillPaint(address: Long) {
        NanoVG.nvgFillPaint(handle, NVGPaint.create(address))
    }

    override fun getPaintColor(address: Long): Long {
        return NVGPaint.create(address).innerColor().address()
    }

    override fun createColor(): Long {
        return NVGColor.malloc().address()
    }

    override fun fillColor(address: Long) {
        NanoVG.nvgFillColor(handle, NVGColor.create(address))
    }

    override fun rgba(address: Long, rgba: Int) {
        NanoVG.nvgRGBA(
            (rgba shr 16 and 0xFF).toByte(),
            (rgba shr 8 and 0xFF).toByte(),
            (rgba and 0xFF).toByte(),
            (rgba shr 24 and 0xFF).toByte(),
            NVGColor.create(address)
        )
    }

    override fun beginPath() {
        NanoVG.nvgBeginPath(handle)
    }

    override fun pathWinding(winding: Int) {
        NanoVG.nvgPathWinding(handle, winding)
    }

    override fun fill() {
        NanoVG.nvgFill(handle)
    }

    override fun roundedRect(x: Float, y: Float, w: Float, h: Float, r: Float) {
        NanoVG.nvgRoundedRect(handle, x, y, w, h, r)
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
        NanoVG.nvgRoundedRectVarying(handle, x, y, w, h, tl, tr, br, bl)
    }

    override fun lineJoin(join: Int) {
        NanoVG.nvgLineJoin(handle, join)
    }

    override fun lineCap(cap: Int) {
        NanoVG.nvgLineCap(handle, cap)
    }

    override fun stroke() {
        NanoVG.nvgStroke(handle)
    }

    override fun strokeWidth(width: Float) {
        NanoVG.nvgStrokeWidth(handle, width)
    }

    override fun strokePaint(address: Long) {
        NanoVG.nvgStrokePaint(handle, NVGPaint.create(address))
    }

    override fun strokeColor(address: Long) {
        NanoVG.nvgStrokeColor(handle, NVGColor.create(address))
    }

    override fun moveTo(x: Float, y: Float) {
        NanoVG.nvgMoveTo(handle, x, y)
    }

    override fun lineTo(x: Float, y: Float) {
        NanoVG.nvgLineTo(handle, x, y)
    }

    override fun createFont(name: String, buffer: ByteBuffer): Int {
        return NanoVG.nvgCreateFontMem(handle, name, buffer, false)
    }

    override fun fontSize(size: Float) {
//        println("> [$handle] Setting font size: $size")
        NanoVG.nvgFontSize(handle, size)
    }

    override fun fontFaceId(id: Int) {
//        println("> [$handle] Setting font face ID: $id")
        NanoVG.nvgFontFaceId(handle, id)
    }

    override fun textAlign(align: Int) {
//        println("> [$handle] Setting text align: $align")
        NanoVG.nvgTextAlign(handle, align)
    }

    override fun text(x: Float, y: Float, text: String) {
        NanoVG.nvgText(handle, x, y, text)
    }

    override fun textBounds(x: Float, y: Float, text: String, bounds: FloatArray): Float {
//        println("> [$handle] Getting text bounds for: $text")
        return NanoVG.nvgTextBounds(handle, x, y, text, bounds)
    }

    override fun createImage(width: Float, height: Float, buffer: ByteBuffer): Int {
        return NanoVG.nvgCreateImageRGBA(handle, width.toInt(), height.toInt(), NanoVG.NVG_IMAGE_FLIPY, buffer)
    }

    override fun scissor(x: Float, y: Float, w: Float, h: Float) {
        NanoVG.nvgScissor(handle, x, y, w, h)
    }

    override fun intersectScissor(x: Float, y: Float, w: Float, h: Float) {
        NanoVG.nvgIntersectScissor(handle, x, y, w, h)
    }

    override fun resetScissor() {
        NanoVG.nvgResetScissor(handle)
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
        NanoVG.nvgImagePattern(handle, x, y, w, h, angle, image, alpha, NVGPaint.create(address))
    }

    override fun linearGradient(address: Long, x0: Float, y0: Float, x1: Float, y1: Float, startColor: Long, endColor: Long) {
        NanoVG.nvgLinearGradient(handle, x0, y0, x1, y1, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
    }

    override fun radialGradient(address: Long, cx: Float, cy: Float, inr: Float, outr: Float, startColor: Long, endColor: Long) {
        NanoVG.nvgRadialGradient(handle, cx, cy, inr, outr, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
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
        NanoVG.nvgBoxGradient(handle, x, y, w, h, r, f, NVGColor.create(startColor), NVGColor.create(endColor), NVGPaint.create(address))
    }

    override fun deleteImage(address: Int) {
        NanoVG.nvgDeleteImage(handle, address)
    }

    override fun svgBounds(address: Long): FloatArray {
        val svg = NSVGImage.create(address)
        return floatArrayOf(svg.width(), svg.height())
    }

    override fun parseSvg(data: ByteBuffer): Triple<Long, Float, Float> {
        println("> [$handle - $svgHandle] Parsing SVG data")
        val result = NanoSVG.nsvgParse(data, PIXELS, 96f) ?: throw IllegalStateException("Failed to parse SVG data")
        println("> [$handle - $svgHandle] Parsed SVG data: ${result.address()} - ${result.width()}x${result.height()}")
        return Triple(result.address(), result.width(), result.height())
    }

    override fun deleteSvg(address: Long) {
        println("> [$handle - $svgHandle] Deleting SVG data: $address")
        NanoSVG.nsvgDelete(NSVGImage.create(address))
    }

    override fun rasterizeSvg(
        address: Long,
        x: Float,
        y: Float,
        w: Int,
        h: Int,
        scale: Float,
        stride: Int,
        data: ByteBuffer
    ) {
        println("> [$handle - $svgHandle] Rasterizing SVG data: $x, $y, $w, $h, $scale, $stride")
        NanoSVG.nsvgRasterize(svgHandle, NSVGImage.create(address), x, y, scale, data, w, h, stride)
    }

}
