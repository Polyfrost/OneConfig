package org.polyfrost.compose.layout

import org.polyfrost.compose.render.PolyColor

enum class LayoutType { Box, Row, Column }

class PolyLayoutStyle {
    var width: PolySize = PolySize.Wrap
    var height: PolySize = PolySize.Wrap
    var align: PolyAlign = PolyAlign.TopLeft
    var padding: PolyInsets = PolyInsets()
    var margin: PolyInsets = PolyInsets()
    var positionMode: PolyPositionMode = PolyPositionMode.Relative
    var x: Float = 0f
    var y: Float = 0f
    var gap: Float = 0f
    var layoutType: LayoutType = LayoutType.Box
    var background: PolyColor = PolyColor.TRANSPARENT
    var radius: Float = 0f
    var borderColor: PolyColor = PolyColor.TRANSPARENT
    var borderWidth: Float = 0f
    var clipToBounds: Boolean = false
    var alpha: Float = 1f
}
