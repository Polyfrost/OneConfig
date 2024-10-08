package org.polyfrost.oneconfig.api.ui.v1.api

import java.nio.ByteBuffer

interface StbApi {

    fun loadFromMemory(buffer: ByteBuffer, widthOutput: IntArray, heightOutput: IntArray, channelsOutput: IntArray, desiredChannels: Int): ByteBuffer

}
