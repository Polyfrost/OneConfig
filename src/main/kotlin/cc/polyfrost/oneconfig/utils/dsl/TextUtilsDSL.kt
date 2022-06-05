package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.renderer.font.Font
import cc.polyfrost.oneconfig.utils.TextUtils

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see TextUtils.wrapText
 */
fun String.wrap(vg: Long, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(vg, this, maxWidth, fontSize.toFloat(), font)

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see wrap
 */
fun Long.wrap(text: String, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(this, text, maxWidth, fontSize.toFloat(), font)

/**
 * Wraps the given [String] to the given [maxWidth].
 * @see wrap
 */
fun VG.wrap(text: String, maxWidth: Float, fontSize: Number, font: Font) =
    TextUtils.wrapText(instance, text, maxWidth, fontSize.toFloat(), font)
