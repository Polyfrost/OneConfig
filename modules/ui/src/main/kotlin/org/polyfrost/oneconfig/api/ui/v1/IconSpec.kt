@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1

@ExperimentalOneConfigUiApi
public class IconSpec @JvmOverloads public constructor(
    public val path: String,
    public val x: Int,
    public val y: Int,
    public val width: Int,
    public val height: Int = width,
    public val sheetWidth: Int = 0,
    public val sheetHeight: Int = sheetWidth,
    public val pixelated: Boolean = true,
) {
    init {
        require(width > 0 && height > 0) { "IconSpec cell size must be positive, got ${width}x$height" }
        require(x >= 0 && y >= 0) { "IconSpec cell position must not be negative, got $x,$y" }
        require(sheetWidth >= 0 && sheetHeight >= 0) { "IconSpec sheet size must not be negative" }
    }

    override fun equals(other: Any?): Boolean = other is IconSpec && other.path == path && other.x == x &&
        other.y == y && other.width == width && other.height == height && other.sheetWidth == sheetWidth &&
        other.sheetHeight == sheetHeight && other.pixelated == pixelated

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + sheetWidth
        result = 31 * result + sheetHeight
        result = 31 * result + pixelated.hashCode()
        return result
    }

    override fun toString(): String = "IconSpec($path, $x, $y, ${width}x$height)"

    public companion object {
        @JvmStatic
        @JvmOverloads
        public fun grid(path: String, column: Int, row: Int, cellSize: Int, sheetSize: Int = 0): IconSpec =
            IconSpec(path, column * cellSize, row * cellSize, cellSize, cellSize, sheetSize, sheetSize)
    }
}
