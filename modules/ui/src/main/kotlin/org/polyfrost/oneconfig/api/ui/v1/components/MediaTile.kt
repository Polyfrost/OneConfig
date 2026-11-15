@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object MediaTileDefaults {
    public const val aspectRatio: Float = 16f / 9f

    public val selectDotSize: Dp = 22.dp

    public val selectDotRing: Dp = 1.5.dp

    public const val scrimAlpha: Float = 0.75f

    public const val scrimStart: Float = 0.45f

    public const val subtitleAlpha: Float = 0.7f
}

@ExperimentalOneConfigUiApi
public object FilmstripDefaults {
    public val tileMinWidth: Dp = 104.dp

    public const val maxTiles: Int = 9

    public const val restAlpha: Float = 0.55f

    public val gap: Dp
        @Composable get() = OneConfigTheme.current.spacing.s
}

@ExperimentalOneConfigUiApi
@Composable
public fun MediaTile(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    selecting: Boolean = false,
    title: String? = null,
    subtitle: String? = null,
    aspectRatio: Float? = MediaTileDefaults.aspectRatio,
    enabled: Boolean = true,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    tags: (@Composable RowScope.() -> Unit)? = null,
    image: @Composable BoxScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val engaged = hovered || focusWithin
    val overlay by Motion.animateFloatAsState(if (engaged || selected) 1f else 0f, Motion.tweenSpec(), "mediaTileOverlay")
    val shadow = theme.onMediaShadow
    Card(
        modifier
            .then(if (aspectRatio != null) Modifier.fillMaxWidth().aspectRatio(aspectRatio) else Modifier)
            .onFocusChanged { focusWithin = it.hasFocus },
        onClick = onClick,
        label = label,
        selected = selected,
        enabled = enabled,
        shape = shape,
        interactionSource = source,
    ) {
        image()
        if (title != null || subtitle != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(overlay)
                    .background(Brush.verticalGradient(MediaTileDefaults.scrimStart to Color.Transparent, 1f to shadow.copy(alpha = MediaTileDefaults.scrimAlpha))),
            )
            Column(
                Modifier.align(Alignment.BottomStart).padding(spacing.l).alpha(overlay),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                if (title != null) LayoutText(title, theme.typography.label, theme.onMedia, maxLines = 1)
                if (subtitle != null) LayoutText(subtitle, theme.typography.caption, theme.onMedia.copy(alpha = MediaTileDefaults.subtitleAlpha), maxLines = 1)
            }
        }
        if (!engaged && tags != null) {
            Row(Modifier.align(Alignment.TopEnd).padding(spacing.m), horizontalArrangement = Arrangement.spacedBy(spacing.xs), content = tags)
        }
        if (engaged && !selecting && actions != null) {
            Row(Modifier.align(Alignment.TopEnd).padding(spacing.m), horizontalArrangement = Arrangement.spacedBy(spacing.xs), content = actions)
        }
        if (onSelectedChange != null && (engaged || selecting)) {
            SelectDot(selected, { onSelectedChange(!selected) }, Modifier.align(Alignment.TopStart).padding(spacing.m), enabled = enabled)
        } else if (badge != null) {
            Box(Modifier.align(Alignment.TopStart).padding(spacing.m)) { badge() }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun SelectDot(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = if (selected) layoutString("oneconfig.ui.selection.deselect", "Deselect") else layoutString("oneconfig.ui.selection.select", "Select"),
    enabled: Boolean = true,
    size: Dp = MediaTileDefaults.selectDotSize,
) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val accent = OneConfigTheme.accent
    val circle = theme.shapes.circle
    val fill by Motion.animateColorAsState(if (selected) accent else theme.onMediaShadow, Motion.colorSpec(), "selectDotFill")
    val ring by Motion.animateColorAsState(
        if (selected) accent else theme.onMedia.copy(alpha = if (hovered) 1f else MediaTileDefaults.subtitleAlpha),
        Motion.colorSpec(),
        "selectDotRing",
    )
    Box(
        modifier
            .size(size)
            .background(fill, circle)
            .border(snappedStroke(MediaTileDefaults.selectDotRing), ring, circle)
            .oneConfigClickable(label, onClick, A11yRole.Checkbox, enabled, checked = selected, shape = circle, indication = false, interactionSource = source),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon("tick", null, Modifier.size(ControlDefaults.metrics(ControlSize.Compact).iconSize), theme.accentTextColor)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun FilmstripTile(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.medium
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val rest by Motion.animateFloatAsState(if (selected || hovered) 1f else FilmstripDefaults.restAlpha, Motion.tweenSpec(), "filmstripTile")
    Box(
        modifier
            .clip(outline)
            .alpha(rest)
            .selectableCard(selected, onClick, label, shape = outline, interactionSource = source),
    ) {
        content()
        if (!selected) Box(Modifier.fillMaxSize().border(1.dp, theme.borderColor, outline))
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun Filmstrip(
    count: Int,
    index: Int,
    onIndexChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier,
    itemKey: ((Int) -> Any)? = null,
    tileMinWidth: Dp = FilmstripDefaults.tileMinWidth,
    maxTiles: Int = FilmstripDefaults.maxTiles,
    aspectRatio: Float = MediaTileDefaults.aspectRatio,
    gap: Dp = FilmstripDefaults.gap,
    tileModifier: (Int) -> Modifier = { Modifier },
    shape: Shape? = null,
    previousLabel: String = layoutString("oneconfig.ui.filmstrip.previous", "Earlier items"),
    nextLabel: String = layoutString("oneconfig.ui.filmstrip.next", "Later items"),
    thumbnail: @Composable BoxScope.(Int) -> Unit,
) {
    if (count <= 0) return
    val current = index.coerceIn(0, count - 1)
    val arrow = ControlSize.Compact.height
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val usable = (maxWidth - (arrow + gap) * 2).coerceAtLeast(tileMinWidth)
        val perPage = ((usable + gap) / (tileMinWidth + gap)).toInt().coerceIn(1, maxTiles.coerceAtLeast(1))
        val tileWidth = (usable - gap * (perPage - 1)) / perPage
        val tileHeight = tileWidth / aspectRatio
        val pages = (count + perPage - 1) / perPage
        var page by remember { mutableIntStateOf(current / perPage) }
        LaunchedEffect(current, perPage) { page = current / perPage }
        val shown = page.coerceIn(0, (pages - 1).coerceAtLeast(0))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
            IconButton("left-arrow", previousLabel, { page = (shown - 1).coerceAtLeast(0) }, enabled = shown > 0, size = ControlSize.Compact)
            AnimatedContent(
                targetState = shown,
                modifier = Modifier.size(usable, tileHeight).clipToBounds(),
                transitionSpec = {
                    val forward = targetState > initialState
                    Motion.enter(slideInHorizontally(Motion.tweenSpec()) { if (forward) it else -it }) togetherWith
                        Motion.exit(slideOutHorizontally(Motion.tweenSpec()) { if (forward) -it else it })
                },
                label = "filmstrip",
            ) { at ->
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                    for (item in at * perPage until minOf(count, (at + 1) * perPage)) {
                        key(itemKey?.invoke(item) ?: item) {
                            FilmstripTile(item == current, { onIndexChange(item) }, label(item), tileModifier(item).size(tileWidth, tileHeight), shape) {
                                thumbnail(item)
                            }
                        }
                    }
                }
            }
            IconButton("right-arrow", nextLabel, { page = (shown + 1).coerceAtMost(pages - 1) }, enabled = shown < pages - 1, size = ControlSize.Compact)
        }
    }
}
