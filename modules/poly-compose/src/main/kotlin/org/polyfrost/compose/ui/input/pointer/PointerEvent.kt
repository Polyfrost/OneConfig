package org.polyfrost.compose.ui.input.pointer

import org.polyfrost.compose.node.PolyNode

enum class PointerEventType { Enter, Exit, Move, Press, Release, Scroll, Outside }

enum class PointerIcon { Default, Hand, Text }

class PointerEvent(
    val type: PointerEventType,
    val x: Float,
    val y: Float,
    val button: Int = 0,
    val scroll: Float = 0f,
    val inside: Boolean = true,
    val clicks: Int = 1,
    val modifiers: Int = 0,
) {
    lateinit var node: PolyNode
        internal set
}
