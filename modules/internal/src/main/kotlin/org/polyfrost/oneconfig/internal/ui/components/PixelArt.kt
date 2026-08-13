package org.polyfrost.oneconfig.internal.ui.components

import java.awt.image.BufferedImage

fun BufferedImage.isPixelArt(): Boolean {
    if (this.width <= 64 && this.height <= 64) {
        return true
    }

    val possibleGridSizes = arrayOf(2, 3, 5, 7, 11, 13, 17, 19)
    val gridSize: Int = possibleGridSizes.firstOrNull { this.width % it == 0 && this.height % it == 0 } ?: return false

    // only 10 lines are sampled for performance which is usually plenty
    val linesToVisit = 10
    for (i in 0 until linesToVisit) {
        val x = (this.width / linesToVisit) * i

        for (gridY in 0 until this.height / gridSize) {
            val gridYBase = gridY * gridSize
            val gridColor = this.getRGB(x, gridYBase)
            for (deltaY in 1 until gridSize) {
                if (gridColor != this.getRGB(x, gridYBase + deltaY)) {
                    return false
                }
            }
        }
    }
    return true
}
