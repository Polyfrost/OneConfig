/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.ui.pages

import com.google.gson.GsonBuilder
import org.polyfrost.oneconfig.utils.JsonUtils
import org.polyfrost.polyui.color.Colors
import org.polyfrost.polyui.component.ContainingComponent
import org.polyfrost.polyui.component.impl.Image
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.event.MouseClicked
import org.polyfrost.polyui.input.PolyText
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.layout.impl.FlexLayout
import org.polyfrost.polyui.layout.impl.PixelLayout
import org.polyfrost.polyui.layout.impl.SwitchingLayout
import org.polyfrost.polyui.property.Properties
import org.polyfrost.polyui.property.impl.BlockProperties.Companion.withStates
import org.polyfrost.polyui.property.impl.ImageProperties
import org.polyfrost.polyui.property.impl.TextProperties
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.*
import org.polyfrost.polyui.unit.Unit
import org.polyfrost.polyui.utils.fastEach
import org.polyfrost.polyui.utils.getResourceStreamNullable
import org.polyfrost.polyui.utils.radii
import java.text.SimpleDateFormat
import java.util.*

class ChangelogsPage(val owner: SwitchingLayout) {

    val content: Layout

    init {
        val rawNews = getResourceStreamNullable("/news.json")?.reader()?.use { it.readText() } ?: "[]"
        val gson = GsonBuilder().create()
        val newsList = arrayListOf<News>()
        for (news in JsonUtils.parseString(rawNews).asJsonArray) {
            newsList.add(gson.fromJson(news, News::class.java))
        }
        content = FlexLayout(
            resizesChildren = true,
            at = 0.px * 40.px,
            drawables = newsList.map {
                NewsCard(flex(), news = it, owner = owner)
            }.toTypedArray(),
            gap = Gap(0.px, 48.px)
        ).scrolling(1126.px * 600.px)
    }
    val self: Layout = object :
        PixelLayout(
            at = origin,
            size = 1126.px * 600.px,
            drawables = drawables(
                content,
            ),
        ),
        Page {
        override fun filter(query: String, search: Any.(String) -> Boolean) {
            children[1].components.fastEach {
                if (it !is NewsCard) return@fastEach
                it.exists = it.news.title.run { search(query) }
            }
            children[1].calculateBounds()
        }
    }

    fun open() {
        owner.switch(self)
    }

    @Suppress("UNCHECKED_CAST")
    private class NewsCard(
        at: Vec2<Unit>,
        val news: News,
        private val owner: SwitchingLayout
    ) : ContainingComponent(
        properties = NewsCardProperties,
        at = at,
        size = null,
        children = arrayOf(),
        acceptInput = true
    )
    {
        val titleText = Text(properties = NewsCardTitleProperties(), initialText = PolyText(news.title), at = 0.px * 0.px, size = 1126.px * 30.px)
        val bodyText = Text(properties = NewsCardBodyProperties(), initialText = PolyText(news.short ?: news.big), at = 0.px * 0.px, size = 1126.px * 1000.px)
        val dateText = Text(properties = NewsCardDateProperties(), initialText = PolyText(calculateDate(news.date)), at = 0.px * 0.px, textAlign = TextAlign.Left)
        val readButton = Text(properties = NewsCardReadButtonProperties(), initialText = PolyText("Read more"), at = 0.px * 0.px, textAlign = TextAlign.Left, acceptInput = true) // todo change this to right align
        val image = if (news.image != null) Image(properties = NewCardImageProperties(), image = PolyImage(news.image), at = origin, acceptInput = false) else null

        init {
            if (image != null) addComponents(image)
            addComponents(titleText, bodyText, dateText, readButton)
            readButton.events {
                MouseClicked(0) to {
                    NewsPage(news, owner).open()
                }
            }
        }

        override fun calculateSize(): Size<Unit> {
            if (bodyText.y == 0f) {
                bodyText.y = (titleText.properties.fontSize.px + 8)
            }
            if (image != null) {
                resizeImage(image)
                titleText.x = image.width + 20
                bodyText.x = titleText.x
                titleText.size = (1126 - titleText.x).px * 30.px
                bodyText.size = (1126 - bodyText.x).px * 1000.px
            }
            readButton.x = 1126f - renderer.textBounds(readButton.font, readButton.string, readButton.fontSize).width
            readButton.y = bodyText.y + (bodyText.lines.size + 2) * (bodyText.properties.fontSize.px)
            dateText.x = titleText.x
            dateText.y = readButton.y
            val mh2 = children.maxOf { it.y + (it.size?.height ?: 0f) }
            return 1126.px * mh2.px
        }

        object NewsCardProperties : Properties() {
            override val palette get() = colors.component.bg
        }

        class NewsCardTitleProperties : TextProperties() {
            override val fontSize = 16.px
            override val font
                get() = fonts.medium
        }

        class NewsCardBodyProperties : TextProperties() {
            override val palette: Colors.Palette
                get() = colors.text.secondary
        }

        class NewsCardDateProperties : TextProperties() {
            override val font
                get() = fonts.regular

            override val palette: Colors.Palette
                get() = colors.text.secondary
        }

        class NewsCardReadButtonProperties : TextProperties() {
            init {
                withStates()
            }
            override val fontSize = 12.px
            override val font
                get() = fonts.regular
        }

        class NewCardImageProperties : ImageProperties() {
            override val cornerRadii = 12.radii()
        }
    }

}

