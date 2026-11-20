package org.polyfrost.compose.layout

import org.polyfrost.compose.render.PolyColor

public enum class LayoutType { Box, Row, Column }

public class PolyLayoutStyle {
    public var width: PolySize = PolySize.Wrap
    public var height: PolySize = PolySize.Wrap
    public var minWidth: Float = Float.NaN
    public var maxWidth: Float = Float.NaN
    public var minHeight: Float = Float.NaN
    public var maxHeight: Float = Float.NaN
    public var fitContent: Boolean = false
    public var align: PolyAlign = PolyAlign.TopLeft
    public var padding: PolyInsets = PolyInsets()
    public var margin: PolyInsets = PolyInsets()
    public var positionMode: PolyPositionMode = PolyPositionMode.Relative
    public var x: Float = 0f
    public var y: Float = 0f
    public var gap: Float = 0f
    public var layoutType: LayoutType = LayoutType.Box
    public var background: PolyColor = PolyColor.TRANSPARENT
    public var radius: Float = 0f
    public var borderColor: PolyColor = PolyColor.TRANSPARENT
    public var borderWidth: Float = 0f
    public var clipToBounds: Boolean = false
    public var alpha: Float = 1f
    public var fade: (() -> Float)? = null
    public var tag: Any? = null
}
