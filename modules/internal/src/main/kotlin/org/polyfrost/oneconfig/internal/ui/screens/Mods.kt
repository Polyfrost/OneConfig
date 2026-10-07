package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.api.ModCardTypeCollapseStore
import org.polyfrost.oneconfig.internal.ui.api.ModFavorites
import org.polyfrost.oneconfig.internal.ui.api.ModGridEntry
import org.polyfrost.oneconfig.internal.ui.api.ModHidden
import org.polyfrost.oneconfig.internal.ui.api.ModOrder
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories
import org.polyfrost.oneconfig.internal.ui.api.buildModGridEntries
import org.polyfrost.oneconfig.internal.ui.api.modCardOrder
import org.polyfrost.oneconfig.internal.ui.api.modFavoriteAt
import org.polyfrost.oneconfig.internal.ui.api.modGroupBounds
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberGridReorderState
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.reorderContainer
import org.polyfrost.oneconfig.internal.ui.components.reorderOverlay
import org.polyfrost.oneconfig.internal.ui.components.reorderableItem
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyGridState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class ModCategory(
    val title: String,
    val icon: String?,
    val configCategory: Config.Category?,
    val favoritesOnly: Boolean = false,
    val hiddenOnly: Boolean = false,
) {
    All("All", null, null),
    Favorited("Favorites", "star", null, favoritesOnly = true),
    Hypixel("Hypixel", "hypixel", Config.Category.HYPIXEL),
    Performance("Performance", "lightning-01", Config.Category.PERFORMANCE),
    Visuals("Visuals", "paintbrush", Config.Category.VISUALS),
    HUD("HUD", "hud", Config.Category.HUD),
    Utility("Utility", "settings", Config.Category.UTILITY),
    QoL("Quality of Life", "qol", Config.Category.QOL),
    Other("Other", null, Config.Category.OTHER),
    Hidden("Hidden", "eye-off", null, hiddenOnly = true);
}

@Composable
fun Mods() {
    var activeCategory by remember { mutableStateOf(ModCategory.All) }

    DisposableEffect(Unit) {
        ShellState.title = "Mods"
        onDispose { }
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val cards = ConfigRegistry.modCardConfigs
            ModCategory.entries.forEach {
                val shown = when {
                    it.favoritesOnly || activeCategory == it -> true
                    it.hiddenOnly -> cards.any { configData -> ModHidden.isHidden(configData.id) }
                    else -> cards.any { configData ->
                        configData.category == it.configCategory && !ModHidden.isHidden(configData.id)
                    }
                }
                if (shown) {
                    Chip(
                        label = it.title,
                        selected = activeCategory == it,
                        icon = it.icon,
                        onClick = {
                            activeCategory = if (activeCategory != it) {
                                it
                            } else {
                                ModCategory.All;
                            }
                        }
                    )
                }
            }
        }
        ModsGrid(activeCategory)
    }
}

