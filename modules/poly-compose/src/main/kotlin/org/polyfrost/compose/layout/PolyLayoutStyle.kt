package org.polyfrost.compose.layout

import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.draw.ContentDrawScope
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerIcon

enum class LayoutType { Box, Row, Column }

enum class PolyArrangement { Start, Center, End, SpaceBetween, SpaceEvenly, SpaceAround }

class PolyLayoutStyle {
    var width: PolySize = PolySize.Wrap
    var height: PolySize = PolySize.Wrap

    var align: Alignment? = null

    var crossBias: Float = Float.NaN
    var padding: PolyInsets = PolyInsets()
    var margin: PolyInsets = PolyInsets()
    var positionMode: PolyPositionMode = PolyPositionMode.Relative
    var x: Float = 0f
    var y: Float = 0f
    var gap: Float = 0f
    var layoutType: LayoutType = LayoutType.Box
    var contentAlignment: Alignment = Alignment.TopStart
    var background: PolyColor = PolyColor.TRANSPARENT
    var radius: Float = 0f

    var radiusPercent: Float = -1f
    var borderColor: PolyColor = PolyColor.TRANSPARENT

    var gradient: Brush? = null

    var shadows: List<Shadow> = emptyList()
    var borderWidth: Float = 0f

    var borderColorBottom: PolyColor? = null
    var clipToBounds: Boolean = false
    var alpha: Float = 1f

    var tag: Any? = null

    var minWidth: Float = 0f
    var maxWidth: Float = Float.POSITIVE_INFINITY
    var minHeight: Float = 0f
    var maxHeight: Float = Float.POSITIVE_INFINITY

    var weight: Float = 0f

    var weightFill: Boolean = true
    var arrangement: PolyArrangement = PolyArrangement.Start

    var crossAlignment: Float = -1f

    var onPointer: List<(PointerEvent) -> Boolean> = emptyList()

    var onKey: List<(KeyEvent) -> Boolean> = emptyList()

    var focusable: Boolean = false
    var onFocus: ((Boolean) -> Unit)? = null
    var onFocusChanged: List<(Boolean) -> Unit> = emptyList()

    var focusRequest: (() -> Boolean)? = null

    var inert: Boolean = false

    var onPlaced: ((PolyNode) -> Unit)? = null

    var onMeasured: ((PolyNode) -> Unit)? = null

    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var translateX: Float = 0f
    var translateY: Float = 0f

    var offsetX: Float = 0f
    var offsetY: Float = 0f

    var drawWithContent: (ContentDrawScope.() -> Unit)? = null

    var cursor: PointerIcon? = null

    internal var innerStarted = false
}
