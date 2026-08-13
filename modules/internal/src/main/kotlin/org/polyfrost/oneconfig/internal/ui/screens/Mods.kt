package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import org.polyfrost.oneconfig.internal.ui.api.ModOrder
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories
import org.polyfrost.oneconfig.internal.ui.api.buildModGridEntries
import org.polyfrost.oneconfig.internal.ui.api.modCardOrder
import org.polyfrost.oneconfig.internal.ui.api.modGroupBounds
import org.polyfrost.oneconfig.internal.ui.api.sameModGroup
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberGridReorderState
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.reorderOverlay
import org.polyfrost.oneconfig.internal.ui.components.reorderableItem
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyGridState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

enum class ModCategory(
    val title: String,
    val icon: String?,
    val configCategory: Config.Category?,
    val favoritesOnly: Boolean = false,
) {
    All("All", null, null),
    Favorited("Favorites", "star", null, favoritesOnly = true),
    Hypixel("Hypixel", "hypixel", Config.Category.HYPIXEL),
    Performance("Performance", "lightning-01", Config.Category.PERFORMANCE),
    Visuals("Visuals", "paintbrush", Config.Category.VISUALS),
    HUD("HUD", "hud", Config.Category.HUD),
    Utility("Utility", "settings", Config.Category.UTILITY),
    QoL("Quality of Life", "qol", Config.Category.QOL),
    Other("Other", null, Config.Category.OTHER);
}

@Composable
fun Mods() {
    var activeCategory by remember { mutableStateOf(ModCategory.All) }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModCategory.entries.forEach {
                Chip(
                    label = it.title,
                    selected = activeCategory == it,
                    icon = it.icon,
                    onClick = { activeCategory = it }
                )
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
    val orderRevision = ModOrder.revision
    val typeRevision = ModCardTypes.revision
    val collapseRevision = ModCardTypeCollapseStore.revision
    val filtered = remember(
        registryRevision, hudRevision, category, categoryRevision, favoriteRevision, orderRevision, typeRevision,
    ) {
        ConfigRegistry.modCardConfigs
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

    if (filtered.isEmpty() && category.favoritesOnly) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No favorite mods.", color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    // mutated live while dragging so the grid re-lays out under the pointer
    val flat = remember(filtered, collapseRevision) {
        buildModGridEntries(filtered, ModCardTypeCollapseStore::isCollapsed)
    }
    val entries = remember(flat) { flat.toMutableStateList() }

    val gridState = rememberRestorableLazyGridState("mods")
    val reorderState = rememberGridReorderState(
        gridState = gridState,
        onMove = { from, to -> entries.add(to, entries.removeAt(from)) },
        onDrop = { index -> commitDrop(entries, index) },
        canSwap = { from, to -> sameModGroup(entries, from, to) },
    )

    Box(modifier = Modifier.weight(1f)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(19.dp),
            horizontalArrangement = Arrangement.spacedBy(19.dp),
            modifier = Modifier.padding(end = 16.dp),
        ) {
            items(
                entries,
                key = { it.key },
                span = { if (it is ModGridEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
            ) { entry ->
                when (entry) {
                    is ModGridEntry.Header -> ModTypeHeader(
                        entry,
                        modifier = Modifier.animateItem(placementSpec = ModCardPlacementSpec),
                        onToggle = { ModCardTypeCollapseStore.toggle(entry.type.id) },
                    )

                    is ModGridEntry.Card -> {
                        val mod = entry.data
                        val dragging = reorderState.draggingKey == mod.id
                        ModCard(
                            mod,
                            modifier = Modifier
                                .animateItem(placementSpec = if (dragging) null else ModCardPlacementSpec)
                                .reorderableItem(reorderState, mod.id),
                        )
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(gridState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )

        // drawn outside the grid so the card is not clipped when dragged past a viewport edge
        val draggedId = reorderState.overlayKey
        val dragged = remember(entries, draggedId) {
            entries.firstNotNullOfOrNull { (it as? ModGridEntry.Card)?.data?.takeIf { mod -> mod.id == draggedId } }
        }
        if (dragged != null) ModCard(dragged, modifier = Modifier.reorderOverlay(reorderState))
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
        entry.type.icon?.takeIf(::canRenderIcon)?.let { icon ->
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

/**
 * Persists the arrangement after a card is dropped at [index]
 *
 * Dropping a card inside the favourites block favourites it and dragging one out clears it
 */
private fun commitDrop(entries: List<ModGridEntry>, index: Int) {
    val dropped = (entries.getOrNull(index) as? ModGridEntry.Card)?.data ?: return
    val bounds = modGroupBounds(entries, index)
    val group = entries.slice(bounds).filterIsInstance<ModGridEntry.Card>().map { it.data }
    val position = index - bounds.first
    val favoritesElsewhere = group.filterIndexed { i, _ -> i != position }.count { ModFavorites.isFavorite(it.id) }
    if ((position < favoritesElsewhere) != ModFavorites.isFavorite(dropped.id)) ModFavorites.toggle(dropped.id)
    ModOrder.reorder(
        group.map { it.id },
        ConfigRegistry.modCardConfigs.sortedWith(modCardOrder()).map { it.id },
    )
}

private val ModCardFooterHeight = 36.dp

private val FavoriteStarColor = Color(0xFFFFD700)

@Composable
fun ModCard(mod: ConfigData, modifier: Modifier = Modifier) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current

    Box(
        modifier = modifier.fillMaxWidth().height(140.dp)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), theme.modCardShape
            )
            .onClick(interactionSource) {
                val onOpen = mod.onOpen
                when {
                    onOpen != null -> onOpen()
                    mod.source == ConfigSource.OC -> LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
                }
            }
            .clip(theme.modCardShape)
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val preview = mod.preview
                val icon = mod.icon?.takeIf(::canRenderIcon)
                if (preview != null) {
                    preview(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp))
                } else if (icon != null) {
                    Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
                } else {
                    Text(
                        mod.title.asRenderText(),
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
                    val gradient = Brush.radialGradient(
                        colors = listOf(
                            vignetteColor.copy(alpha = 0f),
                            vignetteColor.copy(alpha = 0.04f),
                            vignetteColor.copy(alpha = 0.08f)
                        ),
                        center = size.center,
                        radius = size.minDimension * 0.9f
                    )
                    onDrawBehind { drawRect(gradient) }
                }
            )
            val gradient = Brush.verticalGradient(
                0f to Accent.copy(0f),
                0.4f to Accent.copy(0.2f),
                1f to Accent.copy(0.4f),
            )

            Box(
                Modifier.align(Alignment.BottomCenter).height(50.dp).fillMaxWidth().drawWithCache {
                    onDrawBehind { drawRect(gradient) }
                }
            )
        }

        FavoriteStar(
            mod = mod,
            cardHovered = interactionSource.collectIsHoveredAsState().value,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@Composable
private fun FavoriteStar(mod: ConfigData, cardHovered: Boolean, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val favorite = ModFavorites.isFavorite(mod.id)
    val interactionSource = rememberInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val alpha by animateFloatAsState(
        when {
            favorite || hovered -> 1f
            cardHovered -> 0.6f
            else -> 0f
        }
    )
    val color by animateColorAsState(if (favorite) FavoriteStarColor else theme.textColor)

    Box(
        modifier = modifier
            .padding(4.dp)
            .size(24.dp)
            .alpha(alpha)
            .onClick(interactionSource) { ModFavorites.toggle(mod.id) }
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (favorite) "star-filled" else "star", color = color, modifier = Modifier.size(18.dp))
    }
}
