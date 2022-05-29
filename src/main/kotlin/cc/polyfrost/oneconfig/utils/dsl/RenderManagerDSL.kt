package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.config.data.InfoType
import cc.polyfrost.oneconfig.lwjgl.RenderManager
import cc.polyfrost.oneconfig.lwjgl.font.Font
import cc.polyfrost.oneconfig.lwjgl.image.Images
import cc.polyfrost.oneconfig.lwjgl.image.SVGs
import cc.polyfrost.oneconfig.utils.ColorUtils
import org.lwjgl.nanovg.NVGColor

/**
 * Wrapper for a NanoVG instance.
 * @see nanoVG
 * @see RenderManager
 */
data class VG(val instance: Long)

/**
 * Sets up rendering, calls the block with the NanoVG instance, and then cleans up.
 *
 * To start, call this method.
 * ```kt
 * nanoVG {
 *     // Do stuff with the NanoVG instance
 * }
 * ```
 * From there, you can use the passed [VG] object to draw things. For example...
 * ```kt
 * nanoVG {
 *     drawRect(1, 1, 100, 100, Color.RED.rgb)
 *     drawText("Hello, world!", 10, 10, Color.BLACK.rgb, 9, Fonts.BOLD)
 * }
 * ```
 * You can also set the [mcScaling] parameter to true to scale the NanoVG instance to match the Minecraft GUI scale.
 *
 * @param mcScaling Whether to scale the NanoVG instance to match the Minecraft GUI scale.
 * @param block The block to run.
 */
fun nanoVG(mcScaling: Boolean = false, block: VG.() -> Unit) = RenderManager.setupAndDraw(mcScaling) {
    block.invoke(
        VG(it)
    )
}


fun Long.drawRect(x: Number, y: Number, width: Number, height: Number, color: Int, bypassOneConfig: Boolean = false) =
    if (bypassOneConfig) {
        RenderManager.drawRect(this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)
    } else {
        RenderManager.drawRect(this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)
    }

fun VG.drawRect(x: Number, y: Number, width: Number, height: Number, color: Int, bypassOneConfig: Boolean = false) =
    instance.drawRect(x, y, width, height, color, bypassOneConfig)

fun Long.drawRoundedRect(x: Number, y: Number, width: Number, height: Number, radius: Number, color: Int) =
    RenderManager.drawRoundedRect(
        this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color, radius.toFloat()
    )

fun VG.drawRoundedRect(x: Number, y: Number, width: Number, height: Number, radius: Number, color: Int) =
    instance.drawRoundedRect(x, y, width, height, radius, color)

fun Long.drawHollowRoundedRect(
    x: Number, y: Number, width: Number, height: Number, radius: Number, color: Int, thickness: Number
) = RenderManager.drawHollowRoundRect(
    this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color, radius.toFloat(), thickness.toFloat()
)

fun VG.drawHollowRoundedRect(
    x: Number, y: Number, width: Number, height: Number, radius: Number, color: Int, thickness: Number
) = instance.drawHollowRoundedRect(x, y, width, height, radius, color, thickness)

fun Long.drawRoundedRectVaried(
    x: Number,
    y: Number,
    width: Number,
    height: Number,
    color: Int,
    radiusTL: Number,
    radiusTR: Number,
    radiusBR: Number,
    radiusBL: Number
) = RenderManager.drawRoundedRectVaried(
    this,
    x.toFloat(),
    y.toFloat(),
    width.toFloat(),
    height.toFloat(),
    color,
    radiusTL.toFloat(),
    radiusTR.toFloat(),
    radiusBR.toFloat(),
    radiusBL.toFloat()
)

fun VG.drawRoundedRectVaried(
    x: Number,
    y: Number,
    width: Number,
    height: Number,
    color: Int,
    radiusTL: Number,
    radiusTR: Number,
    radiusBR: Number,
    radiusBL: Number
) = instance.drawRoundedRectVaried(x, y, width, height, color, radiusTL, radiusTR, radiusBR, radiusBL)

fun Long.drawGradientRect(x: Number, y: Number, width: Number, height: Number, color1: Int, color2: Int) =
    RenderManager.drawGradientRect(this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color1, color2)

