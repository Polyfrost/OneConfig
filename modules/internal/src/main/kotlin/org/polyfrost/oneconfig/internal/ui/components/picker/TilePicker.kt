package org.polyfrost.oneconfig.internal.ui.components.picker

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.containVerticalScroll
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

interface PickerEntry {
    val id: String
    val displayName: String
}

typealias PickerTileIcon<T> = @Composable (entry: T, modifier: Modifier, alpha: Float) -> Unit

val PickerTileShape = RoundedCornerShape(8.dp)

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

/** A square cell with selection border tick badge and tooltip where a `null` [onClick] is decorative */
@Composable
fun <T : PickerEntry> PickerTile(
    entry: T,
    icon: PickerTileIcon<T>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    tooltip: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = enabled && onClick != null
    val background by animateColorAsState(
        when {
            selected -> Accent.copy(alpha = 0.18f).compositeOver(theme.modCardBackground)
            hovered && interactive -> theme.componentBackground
            else -> theme.modCardBackground
        }
    )
    val border by animateColorAsState(if (selected) Accent else theme.borderColor)
    val contentAlpha = if (enabled) 1f else 0.45f

    PickerTooltip(entry, enabled = tooltip) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(PickerTileShape)
                .background(background, PickerTileShape)
                .border(1.dp, border, PickerTileShape)
                .hoverable(interaction)
                .pointerHoverIcon(if (interactive) PointerIcon.Hand else PointerIcon.Default)
                .then(if (interactive) Modifier.onClick(interaction, onClick) else Modifier)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon(entry, Modifier.fillMaxSize(), contentAlpha)
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
    Tooltip(
        text = {
            Column {
                Text(
                    entry.displayName,
                    color = theme.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(entry.id, color = theme.textColorSecondary, fontSize = 9.sp)
            }
        },
        modifier = Modifier,
        anchor = anchor,
        content = content,
    )
}

/** Adaptive grid of [PickerTile]s where [selected] and [enabled] are queried per entry so selection rules stay with the caller */
@Composable
fun <T : PickerEntry> TileGrid(
    entries: List<T>,
    icon: PickerTileIcon<T>,
    modifier: Modifier = Modifier,
    selected: (T) -> Boolean = { false },
    enabled: (T) -> Boolean = { true },
    cellSize: Dp = PickerTileSize,
    maxHeight: Dp = 300.dp,
    spacing: Dp = TileGridSpacing,
    gutter: Dp = 12.dp,
    onClick: ((T) -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    val unique = remember(entries) { entries.distinctBy(PickerEntry::id) }
    val scrollbarStyle = LocalScrollbarStyle.current.copy(thickness = TileGridScrollbarThickness)
    CompositionLocalProvider(LocalScrollbarStyle provides scrollbarStyle) {
        ScrollbarBox(
            gridState,
            gutter,
            modifier = modifier.fillMaxWidth().heightIn(max = maxHeight)
                .containVerticalScroll { gridState.canScrollBackward || gridState.canScrollForward },
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(cellSize),
                state = gridState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                items(unique, key = PickerEntry::id) { entry ->
                    PickerTile(
                        entry,
                        icon,
                        modifier = Modifier.fillMaxWidth(),
                        selected = selected(entry),
                        enabled = enabled(entry),
                        onClick = onClick?.let { click -> { click(entry) } },
                    )
                }
            }
        }
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
    val interaction = rememberInteractionSource()
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = theme.textColor, fontSize = 12.sp, fontFamily = theme.typography.family),
        cursorBrush = SolidColor(theme.textColor),
        interactionSource = interaction,
        modifier = modifier.trackTextInputFocus().focusRequester(focusRequester),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(PickerTileShape)
                    .background(theme.componentBackground, PickerTileShape)
                    .border(1.dp, if (focused) Accent else theme.borderColor, PickerTileShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon("search", modifier = Modifier.size(14.dp), color = theme.textColorSecondary)
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            localizedString(null, placeholderKey),
                            color = theme.textColorSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    IconButton("close", modifier = Modifier.size(14.dp)) { onValueChange("") }
                }
            }
        },
    )
}

/**
 * Header search field and result grid making up the whole tile selector for any HUD or screen
 *
 * The [modifier] supplies the chrome, minus [gutter] side padding, so it fits a dropdown a HUD editor panel
 * or a full screen
 *
 * [selected] and [onToggle] leave the selection with the caller
 *
 * [maxEntries] of `0` or less is unlimited and `1` is radio-style
 */
@Composable
fun <T : PickerEntry> TilePicker(
    entries: List<T>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    icon: PickerTileIcon<T>,
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
    val filtered = remember(entries, query) { filterEntries(entries, query) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = gutter),
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
                        IconButton("close", modifier = Modifier.size(18.dp), onClick = onClose)
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

        val density = LocalDensity.current
        var stableHeight by remember(entries, gridMaxHeight) { mutableStateOf(0.dp) }
        Box(
            Modifier.fillMaxWidth().heightIn(min = stableHeight).onSizeChanged {
                val height = with(density) { it.height.toDp() }
                if (height > stableHeight) stableHeight = height
            },
        ) {
            if (filtered.isEmpty()) {
                PickerMessage(emptyKey, height = stableHeight.takeIf { it > 0.dp } ?: minOf(gridMaxHeight, PickerMessageHeight))
            } else {
                TileGrid(
                    entries = filtered,
                    icon = icon,
                    selected = { it.id in selected },
                    enabled = { canSelect(selected, it.id, maxEntries) },
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
    val current = selected.distinct()
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
