package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.stb.STBImage
import org.polyfrost.oneconfig.api.ui.v1.api.StbApi
import java.nio.ByteBuffer

class StbImpl : StbApi {

    override fun loadFromMemory(
        buffer: ByteBuffer,
        widthOutput: IntArray,
        heightOutput: IntArray,
        channelsOutput: IntArray,
        desiredChannels: Int
    ): ByteBuffer {
        return STBImage.stbi_load_from_memory(buffer, widthOutput, heightOutput, channelsOutput, desiredChannels) ?: throw IllegalStateException("Failed to load image from memory")
    }

}