fun VG.drawGradientRect(x: Number, y: Number, width: Number, height: Number, color1: Int, color2: Int) =
    instance.drawGradientRect(x, y, width, height, color1, color2)

fun Long.drawGradientRoundedRect(
    x: Number, y: Number, width: Number, height: Number, color: Int, color2: Int, radius: Number
) = RenderManager.drawGradientRoundedRect(
    this, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color, color2, radius.toFloat()
)

fun VG.drawGradientRoundedRect(
    x: Number, y: Number, width: Number, height: Number, color: Int, color2: Int, radius: Number
) = instance.drawGradientRoundedRect(x, y, width, height, color, color2, radius)


fun Long.drawCircle(x: Number, y: Number, radius: Number, color: Int) =
    RenderManager.drawCircle(this, x.toFloat(), y.toFloat(), radius.toFloat(), color)

fun VG.drawCircle(x: Number, y: Number, radius: Number, color: Int) = instance.drawCircle(x, y, radius, color)


fun Long.drawText(text: String, x: Number, y: Number, color: Int, size: Number, font: Font) =
    RenderManager.drawText(this, text, x.toFloat(), y.toFloat(), color, size.toFloat(), font)

fun VG.drawText(text: String, x: Number, y: Number, color: Int, size: Number, font: Font) =
    instance.drawText(text, x, y, color, size, font)

fun Long.drawWrappedString(text: String, x: Number, y: Number, width: Number, color: Int, size: Number, font: Font) =
    RenderManager.drawWrappedString(this, text, x.toFloat(), y.toFloat(), width.toFloat(), color, size.toFloat(), font)

fun VG.drawWrappedString(text: String, x: Number, y: Number, width: Number, color: Int, size: Number, font: Font) =
    instance.drawWrappedString(text, x, y, width, color, size, font)

fun Long.drawURL(url: String, x: Number, y: Number, size: Number, font: Font) =
    RenderManager.drawURL(this, url, x.toFloat(), y.toFloat(), size.toFloat(), font)

fun VG.drawURL(url: String, x: Number, y: Number, size: Number, font: Font) = instance.drawURL(url, x, y, size, font)


fun Long.drawImage(filePath: String, x: Number, y: Number, width: Number, height: Number) =
    RenderManager.drawImage(this, filePath, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

fun VG.drawImage(filePath: String, x: Number, y: Number, width: Number, height: Number) =
    instance.drawImage(filePath, x, y, width, height)

fun Long.drawImage(filePath: String, x: Number, y: Number, width: Number, height: Number, color: Int) =
    RenderManager.drawImage(this, filePath, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)

fun VG.drawImage(filePath: String, x: Number, y: Number, width: Number, height: Number, color: Int) =
    instance.drawImage(filePath, x, y, width, height, color)

fun Long.drawImage(image: Images, x: Number, y: Number, width: Number, height: Number) =
    RenderManager.drawImage(this, image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

fun VG.drawImage(image: Images, x: Number, y: Number, width: Number, height: Number) =
    instance.drawImage(image, x, y, width, height)

fun Long.drawImage(image: Images, x: Number, y: Number, width: Number, height: Number, color: Int) =
    RenderManager.drawImage(this, image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)

fun VG.drawImage(image: Images, x: Number, y: Number, width: Number, height: Number, color: Int) =
    instance.drawImage(image, x, y, width, height, color)


fun Long.drawRoundedImage(filePath: String, x: Number, y: Number, width: Number, height: Number, radius: Number) =
    RenderManager.drawRoundImage(
        this, filePath, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius.toFloat()
    )

fun VG.drawRoundedImage(filePath: String, x: Number, y: Number, width: Number, height: Number, radius: Number) =
    instance.drawRoundedImage(filePath, x, y, width, height, radius)

fun Long.drawRoundedImage(image: Images, x: Number, y: Number, width: Number, height: Number, radius: Number) =
    RenderManager.drawRoundImage(
        this, image, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), radius.toFloat()
    )

fun VG.drawRoundedImage(image: Images, x: Number, y: Number, width: Number, height: Number, radius: Number) =
    instance.drawRoundedImage(image, x, y, width, height, radius)


fun Long.getTextWidth(text: String, size: Number, font: Font) =
    RenderManager.getTextWidth(this, text, size.toFloat(), font)

fun VG.getTextWidth(text: String, size: Number, font: Font) = instance.getTextWidth(text, size, font)


fun Long.drawLine(x1: Number, y1: Number, x2: Number, y2: Number, width: Number, color: Int) =
    RenderManager.drawLine(this, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), width.toFloat(), color)

