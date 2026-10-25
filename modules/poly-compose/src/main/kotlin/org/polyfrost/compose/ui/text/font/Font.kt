package org.polyfrost.compose.ui.text.font

import kotlin.math.abs
import org.polyfrost.compose.render.FontManager

@JvmInline
value class FontWeight(val weight: Int) : Comparable<FontWeight> {
    override fun compareTo(other: FontWeight) = weight.compareTo(other.weight)

    companion object {
        val Thin = FontWeight(100)
        val ExtraLight = FontWeight(200)
        val Light = FontWeight(300)
        val Normal = FontWeight(400)
        val Medium = FontWeight(500)
        val SemiBold = FontWeight(600)
        val Bold = FontWeight(700)
        val ExtraBold = FontWeight(800)
        val Black = FontWeight(900)
    }
}

data class Font(val name: String, val weight: FontWeight = FontWeight.Normal)

class FontFamily(val fonts: List<Font>) {
    fun resolve(weight: FontWeight): String = fonts.minBy { abs(it.weight.weight - weight.weight) }.name

    companion object {
        val Default = FontFamily(Font(FontManager.DEFAULT_KEY))
    }
}

fun FontFamily(vararg fonts: Font) = FontFamily(fonts.toList())

fun FontFamily(name: String) = FontFamily(Font(name))
