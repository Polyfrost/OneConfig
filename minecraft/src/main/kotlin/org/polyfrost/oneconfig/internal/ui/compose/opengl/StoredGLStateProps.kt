package org.polyfrost.oneconfig.internal.ui.compose.opengl

import java.util.BitSet

class StoredGLStateProps {

    val lastActiveTexture = IntArray(1)
    val lastProgram = IntArray(1)
    val lastTexture = IntArray(1)
    val lastSampler = IntArray(1)
    val lastArrayBuffer = IntArray(1)
    val lastVertexArrayObject = IntArray(1)
    val lastPolygonMode = IntArray(2)
    val lastViewport = IntArray(4)
    val lastScissorBox = IntArray(4)
    val lastBlendSrcRgb = IntArray(1)
    val lastBlendDstRgb = IntArray(1)
    val lastBlendSrcAlpha = IntArray(1)
    val lastBlendDstAlpha = IntArray(1)
    val lastBlendEquationRgb = IntArray(1)
    val lastBlendEquationAlpha = IntArray(1)

    val lastPixelUnpackBufferBinding = IntArray(1)
    val lastUnpackAlignment = IntArray(1)
    val lastUnpackRowLength = IntArray(1)
    val lastUnpackSkipPixels = IntArray(1)
    val lastUnpackSkipRows = IntArray(1)
    val lastPackSwapBytes = IntArray(1)
    val lastPackLsbFirst = IntArray(1)
    val lastPackRowLength = IntArray(1)
    val lastPackImageHeight = IntArray(1)
    val lastPackSkipPixels = IntArray(1)
    val lastPackSkipRows = IntArray(1)
    val lastPackSkipImages = IntArray(1)
    val lastPackAlignment = IntArray(1)
    val lastUnpackSwapBytes = IntArray(1)
    val lastUnpackLsbFirst = IntArray(1)
    val lastUnpackImageHeight = IntArray(1)
    val lastUnpackSkipImages = IntArray(1)

    private val flags = BitSet(7)

    var lastEnableBlend
        get() = flags[0]
        set(value) {
            flags[0] = value
        }
    var lastEnableCullFace
        get() = flags[1]
        set(value) {
            flags[1] = value
        }
    var lastEnableDepthTest
        get() = flags[2]
        set(value) {
            flags[2] = value
        }
    var lastEnableStencilTest
        get() = flags[3]
        set(value) {
            flags[3] = value
        }
    var lastEnableScissorTest
        get() = flags[4]
        set(value) {
            flags[4] = value
        }
    var lastEnablePrimitiveRestart
        get() = flags[5]
        set(value) {
            flags[5] = value
        }

    var lastDepthMask
        get() = flags[6]
        set(value) {
            flags[6] = value
        }
}