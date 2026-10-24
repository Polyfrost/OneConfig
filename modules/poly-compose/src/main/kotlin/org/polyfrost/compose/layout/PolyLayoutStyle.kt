package org.polyfrost.compose.layout

import androidx.compose.runtime.State
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFade
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.graphics.Brush
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

    var animatedAlpha: State<Float>? = null

    var animatedBackground: State<PolyColor>? = null
    var animatedBorder: State<PolyColor>? = null

    var animatedOffsetX: State<Float>? = null
    var offsetXTarget: Float = 0f
    val drawnOffsetX get() = animatedOffsetX.let { if (it == null) 0f else it.value - offsetXTarget }

    val drawnBackground get() = animatedBackground?.value ?: background
    val drawnBorderColor get() = animatedBorder?.value ?: borderColor

    val animating get() = animatedAlpha.let { it != null && it.value != alpha } ||
        animatedBackground.let { it != null && it.value != background } ||
        animatedBorder.let { it != null && it.value != borderColor } ||
        animatedOffsetX.let { it != null && it.value != offsetXTarget }
    var tag: Any? = null

    var minWidth: Float = 0f
    var maxWidth: Float = Float.POSITIVE_INFINITY
    var minHeight: Float = 0f
    var maxHeight: Float = Float.POSITIVE_INFINITY

    var weight: Float = 0f

    var weightFill: Boolean = true
    var arrangement: PolyArrangement = PolyArrangement.Start

    var crossAlignment: Float = -1f

    var scroll: ScrollState? = null

    var scrollHorizontal: Boolean = false

    var onPointer: List<(PointerEvent) -> Boolean> = emptyList()

    var onKey: List<(KeyEvent) -> Boolean> = emptyList()

    var focusable: Boolean = false
    var onFocus: ((Boolean) -> Unit)? = null

    var focusRequest: (() -> Boolean)? = null

    var inert: Boolean = false

    var overlay: Boolean = false

    var overlayAnchor: Alignment = Alignment.TopStart

    var overlayRoomier: Boolean = false

    var onPlaced: ((PolyNode) -> Unit)? = null

    var scaleX: Float = 1f
    var scaleY: Float = 1f
    var translateX: Float = 0f
    var translateY: Float = 0f

    var offsetX: Float = 0f
    var offsetY: Float = 0f

    var fade: ((PolyNode) -> PolyFade?)? = null

    var cursor: PointerIcon? = null

    internal var innerStarted = false
}
