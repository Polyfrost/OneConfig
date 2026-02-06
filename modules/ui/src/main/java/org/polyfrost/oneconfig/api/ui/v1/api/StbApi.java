package org.polyfrost.oneconfig.api.ui.v1.api;

import java.nio.ByteBuffer;

public interface StbApi {

    ByteBuffer image_load_from_memory(ByteBuffer buffer, int[] widthOutput, int[] heightOutput, int[] channelsOutput, int desiredChannels);

    String image_failure_reason();

    void image_free(ByteBuffer image);

    void image_write_png(String filename, int w, int h, int comp, ByteBuffer data, int strideInBytes);

    long font_CreateFontInfo();

    long font_CreatePackRange();

    long font_CreatePackedCharArray(int capacity);

    long font_CreatePackContext();

    boolean font_InitFont(long info, ByteBuffer data);

    int font_FindGlyphIndex(long info, int codepoint);

    float font_ScaleForMappingEmToPixels(long info, float pixels);

    void font_GetFontVMetrics(long info, int[] ascent, int[] descent, int[] lineGap);

    void font_GetGlyphHMetrics(long info, int glyphIndex, int[] advanceWidth, int[] leftSideBearing);

    long font_GetGlyphBitmap(long info, float scaleX, float scaleY, int glyphIndex, int[] w, int[] h, int[] x_off, int[] y_off);

    long font_GetGlyphSDF(long info, float scale, int glyphIndex, int padding, byte onEdgeValue, float pixelDistScale, int[] w, int[] h, int[] x_off, int[] y_off);

    boolean font_PackBegin(long packCtx, ByteBuffer pixels, int width, int height, int strideInBytes, int padding, long allocCtx);

    boolean font_PackFontRange(long packCtx, ByteBuffer fontData, int fontIndex, float fontSize, int firstUnicodeCodepointInRange, int numCharsInRange, long packedCharArray);

    void font_PackEnd(long packCtx);

    void font_RangeSetFontSize(long range, float size);

    void font_RangeSetFirstUnicodeCodepointInRange(long range, int firstUnicodeCodepointInRange);

    void font_RangeSetNumChars(long range, int numCharsInRange);

    void font_RangeSetChardata(long range, long packedCharArray);

    void free(long struct);

    long font_GetPackedGlyph(long packedCharArray, int index);

    short glyph_x0(long glyph);

    short glyph_y0(long glyph);

    short glyph_x1(long glyph);

    short glyph_y1(long glyph);

    float glyph_xadvance(long glyph);

    float glyph_xoff(long glyph);

    float glyph_yoff(long glyph);

}
