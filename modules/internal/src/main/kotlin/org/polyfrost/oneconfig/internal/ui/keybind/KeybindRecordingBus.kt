package org.polyfrost.oneconfig.internal.ui.keybind

object KeybindRecordingBus {
    @Volatile
    private var escapeHandler: (() -> Unit)? = null

    @Volatile
    private var mouseHandler: ((Int, Boolean) -> Unit)? = null

    fun setEscapeHandler(handler: () -> Unit) {
        escapeHandler = handler
    }

    fun clearEscapeHandler(handler: () -> Unit) {
        if (escapeHandler === handler) escapeHandler = null
    }

    fun setMouseHandler(handler: (Int, Boolean) -> Unit) {
        mouseHandler = handler
    }

    fun clearMouseHandler(handler: (Int, Boolean) -> Unit) {
        if (mouseHandler === handler) mouseHandler = null
    }

    @JvmStatic
    val isRecording get() = escapeHandler != null

    @JvmStatic
    fun consumeEscape(): Boolean {
        val handler = escapeHandler ?: return false
        handler()
        return true
    }

    @JvmStatic
    fun consumeMouse(button: Int, pressed: Boolean): Boolean {
        val handler = mouseHandler ?: return false
        handler(button, pressed)
        return true
    }
}
