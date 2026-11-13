package org.polyfrost.oneconfig.api.ui.v1.internal

import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus

class OneConfigUIKeybindRecordingBridgeImpl : OneConfigUIKeybindRecordingBridge {
    override fun startRecording(onEscape: () -> Unit, onMouse: (button: Int, pressed: Boolean) -> Unit) {
        KeybindRecordingBus.setEscapeHandler(onEscape)
        KeybindRecordingBus.setMouseHandler(onMouse)
    }

    override fun stopRecording(onEscape: () -> Unit, onMouse: (button: Int, pressed: Boolean) -> Unit) {
        KeybindRecordingBus.clearEscapeHandler(onEscape)
        KeybindRecordingBus.clearMouseHandler(onMouse)
    }
}
