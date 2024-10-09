package org.polyfrost.oneconfig.api.ui.v1.api;

import kotlin.Triple;

import java.nio.ByteBuffer;

public interface NanoVgApi {

    interface NanoVgConstants {

        int NVG_ROUND();

        int NVG_ALIGN_LEFT();

        int NVG_ALIGN_TOP();

        int NVG_HOLE();

    }

    NanoVgConstants constants();

    long handle();
    long svgHandle();

    /**
     * If this instance is not set up already, it will set up the instance (initializes it's NVG and NSVG context).
     *
     * @throws IllegalStateException if the instance failed to set up.
     *
     * @since 0.2.0
     * @author Deftu
     */
    void maybeSetup();

    /**
     * Starts a new frame.
     */
    void beginFrame(float width, float height, float scale);

    /**
     * Ends the current frame.
     */
    void endFrame();

    void globalAlpha(float alpha);

    void translate(float x, float y);

    void scale(float x, float y);

    void rotate(float angle);

    void skewX(float angle);

    void skewY(float angle);

    void save();

    void restore();

    /**
     * Creates a new NanoVG paint and returns the address.
     */
    long createPaint();

    /**
     * Fills the paint with the item at the address.
     */
    void fillPaint(long address);

    long getPaintColor(long address);

    long createColor();

    void fillColor(long address);

    void rgba(long address, int rgba);

    void beginPath();

    void pathWinding(int winding);

    void fill();

    void roundedRect(float x, float y, float w, float h, float r);

    void roundedRectVarying(float x, float y, float w, float h, float tl, float tr, float br, float bl);

    void lineJoin(int join);

    void lineCap(int cap);

    void stroke();

    void strokeWidth(float width);

    void strokePaint(long address);

    void strokeColor(long address);

    void moveTo(float x, float y);

    void lineTo(float x, float y);

    int createFont(String name, ByteBuffer buffer);

    void fontSize(float size);

    void fontFaceId(int id);

    void textAlign(int align);

    void text(float x, float y, String text);

    float textBounds(float x, float y, String text, float[] bounds);

    int createImage(float width, float height, ByteBuffer buffer);

    void scissor(float x, float y, float w, float h);

    void intersectScissor(float x, float y, float w, float h);

    void resetScissor();

    void imagePattern(float x, float y, float w, float h, float angle, int image, float alpha, long address);

    void linearGradient(long address, float x0, float y0, float x1, float y1, long startColor, long endColor);

    void radialGradient(long address, float cx, float cy, float inr, float outr, long startColor, long endColor);

    void boxGradient(long address, float x, float y, float w, float h, float r, float f, long startColor, long endColor);

    void deleteImage(int address);

    float[] svgBounds(long address);

    Triple<Long, Float, Float> parseSvg(ByteBuffer data);

    void deleteSvg(long address);

    void rasterizeSvg(long address, float x, float y, int w, int h, float scale, int stride, ByteBuffer data);

}
