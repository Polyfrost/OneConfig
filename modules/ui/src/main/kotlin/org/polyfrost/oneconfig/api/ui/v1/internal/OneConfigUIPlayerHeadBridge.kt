package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
interface OneConfigUIPlayerHeadBridge {
    fun face(id: UUID): ImageBitmap?

    fun defaultFace(id: UUID): ImageBitmap?
}

@ExperimentalOneConfigUiApi
private val loadedHeadBridge: OneConfigUIPlayerHeadBridge? by lazy {
    loadBridge(OneConfigUIPlayerHeadBridge::class.java, "player heads fall back to a placeholder")
}

@ExperimentalOneConfigUiApi
internal fun playerHeadBridge(): OneConfigUIPlayerHeadBridge? = loadedHeadBridge
