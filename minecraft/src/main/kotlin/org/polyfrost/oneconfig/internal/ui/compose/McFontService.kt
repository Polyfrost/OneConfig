package org.polyfrost.oneconfig.internal.ui.compose

import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyGlyphPage
//? if >= 1.21.11 {
import net.minecraft.client.Minecraft
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.hud.v1.HudManager
//?}

object McFontService {
    fun init() {
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Thin.ttf", "poppins-thin")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-ThinItalic.ttf", "poppins-thin-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", "poppins")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Italic.ttf", "poppins-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Medium.ttf", "poppins-medium")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-SemiBold.ttf", "poppins-semibold")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-MediumItalic.ttf", "poppins-medium-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Bold.ttf", "poppins-bold")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-BoldItalic.ttf", "poppins-bold-italic")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-Black.ttf", "poppins-black")
        FontManager.loadFromResource("assets/oneconfig/fonts/Poppins/Poppins-BlackItalic.ttf", "poppins-black-italic")
        // used by toasts when a Minecraft theme is active
        FontManager.loadFromResource("assets/oneconfig/fonts/minecraft/Minecraft-Regular.otf", "minecraft", pixelAligned = true)
        FontManager.loadFromResource("assets/oneconfig/fonts/minecraft/Minecraft-Bold.otf", "minecraft-bold", pixelAligned = true)
        FontManager.loadFromResource("assets/oneconfig/fonts/unifont/unifont.otf", "unifont", pixelAligned = true)
        FontManager.setDefault("poppins")
        installEngines()
        //? if >= 1.21.11 {
        McFontQueue.measureWidth = { text, scale -> Minecraft.getInstance().font.width(text) * scale }
        EventManager.register(ResourceFinishedLoading::class.java) { _ ->
            FontManager.invalidateMetrics()
            HudManager.invalidate()
        }
        //?}
    }

    @JvmStatic
    fun installEngines() {
        PolyFont.engine = StbFonts
        PolyGlyphPage.rasterizer = StbFonts
        //? if >= 1.21.11
        PolyBitmap.decoder = BitmapTextures.decoder
    }
}
