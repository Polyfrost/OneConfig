package org.polyfrost.oneconfig.internal.ui.hud.screens

import org.polyfrost.oneconfig.api.hud.v1.HudGroup
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel

private fun memberTitles(group: HudGroup): List<String> = group.huds.map { localizedLabel(it.title) ?: it.title }

/** [group]'s name, or its first two HUDs and how many more there are when a mod gave it none */
internal fun hudGroupLabel(group: HudGroup): String {
    group.name?.let { return it }
    val titles = memberTitles(group)
    return when {
        titles.size <= 2 -> titles.joinToString(", ")
        else -> "${titles[0]}, ${titles[1]} +${titles.size - 2}"
    }
}

/** Every HUD in [group] by title, as in "Air, Armor, Text Hud ×3" */
internal fun hudGroupMembers(group: HudGroup): String = joinMemberTitles(memberTitles(group))

/** [titles] in the order they first appear with each repeated title listed once and followed by how many there are */
internal fun joinMemberTitles(titles: List<String>): String =
    titles.groupingBy { it }.eachCount().entries.joinToString(", ") { (title, count) ->
        if (count > 1) "$title ×$count" else title
    }
