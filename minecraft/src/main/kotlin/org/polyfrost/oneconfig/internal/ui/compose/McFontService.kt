package org.polyfrost.oneconfig.internal.ui.compose

import org.polyfrost.compose.render.FontManager

object McFontService {
    fun init() {
//        SkiaFontRenderer.init()
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Thin.ttf", "poppins-thin")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-ThinItalic.ttf", "poppins-thin-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", "poppins")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Italic.ttf", "poppins-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Medium.ttf", "poppins-medium")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-MediumItalic.ttf", "poppins-medium-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Bold.ttf", "poppins-bold")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-BoldItalic.ttf", "poppins-bold-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Black.ttf", "poppins-black")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-BlackItalic.ttf", "poppins-black-italic")
        // Minecraft theme font (used by toasts when a Minecraft theme is active).
        FontManager.loadFromResource("assets/oneconfig/fonts/minecraft/Minecraft-Regular.otf", "minecraft")
        FontManager.loadFromResource("assets/oneconfig/fonts/minecraft/Minecraft-Bold.otf", "minecraft-bold")
        // Make Poppins the default typeface so PolyText (which uses the default font) renders glyphs;
        // previously nothing was registered for the default key.
        FontManager.setDefault("poppins")
    }
}
