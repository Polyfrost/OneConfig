package org.polyfrost.oneconfig.internal.ui.keybind

object KeybindRecordingBus {
    @Volatile
    private var escapeHandler: (() -> Unit)? = null

    fun setEscapeHandler(handler: () -> Unit) {
        escapeHandler = handler
    }

    fun clearEscapeHandler(handler: () -> Unit) {
        if (escapeHandler === handler) escapeHandler = null
    }

    @JvmStatic
    val isRecording get() = escapeHandler != null

    @JvmStatic
    fun consumeEscape(): Boolean {
        val handler = escapeHandler ?: return false
        handler()
        return true
    }
}
