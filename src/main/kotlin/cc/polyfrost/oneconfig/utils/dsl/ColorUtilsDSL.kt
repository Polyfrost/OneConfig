package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.api.OneColor
import cc.polyfrost.oneconfig.utils.color.ColorUtils

/**
 * Creates a new [OneColor] from the given RGBA integer.
 *
 * @see OneColor
 */
fun Int.toColor() = OneColor(this)

/**
 * Get the red component of the given RGBA value.
 *
 * @see ColorUtils.getRed
 */
fun Int.getRed() = ColorUtils.getRed(this)

/**
 * Get the green component of the given RGBA value.
 *
 * @see ColorUtils.getGreen
 */
fun Int.getGreen() = ColorUtils.getGreen(this)

/**
 * Get the blue component of the given RGBA value.
 *
 * @see ColorUtils.getBlue
 */
fun Int.getBlue() = ColorUtils.getBlue(this)

/**
 * Get the alpha component of the given RGBA value.
 *
 * @see ColorUtils.getAlpha
 */
fun Int.getAlpha() = ColorUtils.getAlpha(this)

/**
 * Return the color with the given red component.
 *
 * @see ColorUtils.setRed
 */
fun Int.setRed(red: Int) = ColorUtils.setRed(this, red)

/**
 * Return the color with the given green component.
 *
 * @see ColorUtils.setGreen
 */
fun Int.setGreen(green: Int) = ColorUtils.setGreen(this, green)

/**
 * Return the color with the given blue component.
 *
 * @see ColorUtils.setBlue
 */
fun Int.setBlue(blue: Int) = ColorUtils.setBlue(this, blue)

/**
 * Return the color with the given alpha component.
 *
 * @see ColorUtils.setAlpha
 */
fun Int.setAlpha(alpha: Int) = ColorUtils.setAlpha(this, alpha)