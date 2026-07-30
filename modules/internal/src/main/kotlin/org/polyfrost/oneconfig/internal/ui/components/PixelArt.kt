package org.polyfrost.oneconfig.internal.ui.components

import java.awt.image.BufferedImage

fun BufferedImage.isPixelArt(): Boolean {
    // We will treat images smaller than 64x64 as pixel art
    if (this.width <= 64 && this.height <= 64) {
        return true
    }

    // Choose the grid size by looping over some primes and checking if the dimensions of the image match
    val possibleGridSizes = arrayOf(2, 3, 5, 7, 11, 13, 17, 19)
    val gridSize: Int = possibleGridSizes.firstOrNull { this.width % it == 0 && this.height % it == 0 } ?: return false

    // Now verify the grid size by checking color doesn't change in the grid size
    // We only check 10 lines for performance and this is usually plenty
    val linesToVisit = 10
    for (i in 0 until linesToVisit) {
        val x = (this.width / linesToVisit) * i

        for (gridY in 0 until this.height / gridSize) {
            val gridYBase = gridY * gridSize
            val gridColor = this.getRGB(x, gridYBase)
            for (deltaY in 1 until gridSize) {
                // Check the color matches the base color for the entire grid
                if (gridColor != this.getRGB(x, gridYBase + deltaY)) {
                    return false
                }
            }
        }
    }
    return true
}
