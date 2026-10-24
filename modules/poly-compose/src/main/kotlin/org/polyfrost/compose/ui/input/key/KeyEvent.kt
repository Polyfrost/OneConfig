package org.polyfrost.compose.ui.input.key

import org.polyfrost.compose.node.PolyNode

enum class KeyEventType { KeyDown, KeyUp, Char, Preedit }

class KeyEvent(val type: KeyEventType, val key: Int, val modifiers: Int, val codepoint: Int = 0, val text: String = "") {
    lateinit var node: PolyNode
        internal set

    val shift get() = modifiers and SHIFT != 0
    val ctrl get() = modifiers and CONTROL != 0
    val alt get() = modifiers and ALT != 0

    val shortcut get() = modifiers and (if (MAC) SUPER else CONTROL) != 0

    val word get() = modifiers and (if (MAC) ALT else CONTROL) != 0

    companion object {
        @JvmStatic var SHIFT = 1
        @JvmStatic var CONTROL = 2
        @JvmStatic var ALT = 4
        @JvmStatic var SUPER = 8

        @JvmStatic var KEY_TAB = 258
        @JvmStatic var KEY_ESCAPE = 256
        @JvmStatic var KEY_ENTER = 257
        @JvmStatic var KEY_BACKSPACE = 259
        @JvmStatic var KEY_DELETE = 261
        @JvmStatic var KEY_RIGHT = 262
        @JvmStatic var KEY_LEFT = 263
        @JvmStatic var KEY_DOWN = 264
        @JvmStatic var KEY_UP = 265
        @JvmStatic var KEY_PAGE_UP = 266
        @JvmStatic var KEY_PAGE_DOWN = 267
        @JvmStatic var KEY_HOME = 268
        @JvmStatic var KEY_END = 269
        @JvmStatic var KEY_A = 65
        @JvmStatic var KEY_C = 67
        @JvmStatic var KEY_V = 86
        @JvmStatic var KEY_X = 88
        @JvmStatic var KEY_Y = 89
        @JvmStatic var KEY_Z = 90

        @JvmField
        val MAC = System.getProperty("os.name", "").lowercase().contains("mac")
    }
}
