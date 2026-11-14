package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi

@ExperimentalOneConfigUiApi
@InternalOneConfigUiApi
public interface OneConfigUIKeybindRecordingBridge {
    public fun startRecording(onEscape: () -> Unit, onMouse: (button: Int, pressed: Boolean) -> Unit)

    public fun stopRecording(onEscape: () -> Unit, onMouse: (button: Int, pressed: Boolean) -> Unit)

    public val isRecording: Boolean
}

@ExperimentalOneConfigUiApi
private val loadedKeybindRecordingBridge: OneConfigUIKeybindRecordingBridge? by lazy {
    loadBridge(OneConfigUIKeybindRecordingBridge::class.java, "keybind fields cannot record mouse buttons and Escape may close the screen while recording")
}

@ExperimentalOneConfigUiApi
internal fun keybindRecordingBridge(): OneConfigUIKeybindRecordingBridge? = loadedKeybindRecordingBridge
