package org.polyfrost.oneconfig.internal.ui.components.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Locale
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.VerticalScrollbar
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.ColumnScope
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.lazy.rememberLazyListState
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.focusOnPress
import org.polyfrost.oneconfig.internal.ui.components.settings.textKeys
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

interface PickerEntry {
    val id: String
    val displayName: String
}

typealias PickerTileIcon<T> = @Composable (entry: T, modifier: Modifier, alpha: Float) -> Unit

val PickerTileShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

val PickerTileSize = 44.dp

/** Display name over ID where a `null` [entry] is not in the catalog so [id] becomes the name */
@Composable
fun PickerLabel(
    id: String,
    entry: PickerEntry?,
    modifier: Modifier = Modifier,
    nameColor: Color = LocalTheme.current.textColor,
    idColor: Color = LocalTheme.current.textColorSecondary,
    nameSize: TextUnit = 12.sp,
    idSize: TextUnit = 10.sp,
    unavailableKey: String = PickerStrings.Unavailable,
) {
    Column(modifier = modifier) {
        Text(
            entry?.displayName ?: id,
            color = nameColor,
            fontSize = nameSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            entry?.id ?: localizedString(null, unavailableKey),
            color = idColor,
            fontSize = idSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun <T : PickerEntry> PickerTile(
    entry: T,
    icon: PickerTileIcon<T>,
    modifier: Modifier = Modifier,
    size: Dp = PickerTileSize,
    selected: Boolean = false,
    enabled: Boolean = true,
    tooltip: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = enabled && onClick != null
    val background = animateColorAsState(
        when {
            selected -> theme.modCardBackground.lerp(Accent.withAlpha(255), 0.18f)
            hovered && interactive -> theme.componentBackground
            else -> theme.modCardBackground
        }
    )
    val border = animateColorAsState(if (selected) Accent else theme.borderColor)
    val contentAlpha = if (enabled) 1f else 0.45f

    PickerTooltip(entry, enabled = tooltip) {
        Box(
            modifier = modifier
                .size(size)
                .clip(PickerTileShape)
                .background(background, PickerTileShape)
                .border(1.dp, border, PickerTileShape)
                .hoverable(interaction)
                .pointerHoverIcon(if (interactive) PointerIcon.Hand else PointerIcon.Default)
                .then(if (interactive) Modifier.onClick(interaction, onClick) else Modifier)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon(entry, Modifier.size(size - 10.dp), contentAlpha)
            if (selected) {
                Icon(
                    "tick",
                    modifier = Modifier.align(Alignment.TopEnd).size(11.dp),
                    color = Accent,
                )
            }
        }
    }
}

/** Wraps [content] in the name and ID tooltip or renders it bare when [enabled] is false */
@Composable
fun PickerTooltip(
    entry: PickerEntry,
    enabled: Boolean = true,
    anchor: Alignment = Alignment.BottomCenter,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val theme = LocalTheme.current
    TooltipArea(
        tooltip = {
            Column(
                modifier = Modifier
                    .background(theme.popupBackground, PickerTooltipShape)
                    .border(1.dp, theme.borderColor, PickerTooltipShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    entry.displayName,
                    color = theme.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(entry.id, color = theme.textColorSecondary, fontSize = 9.sp)
            }
        },
        below = anchor.verticalBias > 0f,
        content = content,
    )
}

private val PickerTooltipShape = RoundedCornerShape(8.dp)

@Composable
fun <T : PickerEntry> TileGrid(
    entries: List<T>,
    icon: PickerTileIcon<T>,
    width: Dp,
    modifier: Modifier = Modifier,
    selected: (T) -> Boolean = { false },
    enabled: (T) -> Boolean = { true },
    cellSize: Dp = PickerTileSize,
    maxHeight: Dp = 300.dp,
    spacing: Dp = TileGridSpacing,
    gutter: Dp = 12.dp,
    onClick: ((T) -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val gridState = rememberLazyListState()
    val available = width - gutter * 2
    val columns = gridColumns(available, cellSize, spacing)
    val cell = (available - spacing * (columns - 1)) / columns
    val rows = remember(entries, columns) { entries.chunked(columns) }
    Box(modifier = modifier.fillMaxWidth().heightIn(max = maxHeight)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = gutter),
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(rows, key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (entry in row) key(entry.id) {
                        PickerTile(
                            entry,
                            icon,
                            size = cell,
                            selected = selected(entry),
                            enabled = enabled(entry),
                            onClick = onClick?.let { click -> { click(entry) } },
                        )
                    }
                }
            }
        }
        VerticalScrollbar(
            gridState.scroll,
            color = theme.textColorSecondary.withAlpha(0.40f),
            hoverColor = theme.textColorSecondary.withAlpha(0.70f),
            modifier = Modifier
                .padding(end = (gutter - TileGridScrollbarThickness) / 2)
                .fillMaxHeight()
                .align(Alignment.TopEnd),
            thickness = TileGridScrollbarThickness,
            radius = minOf(theme.scrollBarShape.radius, 3f).dp,
        )
    }
}

private val TileGridScrollbarThickness = 6.dp

val TileGridSpacing = 6.dp

/** Search box for [TileGrid] where [autoFocus] is off by default for embedded use */
@Composable
fun PickerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    placeholderKey: String = PickerStrings.Search,
) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val state = remember { TextFieldState(value) }
    LaunchedEffect(value) {
        if (state.text != value) {
            state.replaceText(value)
            state.moveCaret(value.length)
        }
    }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            state.requestFocus()
        }
    }

    val border = animateColorAsState(if (state.focused) Accent else theme.borderColor)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(theme.componentBackground, PickerTileShape)
            .border(1.dp, border, PickerTileShape)
            .onPointerEvent { focusOnPress(it, state) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon("search", modifier = Modifier.size(14.dp), color = theme.textColorSecondary)
        Box(modifier = Modifier.weight(1f)) {
            if (state.text.isEmpty() && state.preedit.isEmpty()) {
                Text(
                    localizedString(null, placeholderKey),
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = theme.textColorSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                state,
                PolyFont(theme.typography.family.resolve(FontWeight.Normal), 12f),
                color = theme.textColor,
                caretColor = theme.textColor,
                selectionColor = Accent.withAlpha(0.4f),
                readClipboard = clipboard.read,
                writeClipboard = clipboard.write,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                onKey = textKeys(clipboard, state, onValueChange),
            )
        }
        if (state.text.isNotEmpty()) {
            IconButton("close", modifier = Modifier.size(14.dp).pointerHoverIcon(PointerIcon.Hand)) {
                state.replaceText("")
                state.moveCaret(0)
                onValueChange("")
            }
        }
    }
}

