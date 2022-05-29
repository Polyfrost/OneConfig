package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.MathUtils

/**
 * @see MathUtils.clamp
 */
fun Number.clamp(min: Number = 0F, max: Number = 1F) = MathUtils.clamp(this.toFloat(), min.toFloat(), max.toFloat())