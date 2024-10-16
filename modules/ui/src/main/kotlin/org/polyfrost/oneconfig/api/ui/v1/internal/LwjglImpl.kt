package org.polyfrost.oneconfig.api.ui.v1.internal

import org.lwjgl.system.MemoryUtil
import org.polyfrost.oneconfig.api.ui.v1.api.LwjglApi
import java.nio.ByteBuffer

class LwjglImpl : LwjglApi {

    override fun memAlloc(size: Int): ByteBuffer {
        return MemoryUtil.memAlloc(size)
    }

}