class NewsPage(news: News, val owner: SwitchingLayout) {

    val content: Layout

    init {
        content = FlexLayout(
            resizesChildren = true,
            at = 0.px * 40.px,
            drawables = arrayOf(
                TopComponent(flex(), news),
                Text(properties = BodyTextProperties(), at = flex(), initialText = PolyText(news.big), size = 1126.px * 100.px)
            ),
            gap = Gap(0.px, 24.px)
        ).scrolling(1126.px * 600.px)
    }
    val self: Layout = object :
        PixelLayout(
            at = origin,
            size = 1126.px * 600.px,
            drawables = drawables(
                content,
            ),
        ),
        Page {
        override fun filter(query: String, search: Any.(String) -> Boolean) {

        }
    }

    fun open() {
        owner.switch(self)
    }

    private class BodyTextProperties : TextProperties() {
        override val fontSize: Unit.Pixel = 14.px
    }

    private class TopComponent(
        at: Vec2<Unit>,
        news: News,
    ) : ContainingComponent(
        properties = TopComponentProperties,
        at = at,
        children = arrayOf()
    ) {
        val image = if (news.image != null) Image(properties = TopImageProperties(), image = PolyImage(news.image), at = origin, acceptInput = false) else null
        val titleText = Text(properties = TopTitleProperties(), initialText = PolyText(news.title), at = 0.px * 0.px, size = 1126.px * 30.px)
        val dateText = Text(properties = TopDateProperties(), initialText = PolyText("${if (news.author != null) "Written by ${news.author}\n" else ""}${calculateDate(news.date)}"), at = 0.px * 0.px, textAlign = TextAlign.Left, size = 1126.px * 60.px)

        init {
            if (image != null) addComponents(image)
            addComponents(titleText, dateText)
        }

        override fun calculateSize(): Size<Unit> {
            if (image != null) {
                resizeImage(image)
                titleText.x = image.width + 20
                titleText.size = (1126 - titleText.x).px * 1000.px
                dateText.x = titleText.x
                dateText.y = titleText.y + (titleText.lines.size) * (titleText.properties.fontSize.px) + 6
            }

            val mh2 = children.maxOf { it.y + (it.size?.height ?: 0f) }
            return 1126.px * mh2.px
        }

        object TopComponentProperties : Properties() {
            override val palette get() = colors.component.bg
        }

        class TopImageProperties : ImageProperties() {
            override val cornerRadii = 12.radii()
        }

        class TopTitleProperties : TextProperties() {
            override val fontSize = 24.px
            override val font
                get() = fonts.medium
        }

        class TopDateProperties : TextProperties() {
            override val font
                get() = fonts.regular

            override val palette: Colors.Palette
                get() = colors.text.secondary
        }
    }
}

data class News(
    val title: String,
    val short: String?,
    val big: String,
    val date: Int,
    val image: String?,
    val author: String?
)

private fun resizeImage(image: Image) {
    if (image.width >= 1126 / 2) {
        image.width /= 2f
        image.height /= 2f
        resizeImage(image)
    }
}

private fun calculateDate(n: Int): String {
    val date = Date(n * 1000L)

    // Extract day of the month to determine the ordinal

    // Extract day of the month to determine the ordinal
    val dayFormat = SimpleDateFormat("d")
    val day = dayFormat.format(date).toInt()

    // Create the formatted date string with the ordinal

    // Create the formatted date string with the ordinal
    val sdf = SimpleDateFormat("MMMM d")
    return sdf.format(date) + getDayOfMonthSuffix(day) + ", " + SimpleDateFormat("yyyy").format(date)
}

private fun getDayOfMonthSuffix(n: Int): String {
    return if (n in 11..13) {
        "th"
    } else when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