@Composable
fun <T : PickerEntry> TilePicker(
    entries: List<T>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    icon: PickerTileIcon<T>,
    width: Dp,
    modifier: Modifier = Modifier,
    maxEntries: Int = 0,
    onClose: (() -> Unit)? = null,
    autoFocusSearch: Boolean = true,
    cellSize: Dp = PickerTileSize,
    gridMaxHeight: Dp = 300.dp,
    gutter: Dp = 12.dp,
    titleKey: String? = PickerStrings.Title,
    searchPlaceholderKey: String = PickerStrings.Search,
    emptyKey: String = PickerStrings.Empty,
    header: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    var query by remember { mutableStateOf("") }
    val unique = remember(entries) { entries.distinctBy(PickerEntry::id) }
    val filtered = remember(unique, query) { filterEntries(unique, query) }

    Column(
        modifier = modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = gutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (header != null) {
                header()
            } else if (titleKey != null || onClose != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (titleKey != null) {
                            Text(
                                localizedString(null, titleKey),
                                color = theme.textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (maxEntries > 0) {
                            Text(
                                "${selected.size}/$maxEntries",
                                color = theme.textColorSecondary,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    if (onClose != null) {
                        IconButton("close", modifier = Modifier.size(18.dp).pointerHoverIcon(PointerIcon.Hand), onClick = onClose)
                    }
                }
            }

            PickerSearchField(
                query,
                onValueChange = { query = it },
                autoFocus = autoFocusSearch,
                placeholderKey = searchPlaceholderKey,
            )
        }

        val height = if (unique.isEmpty()) {
            minOf(gridMaxHeight, PickerMessageHeight)
        } else {
            fittedGridHeight(unique.size, width, cellSize, TileGridSpacing, gutter, gridMaxHeight)
        }
        Box(Modifier.fillMaxWidth().height(height)) {
            if (filtered.isEmpty()) {
                PickerMessage(emptyKey, height = height)
            } else {
                TileGrid(
                    entries = filtered,
                    icon = icon,
                    width = width,
                    selected = { it.id in selected },
                    enabled = { canSelect(selected, it.id, maxEntries) },
                    modifier = Modifier.fillMaxSize(),
                    cellSize = cellSize,
                    maxHeight = gridMaxHeight,
                    gutter = gutter,
                    onClick = { onToggle(it.id) },
                )
            }
        }
    }
}

private val PickerMessageHeight = 120.dp

fun fittedGridHeight(count: Int, width: Dp, cellSize: Dp, spacing: Dp, gutter: Dp, maxHeight: Dp): Dp {
    if (count <= 0) return 0.dp
    val available = width - gutter * 2
    val columns = gridColumns(available, cellSize, spacing)
    val row = (available - spacing * (columns - 1)) / columns
    val rows = (count + columns - 1) / columns
    return (row * rows + spacing * (rows - 1)).coerceIn(0.dp, maxHeight)
}

private fun gridColumns(available: Dp, cellSize: Dp, spacing: Dp): Int =
    maxOf(((available + spacing) / (cellSize + spacing)).toInt(), 1)

/** Centered message for empty and unavailable states */
@Composable
fun PickerMessage(key: String, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(
            localizedString(null, key),
            color = LocalTheme.current.textColorSecondary,
            fontSize = 12.sp,
        )
    }
}

/** Default translation keys where every composable taking one accepts an override */
object PickerStrings {
    const val Title = "oneconfig.picker.title"
    const val Search = "oneconfig.picker.search"
    const val Empty = "oneconfig.picker.empty"
    const val Unavailable = "oneconfig.picker.unavailable"
}

/** Whether [id] can be picked where already-selected entries stay clickable so they can be deselected */
fun canSelect(selected: List<String>, id: String, maxEntries: Int): Boolean =
    id in selected || maxEntries <= 0 || maxEntries == 1 || selected.size < maxEntries

fun toggleEntry(selected: List<String>, id: String, maxEntries: Int): List<String> {
    val current = normalizeIds(selected)
    if (id in current) return current - id
    if (maxEntries == 1) return listOf(id)
    if (maxEntries > 0 && current.size >= maxEntries) return current
    return current + id
}

fun normalizeIds(ids: Iterable<String>): List<String> = ids
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()

fun <T : PickerEntry> filterEntries(entries: List<T>, query: String): List<T> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
    if (terms.isEmpty()) return entries
    return entries.filter { entry ->
        val haystack = "${entry.displayName.lowercase(Locale.ROOT)} ${entry.id.lowercase(Locale.ROOT)}"
        terms.all(haystack::contains)
    }
}