@Composable
fun ColumnScope.ModsGrid(category: ModCategory) {
    val registryRevision = ConfigRegistry.revision
    val hudRevision = HudManager.revision
    val categoryRevision = ThirdPartyModCategories.revision
    val favoriteRevision = ModFavorites.revision
    val hiddenRevision = ModHidden.revision
    val orderRevision = ModOrder.revision
    val typeRevision = ModCardTypes.revision
    val collapseRevision = ModCardTypeCollapseStore.revision
    val filtered = remember(
        registryRevision, hudRevision, category, categoryRevision, favoriteRevision, hiddenRevision, orderRevision,
        typeRevision,
    ) {
        ConfigRegistry.modCardConfigs
            .filter { ModHidden.isHidden(it.id) == category.hiddenOnly }
            .let { items ->
                if (category.configCategory == null) items
                else items.filter { it.category == category.configCategory }
            }
            .let { items ->
                if (category.favoritesOnly) items.filter { ModFavorites.isFavorite(it.id) }
                else items
            }
            .sortedWith(modCardOrder())
    }
    if (filtered.isEmpty() && (category.favoritesOnly || category.hiddenOnly)) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (category.hiddenOnly) "No hidden mods." else "No favorite mods.",
                color = LocalTheme.current.textColorSecondary,
            )
        }
        return
    }

    // mutated live while dragging so the grid re-lays out under the pointer
    val flat = remember(filtered, collapseRevision) {
        buildModGridEntries(filtered, ModCardTypeCollapseStore::isCollapsed)
    }
    val entries = remember(flat) { flat.toMutableStateList() }

    val gridState = rememberRestorableLazyGridState("mods")
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    // Only enable placement animations after the grid has been laid out for the current size
    var animateItems by remember(windowSize, density) { mutableStateOf(false) }
    val reorderState = rememberGridReorderState(
        gridState = gridState,
        onMove = { from, to -> entries.add(to, entries.removeAt(from)) },
        onDrop = { index -> commitDrop(entries, index) },
        dragBounds = { index -> modGroupBounds(entries, index) },
        onClick = { key -> entries.cardData(key)?.let(::openModCard) },
    )
    val dropFavorite = reorderState.draggingKey
        ?.let { key -> entries.indexOfFirst { it.key == key } }
        ?.takeIf { it >= 0 }
        ?.let { index -> modFavoriteAt(entries, index, ModFavorites::isFavorite) }
    // the dragged card's index no longer applies once the list is rebuilt
    DisposableEffect(entries) {
        onDispose { reorderState.cancel() }
    }

    // override scroll to stay in place instead of following the first visible item
    DisposableEffect(favoriteRevision) {
        gridState.requestScrollToItem(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        onDispose { }
    }

    val focusManager = LocalFocusManager.current
    Box(
        modifier = Modifier
            .weight(1f)
            .clipToBounds()
            // cards can't take focus, so clear it on press to unfocus the search field
            .pointerInput(focusManager) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    focusManager.clearFocus()
                }
            }
            .reorderContainer(reorderState),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(19.dp),
            horizontalArrangement = Arrangement.spacedBy(19.dp),
            modifier = Modifier.fillMaxSize().padding(end = 16.dp).onGloballyPositioned {
                animateItems = true
            },
        ) {
            items(
                entries,
                key = { it.key },
                contentType = { if (it is ModGridEntry.Header) HEADER_CONTENT_TYPE else CARD_CONTENT_TYPE },
                span = { if (it is ModGridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { entry ->
                when (entry) {
                    is ModGridEntry.Header -> ModTypeHeader(
                        entry,
                        modifier = if (animateItems) Modifier.animateItem(placementSpec = ModCardPlacementSpec) else Modifier,
                        onToggle = { ModCardTypeCollapseStore.toggle(entry.type.id) },
                    )

                    is ModGridEntry.Card -> {
                        val mod = entry.data
                        val dragging = reorderState.draggingKey == mod.id
                        val outlineAlpha = animateFloatAsState(if (dragging) 1f else 0f)
                        val favoriteSlot = if (dragging) dropFavorite == true else ModFavorites.isFavorite(mod.id)
                        ModCard(
                            mod,
                            modifier = Modifier
                                .then(
                                    if (animateItems) {
                                        Modifier.animateItem(
                                            placementSpec = if (dragging) null else ModCardPlacementSpec,
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .dropSlotOutline(favoriteSlot) { outlineAlpha.value }
                                .reorderableItem(reorderState, mod.id),
                            hoverHint = reorderState.overlayKey == null,
                        )
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(gridState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )

        // drawn over the grid so it passes above the other cards
        val draggedId = reorderState.overlayKey
        val dragged = remember(entries, draggedId) { draggedId?.let(entries::cardData) }
        if (dragged != null) {
            ModCard(
                dragged,
                modifier = Modifier.reorderOverlay(reorderState),
                favorite = dropFavorite ?: ModFavorites.isFavorite(dragged.id),
                interactive = false,
            )
        }
    }
}

@Composable
private fun ModTypeHeader(entry: ModGridEntry.Header, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val chevronRotation by animateFloatAsState(if (entry.expanded) 180f else 90f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onClick(interactionSource, onClick = onToggle)
            .pointerHoverIcon(PointerIcon.Hand),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Rule(Modifier.weight(1f))
        remember(entry.type.icon) { entry.type.icon?.takeIf(::canRenderIcon) }?.let { icon ->
            Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
        }
        Text(
            localizedLabel(entry.type.title) ?: entry.type.title,
            color = theme.textColorSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        if (!entry.expanded && entry.cardCount > 0) {
            Text("(${entry.cardCount})", color = theme.textColorSecondary, fontSize = 11.sp)
        }
        Icon(
            "up",
            color = theme.textColorSecondary,
            modifier = Modifier.size(12.dp).rotate(chevronRotation),
        )
        Rule(Modifier.weight(1f))
    }
}

@Composable
private fun Rule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(LocalTheme.current.borderColor))
}

private val ModCardPlacementSpec = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

private fun List<ModGridEntry>.cardData(key: Any): ConfigData? =
    firstNotNullOfOrNull { (it as? ModGridEntry.Card)?.data?.takeIf { mod -> mod.id == key } }

private fun openModCard(mod: ConfigData) {
    val onOpen = mod.onOpen
    when {
        onOpen != null -> onOpen()
        mod.source == ConfigSource.OC -> LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
    }
}

/** Persists the arrangement and the favorite state after a card is dropped at [index] */
private fun commitDrop(entries: List<ModGridEntry>, index: Int) {
    val dropped = (entries.getOrNull(index) as? ModGridEntry.Card)?.data ?: return
    if (modFavoriteAt(entries, index, ModFavorites::isFavorite) != ModFavorites.isFavorite(dropped.id)) {
        ModFavorites.toggle(dropped.id)
    }
    val group = entries.slice(modGroupBounds(entries, index)).filterIsInstance<ModGridEntry.Card>().map { it.data }
    ModOrder.reorder(
        group.map { it.id },
        ConfigRegistry.modCardConfigs.sortedWith(modCardOrder()).map { it.id },
    )
}

/** Dashed outline on the dragged card's slot, in the favorite color while it would land as a [favorite] */
@Composable
private fun Modifier.dropSlotOutline(favorite: Boolean, alpha: () -> Float): Modifier {
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    val color = if (favorite) theme.favoriteColor else theme.textColorSecondary
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        val stroke = Stroke(1.5.dp.toPx(), pathEffect = dash)
        onDrawBehind {
            val a = alpha()
            if (a == 0f) return@onDrawBehind
            drawOutline(outline, color.copy(alpha = 0.08f * a))
            drawOutline(outline, color.copy(alpha = 0.6f * a), style = stroke)
        }
    }
}

private const val HEADER_CONTENT_TYPE = "header"
private const val CARD_CONTENT_TYPE = "card"

private val ModCardFooterHeight = 36.dp

private val ModCardGlowHeight = 50.dp

/**
 * [hoverHint] faintly shows an empty star on hover, and a card that is not [interactive] lets clicks and scrolling
 * pass through
 */
@Composable
fun ModCard(
    mod: ConfigData,
    modifier: Modifier = Modifier,
    favorite: Boolean = ModFavorites.isFavorite(mod.id),
    hoverHint: Boolean = true,
    interactive: Boolean = true,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current

    Box(
        modifier = modifier.fillMaxWidth().height(140.dp)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, remember(theme.borderColor) {
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
            }, theme.modCardShape)
            .then(
                if (interactive) {
                    // cards never take focus, otherwise they slide in from off screen when scrolled back into view
                    Modifier
                        .focusProperties { canFocus = false }
                        .onClick(interactionSource) { openModCard(mod) }
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            )
            .clip(theme.modCardShape)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val preview = mod.preview
                val icon = remember(mod.icon) { mod.icon?.takeIf(::canRenderIcon) }
                if (preview != null) {
                    preview(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp))
                } else if (icon != null) {
                    Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
                } else {
                    Text(
                        remember(mod.title) { mod.title.asRenderText() },
                        color = theme.textColor,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ModCardFooterHeight)
                    .background(Accent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mod.title,
                    color = LocalTheme.current.accentTextColor,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (LocalTheme.current.shadowEnabled) {
            val vignetteColor = theme.textColor
            Box(
                Modifier.fillMaxSize().drawWithCache {
                    val vignette = Brush.radialGradient(
                        colors = listOf(
                            vignetteColor.copy(alpha = 0f),
                            vignetteColor.copy(alpha = 0.04f),
                            vignetteColor.copy(alpha = 0.08f)
                        ),
                        center = size.center,
                        radius = size.minDimension * 0.9f
                    )
                    val glowHeight = ModCardGlowHeight.toPx().coerceAtMost(size.height)
                    val glowTop = size.height - glowHeight
                    val glow = Brush.verticalGradient(
                        0f to Accent.copy(0f),
                        0.4f to Accent.copy(0.2f),
                        1f to Accent.copy(0.4f),
                        startY = glowTop,
                        endY = size.height,
                    )
                    onDrawBehind {
                        drawRect(vignette)
                        drawRect(
                            glow,
                            topLeft = Offset(0f, glowTop),
                            size = Size(size.width, glowHeight),
                        )
                    }
                }
            )
        }

        FavoriteStar(
            mod = mod,
            favorite = favorite,
            cardInteractions = interactionSource,
            hoverHint = hoverHint,
            interactive = interactive,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        HideToggle(
            mod = mod,
            cardInteractions = interactionSource,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun FavoriteStar(
    mod: ConfigData,
    favorite: Boolean,
    cardInteractions: InteractionSource,
    hoverHint: Boolean,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val cardHovered by cardInteractions.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        when {
            favorite -> 1f
            !hoverHint -> 0f
            hovered -> 1f
            cardHovered -> 0.6f
            else -> 0f
        }
    )
    val color by animateColorAsState(if (favorite) theme.favoriteColor else theme.textColor)

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp)
            .alpha(alpha)
            .then(
                if (interactive) {
                    Modifier
                        .focusProperties { canFocus = false }
                        .onClick(interactionSource) { ModFavorites.toggle(mod.id) }
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (favorite) "star-filled" else "star", color = color, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun HideToggle(
    mod: ConfigData,
    cardInteractions: InteractionSource,
    modifier: Modifier = Modifier,
) {
    val hidden = ModHidden.isHidden(mod.id)
    val interactionSource = rememberInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val cardHovered by cardInteractions.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        when {
            hidden || hovered -> 1f
            cardHovered -> 0.6f
            else -> 0f
        }
    )

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp)
            .alpha(alpha)
            .onClick(interactionSource) { ModHidden.toggle(mod.id) }
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (hidden) "eye-off" else "eye", color = LocalTheme.current.textColor, modifier = Modifier.size(18.dp))
    }
}
