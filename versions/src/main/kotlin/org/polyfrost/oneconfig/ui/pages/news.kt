package org.polyfrost.oneconfig.ui.pages

import org.polyfrost.polyui.renderer.data.PolyImage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object NewsManager {

}

class News(val title: String, val image: PolyImage? = null, val content: String, val summary: String = content, date: Long, val author: String) {
    val date = LocalDateTime.ofInstant(Instant.ofEpochSecond(date), ZoneId.systemDefault())
    val dateString by lazy {
        val after =
            when (this.date.dayOfMonth) {
                1, 21, 31 -> "st"
                2, 22 -> "nd"
                3, 23 -> "rd"
                else -> "th"
            }
        this.date.format(DateTimeFormatter.ofPattern("MMM dd, uuuu")).replace(",", "$after,")
    }
}