package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.State
import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIPlayerHeadBridge {
    public fun face(id: UUID): ImageBitmap?

    public fun observe(id: UUID): State<ImageBitmap?>

    public fun defaultFace(id: UUID): ImageBitmap?

    public fun bitmap(id: UUID): PolyBitmap?
}

@ExperimentalOneConfigUiApi
private val loadedHeadBridge: OneConfigUIPlayerHeadBridge? by lazy {
    loadBridge(OneConfigUIPlayerHeadBridge::class.java, "player heads fall back to a placeholder")
}

@ExperimentalOneConfigUiApi
internal fun playerHeadBridge(): OneConfigUIPlayerHeadBridge? = loadedHeadBridge
