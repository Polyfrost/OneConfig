package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.stb.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryUtil
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi
import java.nio.ByteBuffer

class StbImpl : StbApi {
    override fun image_load_from_memory(
        buffer: ByteBuffer,
        widthOutput: IntArray,
        heightOutput: IntArray,
        channelsOutput: IntArray,
        desiredChannels: Int
    ) = stbi_load_from_memory(buffer, widthOutput, heightOutput, channelsOutput, desiredChannels)
        ?: throw IllegalStateException("Failed to load image from memory")

    override fun image_failure_reason() = stbi_failure_reason()

    override fun image_free(image: ByteBuffer) {
        stbi_image_free(image)
    }

    override fun image_write_png(filename: String, w: Int, h: Int, comp: Int, data: ByteBuffer, strideInBytes: Int) {
        STBImageWrite.stbi_write_png(filename, w, h, comp, data, strideInBytes)
    }

    override fun font_CreateFontInfo() = STBTTFontinfo.malloc().address()

    override fun font_CreatePackRange() = STBTTPackRange.malloc().address()

    override fun font_CreatePackedCharArray(capacity: Int) = STBTTPackedchar.malloc(capacity).address()

    override fun font_CreatePackContext() = STBTTPackContext.malloc().address()

    override fun font_InitFont(info: Long, data: ByteBuffer) = nstbtt_InitFont(info, MemoryUtil.memAddress(data), 0) != 0

    override fun font_ScaleForMappingEmToPixels(info: Long, pixels: Float) =
        nstbtt_ScaleForMappingEmToPixels(info, pixels)

    override fun font_GetFontVMetrics(
        info: Long,
        ascent: IntArray,
        descent: IntArray,
        lineGap: IntArray
    ) {
        nstbtt_GetFontVMetrics(info, ascent, descent, lineGap)
    }

    override fun font_PackBegin(
        packCtx: Long,
        pixels: ByteBuffer,
        width: Int,
        height: Int,
        strideInBytes: Int,
        padding: Int,
        allocCtx: Long
    ) = nstbtt_PackBegin(
        packCtx,
        MemoryUtil.memAddress(pixels),
        width,
        height,
        strideInBytes,
        padding,
        allocCtx
    ) != 0

    override fun font_PackFontRange(
        packCtx: Long,
        fontData: ByteBuffer,
        fontIndex: Int,
        fontSize: Float,
        firstUnicodeCodepointInRange: Int,
        numCharsInRange: Int,
        packedCharArray: Long
    ) = nstbtt_PackFontRange(
        packCtx,
        MemoryUtil.memAddress(fontData),
        fontIndex,
        fontSize,
        firstUnicodeCodepointInRange,
        numCharsInRange,
        packedCharArray
    ) != 0

    override fun font_PackEnd(packCtx: Long) {
        nstbtt_PackEnd(packCtx)
    }

    override fun font_RangeSetFontSize(range: Long, size: Float) {
        STBTTPackRange.nfont_size(range, size)
    }

    override fun font_RangeSetFirstUnicodeCodepointInRange(
        range: Long,
        firstUnicodeCodepointInRange: Int
    ) {
        STBTTPackRange.nfirst_unicode_codepoint_in_range(range, firstUnicodeCodepointInRange)
    }

    override fun font_RangeSetNumChars(range: Long, numCharsInRange: Int) {
        STBTTPackRange.nnum_chars(range, numCharsInRange)
    }

    override fun font_RangeSetChardata(range: Long, packedCharArray: Long) {
        MemoryUtil.memPutAddress(range + STBTTPackRange.CHARDATA_FOR_RANGE, packedCharArray)
    }

    override fun free(struct: Long) {
        MemoryUtil.nmemFree(struct)
    }

    override fun font_GetPackedGlyph(packedCharArray: Long, index: Int) = packedCharArray + index * STBTTPackedchar.SIZEOF

    override fun glyph_x0(glyph: Long) = STBTTPackedchar.nx0(glyph)

    override fun glyph_y0(glyph: Long) = STBTTPackedchar.ny0(glyph)

    override fun glyph_x1(glyph: Long) = STBTTPackedchar.nx1(glyph)

    override fun glyph_y1(glyph: Long) = STBTTPackedchar.ny1(glyph)

    override fun glyph_xadvance(glyph: Long) = STBTTPackedchar.nxadvance(glyph)

    override fun glyph_xoff(glyph: Long) = STBTTPackedchar.nxoff(glyph)

    override fun glyph_yoff(glyph: Long) = STBTTPackedchar.nyoff(glyph)
}