fun VG.drawLine(x1: Number, y1: Number, x2: Number, y2: Number, width: Number, color: Int) =
    instance.drawLine(x1, y1, x2, y2, width, color)


fun Long.drawDropShadow(
    x: Number, y: Number, w: Number, h: Number, blur: Number, spread: Number, cornerRadius: Number
) = RenderManager.drawDropShadow(
    this, x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), blur.toFloat(), spread.toFloat(), cornerRadius.toFloat()
)

fun VG.drawDropShadow(x: Number, y: Number, w: Number, h: Number, blur: Number, spread: Number, cornerRadius: Number) =
    instance.drawDropShadow(x, y, w, h, blur, spread, cornerRadius)


fun Long.newColor(r: Int, g: Int, b: Int, a: Int) = RenderManager.color(this, ColorUtils.getColor(r, g, b, a))
fun VG.newColor(r: Int, g: Int, b: Int, a: Int) = instance.newColor(r, g, b, a)

fun Long.newColor(color: Int) = RenderManager.color(this, color)
fun VG.newColor(color: Int) = instance.newColor(color)

fun NVGColor.fill(color: Int) = fill(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
fun NVGColor.fill(r: Int, g: Int, b: Int, a: Int) = RenderManager.fillNVGColorWithRGBA(
    r.toFloat() / 255f, g.toFloat() / 255f, b.toFloat() / 255f, a.toFloat() / 255f, this
)

fun NVGColor.fill(r: Float, g: Float, b: Float, a: Float) = RenderManager.fillNVGColorWithRGBA(r, g, b, a, this)


fun Long.scale(x: Float, y: Float) = RenderManager.scale(this, x, y)
fun VG.scale(x: Float, y: Float) = instance.scale(x, y)


fun Long.setAlpha(alpha: Float) = RenderManager.setAlpha(this, alpha)
fun VG.setAlpha(alpha: Float) = instance.setAlpha(alpha)


fun Long.drawSVG(filePath: String, x: Number, y: Number, width: Number, height: Number) =
    RenderManager.drawSvg(this, filePath, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

fun VG.drawSVG(filePath: String, x: Number, y: Number, width: Number, height: Number) =
    instance.drawSVG(filePath, x, y, width, height)

fun Long.drawSVG(filePath: String, x: Number, y: Number, width: Number, height: Number, color: Int) =
    RenderManager.drawSvg(this, filePath, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)

fun VG.drawSVG(filePath: String, x: Number, y: Number, width: Number, height: Number, color: Int) =
    instance.drawSVG(filePath, x, y, width, height, color)

fun Long.drawSVG(svg: SVGs, x: Number, y: Number, width: Number, height: Number) =
    RenderManager.drawSvg(this, svg, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat())

fun VG.drawSVG(svg: SVGs, x: Number, y: Number, width: Number, height: Number) =
    instance.drawSVG(svg, x, y, width, height)

fun Long.drawSVG(svg: SVGs, x: Number, y: Number, width: Number, height: Number, color: Int) =
    RenderManager.drawSvg(this, svg, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), color)

fun VG.drawSVG(svg: SVGs, x: Number, y: Number, width: Number, height: Number, color: Int) =
    instance.drawSVG(svg, x, y, width, height, color)


fun Long.drawInfo(type: InfoType, x: Number, y: Number, size: Number) =
    RenderManager.drawInfo(this, type, x.toFloat(), y.toFloat(), size.toFloat())

fun VG.drawInfo(type: InfoType, x: Number, y: Number, size: Number) = instance.drawInfo(type, x, y, size)