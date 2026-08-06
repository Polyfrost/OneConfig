package org.polyfrost.oneconfig.internal.ui.api

import org.polyfrost.oneconfig.api.ui.v1.ModCardType
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.hud.isHudModCard

const val UNTYPED_MOD_CARD_RANK = -1

sealed interface ModGridEntry {
    val key: Any

    data class Header(val type: ModCardType, val expanded: Boolean, val cardCount: Int) : ModGridEntry {
        override val key get() = "modCardType:${type.id}"
    }

    data class Card(val data: ConfigData) : ModGridEntry {
        override val key get() = data.id
    }
}

fun modCardGroupRank(id: String): Int {
    val type = ModCardTypes.typeFor(id) ?: return UNTYPED_MOD_CARD_RANK
    val index = ModCardTypes.types().indexOf(type)
    return if (index >= 0) index else UNTYPED_MOD_CARD_RANK
}

fun modCardOrder(): Comparator<ConfigData> {
    val ranks = HashMap<String, Int>()
    return compareBy<ConfigData> { ranks.getOrPut(it.id) { modCardGroupRank(it.id) } }
        .thenByDescending { ModFavorites.isFavorite(it.id) }
        .thenBy { ModOrder.indexOf(it.id) }
        .thenBy { if (isHudModCard(it.id)) 1 else 0 }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title.asRenderText() }
}

fun buildModGridEntries(sorted: List<ConfigData>, collapsed: (String) -> Boolean): List<ModGridEntry> {
    val out = ArrayList<ModGridEntry>(sorted.size + 4)
    var i = 0
    while (i < sorted.size) {
        val type = ModCardTypes.typeFor(sorted[i].id)
        var end = i + 1
        while (end < sorted.size && ModCardTypes.typeFor(sorted[end].id) == type) end++
        val group = sorted.subList(i, end)
        if (type == null) {
            group.forEach { out.add(ModGridEntry.Card(it)) }
        } else {
            val isCollapsed = collapsed(type.id)
            out.add(ModGridEntry.Header(type, expanded = !isCollapsed, cardCount = group.size))
            if (!isCollapsed) group.forEach { out.add(ModGridEntry.Card(it)) }
        }
        i = end
    }
    return out
}

fun sameModGroup(entries: List<ModGridEntry>, from: Int, to: Int): Boolean {
    if (entries.getOrNull(from) !is ModGridEntry.Card) return false
    if (entries.getOrNull(to) !is ModGridEntry.Card) return false
    if (from == to) return true
    val between = if (from < to) from + 1 until to else to + 1 until from
    return between.none { entries[it] is ModGridEntry.Header }
}

fun modGroupBounds(entries: List<ModGridEntry>, index: Int): IntRange {
    if (entries.getOrNull(index) !is ModGridEntry.Card) return IntRange.EMPTY
    var start = index
    while (start > 0 && entries[start - 1] is ModGridEntry.Card) start--
    var end = index
    while (end + 1 < entries.size && entries[end + 1] is ModGridEntry.Card) end++
    return start..end
}
