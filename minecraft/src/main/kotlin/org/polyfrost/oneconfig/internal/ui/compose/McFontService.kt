package org.polyfrost.oneconfig.internal.ui.compose

import org.polyfrost.compose.render.FontManager

object McFontService {
    fun init() {
//        SkiaFontRenderer.init()
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", "poppins")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Bold.ttf", "poppins-bold")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Italic.ttf", "poppins-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-BoldItalic.ttf", "poppins-bold-italic")
    }
}
