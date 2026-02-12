package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.nanovg.NanoSVG.*
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.NanoSvgApi
import java.nio.ByteBuffer

class NanoSvgImpl : NanoSvgApi {
    private val PIXELS = ByteBuffer.allocateDirect(3).put(112).put(120).put(0).flip() as ByteBuffer

    override fun parse(data: ByteBuffer): NanoSvgApi.SVG {
        val result = nsvgParse(data, PIXELS, 96f) ?: throw IllegalStateException("Failed to parse SVG data")
        return NanoSvgApi.SVG(result.address(), result.width(), result.height())
    }

    override fun delete(address: Long) {
        nnsvgDelete(address)
    }

    override fun rasterize(
        address: Long,
        x: Float,
        y: Float,
        scale: Float,
        data: ByteBuffer,
        w: Int,
        h: Int,
        stride: Int
    ) {
        nnsvgRasterize(handle, address, x, y, scale, Platform.gl().memAddress(data), w, h, stride)
    }

    private var handle: Long = -1L
        get() {
            if (field == -1L) {
                val handle = nsvgCreateRasterizer()
                if (handle == 0L /* NULL */) {
                    throw IllegalStateException("Failed to create NanoSVG context")
                }

                field = handle
                return handle
            }
            return field
        }
}