package org.polyfrost.oneconfig.api.ui.v1.image

import org.polyfrost.polyui.data.image.FlagKey

object NVGImageFlags {
    const val NONE = 0
    const val REPEAT_X = 1 shl 1
    const val REPEAT_Y = 1 shl 2
    const val FLIP_Y = 1 shl 3
    const val PREMULTIPLIED = 1 shl 4
    const val NEAREST = 1 shl 5

    @JvmField val KEY = FlagKey<Int>("nvg:sampling")
}
