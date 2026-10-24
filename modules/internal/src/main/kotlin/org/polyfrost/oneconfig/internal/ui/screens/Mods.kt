package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.animation.core.springStep
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.InteractionSource
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.ColumnScope
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.render.PolyFade
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.LinearGradient
import org.polyfrost.compose.ui.graphics.RadialGradient
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.graphicsLayer
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.api.ui.v1.ModsPageDecorator
import org.polyfrost.oneconfig.api.ui.v1.ModsPageDecorators
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.api.ModCardTypeCollapseStore
import org.polyfrost.oneconfig.internal.ui.api.ModFavorites
import org.polyfrost.oneconfig.internal.ui.api.ModGridEntry
import org.polyfrost.oneconfig.internal.ui.api.ModHidden
import org.polyfrost.oneconfig.internal.ui.api.ModOrder
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories
import org.polyfrost.oneconfig.internal.ui.api.buildModGridEntries
import org.polyfrost.oneconfig.internal.ui.api.modCardOrder
import org.polyfrost.oneconfig.internal.ui.api.modFavoriteAt
import org.polyfrost.oneconfig.internal.ui.api.modGroupBounds
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.background
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberGridReorderState
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.rememberModEnabled
import org.polyfrost.oneconfig.internal.ui.components.reorderContainer
import org.polyfrost.oneconfig.internal.ui.components.reorderOverlay
import org.polyfrost.oneconfig.internal.ui.components.reorderableItem
import org.polyfrost.oneconfig.internal.ui.hud.HudModCardData
import org.polyfrost.oneconfig.internal.ui.hud.components.hudPreviewScale
import org.polyfrost.oneconfig.internal.ui.hud.components.rememberHudPreview
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.shell.ShellHost
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.AccentEnd
import org.polyfrost.oneconfig.internal.ui.themes.AccentGradient
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
fun Mods(host: ShellHost) {
    Box(Modifier.fillMaxSize()) {
        DecoratedModsPage(ModsPageDecorators.all(), 0, host)
        ModSortControl(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun DecoratedModsPage(decorators: List<ModsPageDecorator>, index: Int, host: ShellHost) {
    val decorator = decorators.getOrNull(index) ?: return ModsPage(host)
    decorator.Decorate { DecoratedModsPage(decorators, index + 1, host) }
}

@Composable
private fun ModsPage(host: ShellHost) {
    var activeCategory by remember { mutableStateOf(ModCategory.All) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = GUTTER.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val cards = ConfigRegistry.modCardConfigs
            ModCategory.entries.forEach {
                val shown = when {
                    it == ModCategory.All || it.favoritesOnly || activeCategory == it -> true
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
        ModsGrid(activeCategory, host)
    }
}

@Composable
fun ColumnScope.ModsGrid(category: ModCategory, host: ShellHost) {
    val registryRevision = ConfigRegistry.revision
    val hudRevision = HudManager.revision
    val categoryRevision = ThirdPartyModCategories.revision
    val favoriteRevision = ModFavorites.revision
    val hiddenRevision = ModHidden.revision
    val orderRevision = ModOrder.revision
    val sortMode = ModSortStore.selectedId
    val typeRevision = ModCardTypes.revision
    val collapseRevision = ModCardTypeCollapseStore.revision
    val filtered = remember(
        registryRevision, hudRevision, category, categoryRevision, favoriteRevision, hiddenRevision, orderRevision,
        typeRevision, sortMode,
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
    if (filtered.isEmpty()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (category.favoritesOnly || category.hiddenOnly) {
                Text(
                    if (category.hiddenOnly) "No hidden mods." else "No favorite mods.",
                    color = LocalTheme.current.textColorSecondary,
                )
            }
        }
        return
    }

    // mutated live while dragging so the grid re-lays out under the pointer
    val flat = remember(filtered, collapseRevision) {
        buildModGridEntries(filtered, ModCardTypeCollapseStore::isCollapsed)
    }
    val entries = remember(flat) { flat.toMutableStateList() }

    val gridState = rememberRestorableLazyListState("mods")
    val density = LocalDensity.current
    val placement = remember(entries, density.density) { Placement() }
    val reorderState = rememberGridReorderState(
        gridState = gridState,
        items = entries,
        key = { it.key },
        onMove = { from, to -> entries.add(to, entries.removeAt(from)) },
        onDrop = { index -> commitDrop(entries, index) },
        dragBounds = { index -> modGroupBounds(entries, index) },
        onClick = { key -> entries.cardData(key)?.let { openModCard(it, host) } },
    )
    val dropFavorite = reorderState.draggingKey
        ?.let { key -> entries.indexOfFirst { it.key == key } }
        ?.takeIf { it >= 0 }
        ?.let { index -> modFavoriteAt(entries, index, ModFavorites::isFavorite) }

    val overlayActive = reorderState.overlayKey != null
    DisposableEffect(overlayActive) {
        ShellState.modDragging = overlayActive
        onDispose { if (overlayActive) ShellState.modDragging = false }
    }

    LaunchedEffect(placement.offsets.isNotEmpty()) {
        var last = withFrameNanos { it }
        while (placement.offsets.isNotEmpty()) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1e9f).coerceIn(0f, 0.05f)
            last = now
            for (k in placement.offsets.keys.toList()) {
                val o = placement.offsets[k] ?: continue
                val v = placement.velocity.getOrPut(k) { FloatArray(2) }
                val sx = springStep(o[0], v[0], 20f, dt)
                val sy = springStep(o[1], v[1], 20f, dt)
                v[0] = sx[1]
                v[1] = sy[1]
                if (abs(sx[0]) < 0.5f && abs(sy[0]) < 0.5f) {
                    placement.offsets.remove(k)
                    placement.velocity.remove(k)
                } else {
                    placement.offsets[k] = floatArrayOf(sx[0], sy[0])
                }
            }
        }
    }

    val rows = gridRows(entries)
    ScrollbarBox(
        gridState,
        GUTTER.dp,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .reorderContainer(reorderState),
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = GUTTER.dp)
                .onPlaced {
                    reorderState.viewportStart = it.y
                    reorderState.viewportEnd = it.y + it.height
                },
            state = gridState,
            verticalArrangement = Arrangement.spacedBy(ModGridGap),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is GridRow.Header -> ModTypeHeader(
                        row.entry,
                        onToggle = { ModCardTypeCollapseStore.setCollapsed(row.entry.type.id, row.entry.expanded) },
                    )

                    is GridRow.Cards -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ModGridGap)) {
                        for (entry in row.cards) key(entry.key) {
                            val mod = entry.data
                            val dragging = reorderState.draggingKey == mod.id
                            val lifted = reorderState.overlayKey == mod.id
                            val favoriteSlot = if (dragging) dropFavorite == true else ModFavorites.isFavorite(mod.id)
                            val offset = placement.offsets[mod.id]
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(ModCardHeight)
                                    .onPlaced { placement.placed(mod.id, it.x, it.y + gridState.scroll.value) }
                                    .graphicsLayer(
                                        translationX = if (lifted) 0f else offset?.get(0) ?: 0f,
                                        translationY = if (lifted) 0f else offset?.get(1) ?: 0f,
                                    ),
                            ) {
                                if (lifted) DropSlotOutline(favoriteSlot, dragging)
                                ModCard(
                                    mod,
                                    host,
                                    modifier = Modifier.reorderableItem(reorderState, mod.id),
                                    hoverHint = reorderState.overlayKey == null,
                                    clickToOpen = false,
                                )
                            }
                        }
                        repeat(MOD_GRID_COLUMNS - row.cards.size) { Box(Modifier.weight(1f).height(ModCardHeight)) }
                    }
                }
            }
        }
        // drawn over the grid so it passes above the other cards
        val draggedId = reorderState.overlayKey
        val dragged = remember(entries, draggedId) { draggedId?.let(entries::cardData) }
        if (dragged != null) {
            Box(Modifier.reorderOverlay(reorderState)) {
                ModCard(
                    dragged,
                    host,
                    favorite = dropFavorite ?: ModFavorites.isFavorite(dragged.id),
                    interactive = false,
                )
            }
        }
    }
}

@Composable
private fun ModTypeHeader(entry: ModGridEntry.Header, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val chevronRotation by animateFloatAsState(if (entry.expanded) 180f else 90f, spring(visibilityThreshold = 0.5f))

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
        Icon("up", theme.textColorSecondary, Modifier.size(12.dp), rotation = chevronRotation)
        Rule(Modifier.weight(1f))
    }
}

@Composable
private fun Rule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(LocalTheme.current.borderColor))
}

private sealed class GridRow {
    abstract val key: Any

    class Header(val entry: ModGridEntry.Header) : GridRow() {
        override val key get() = entry.key
    }

    class Cards(val cards: List<ModGridEntry.Card>, val n: Int) : GridRow() {
        override val key get() = "row$n"
    }
}

private fun gridRows(entries: List<ModGridEntry>): List<GridRow> {
    val rows = ArrayList<GridRow>()
    var run = ArrayList<ModGridEntry.Card>()
    fun flush() {
        if (run.isNotEmpty()) rows.add(GridRow.Cards(run, rows.size))
        run = ArrayList()
    }
    for (entry in entries) when (entry) {
        is ModGridEntry.Header -> {
            flush()
            rows.add(GridRow.Header(entry))
        }
        is ModGridEntry.Card -> {
            run.add(entry)
            if (run.size == MOD_GRID_COLUMNS) flush()
        }
    }
    flush()
    return rows
}

private class Placement {
    val last = HashMap<Any, FloatArray>()
    val offsets = mutableStateMapOf<Any, FloatArray>()
    val velocity = HashMap<Any, FloatArray>()

    fun placed(key: Any, x: Float, contentY: Float) {
        val prev = last.put(key, floatArrayOf(x, contentY)) ?: return
        val dx = prev[0] - x
        val dy = prev[1] - contentY
        if (abs(dx) < 0.5f && abs(dy) < 0.5f) return
        val cur = offsets[key]
        offsets[key] = floatArrayOf(dx + (cur?.get(0) ?: 0f), dy + (cur?.get(1) ?: 0f))
    }
}

private fun List<ModGridEntry>.cardData(key: Any): ConfigData? =
    firstNotNullOfOrNull { (it as? ModGridEntry.Card)?.data?.takeIf { mod -> mod.id == key } }

private fun openModCard(mod: ConfigData, host: ShellHost) {
    val onOpen = mod.onOpen
    when {
        onOpen != null -> onOpen()
        mod.source == ConfigSource.OC -> host.openModConfig(mod, null)
    }
}

/** Persists the arrangement and the favorite state after a card is dropped at [index] */
private fun commitDrop(entries: List<ModGridEntry>, index: Int) {
    val dropped = (entries.getOrNull(index) as? ModGridEntry.Card)?.data ?: return
    if (modFavoriteAt(entries, index, ModFavorites::isFavorite) != ModFavorites.isFavorite(dropped.id)) {
        ModFavorites.toggle(dropped.id)
    }
    if (ModSortStore.alphabetical) return
    val group = entries.slice(modGroupBounds(entries, index)).filterIsInstance<ModGridEntry.Card>().map { it.data }
    ModOrder.reorder(
        group.map { it.id },
        ConfigRegistry.modCardConfigs.sortedWith(modCardOrder()).map { it.id },
    )
}

/** Dashed outline on the dragged card's slot, in the favorite color while it would land as a [favorite] */
@Composable
private fun DropSlotOutline(favorite: Boolean, dragging: Boolean) {
    val theme = LocalTheme.current
    val radius = theme.modCardShape.radius
    val color = if (favorite) theme.favoriteColor else theme.textColorSecondary
    val alpha by animateFloatAsState(if (dragging) 1f else 0f, initialValue = 0f)
    Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
        if (alpha == 0f) return@Canvas
        rect(x, y, w, h, color.withAlpha(0.08f * alpha), radius)
        dashedRRect(x, y, w, h, radius, color.withAlpha(0.6f * alpha), 1.5f, 6f, 4f)
    }
}

private fun RenderContext.dashedRRect(x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, stroke: Float, on: Float, off: Float) {
    val rr = min(r, min(w, h) / 2f)
    val pts = ArrayList<Float>()
    fun corner(cx: Float, cy: Float, from: Float) {
        for (i in 0..8) {
            val t = Math.toRadians((from + 90.0 * i / 8)).toFloat()
            pts += cx + rr * cos(t)
            pts += cy + rr * sin(t)
        }
    }
    corner(x + w - rr, y + rr, 270f)
    corner(x + w - rr, y + h - rr, 0f)
    corner(x + rr, y + h - rr, 90f)
    corner(x + rr, y + rr, 180f)
    pts += pts[0]
    pts += pts[1]
    var drawing = true
    var left = on
    for (i in 0 until pts.size / 2 - 1) {
        val ax = pts[i * 2]
        val ay = pts[i * 2 + 1]
        val dx = pts[i * 2 + 2] - ax
        val dy = pts[i * 2 + 3] - ay
        val len = hypot(dx, dy)
        var t = 0f
        while (t < len) {
            val step = min(left, len - t)
            if (drawing) line(ax + dx * t / len, ay + dy * t / len, ax + dx * (t + step) / len, ay + dy * (t + step) / len, color, stroke)
            t += step
            left -= step
            if (left <= 0f) {
                drawing = !drawing
                left = if (drawing) on else off
            }
        }
    }
}

private const val MOD_GRID_COLUMNS = 4

private val ModGridGap = 19.dp

private val ModCardHeight = 140.dp

private val ModCardFooterHeight = 36.dp

private val ModCardGlowHeight = 50.dp

private val ModCardToggleDot = 6.dp
private val ModCardToggleRing = 12.dp
private val ModCardToggleGlow = 5.dp
private val ModCardToggleUnavailable = Color(0xFF4F505B)

// the accent is darkened towards this for the ring, which for the default accent gives the ring of the design
private val ModCardToggleRingShade = Color(0xFF181600)

private fun Shape.cornerFor(size: Float): Float = if (percent >= 0f) size * percent / 100f else min(radius, size / 2f)

@Composable
fun ModCard(
    mod: ConfigData,
    host: ShellHost,
    modifier: Modifier = Modifier,
    favorite: Boolean = ModFavorites.isFavorite(mod.id),
    hoverHint: Boolean = true,
    interactive: Boolean = true,
    clickToOpen: Boolean = true,
) {
    val interactionSource = rememberInteractionSource()
    val footerInteractions = rememberInteractionSource()
    val theme = LocalTheme.current

    val toggleRevision = ModToggles.revision
    val toggle = remember(mod, toggleRevision) { mod.toggle }
    var enabled by rememberModEnabled(toggle)
    val footerAccent by animateFloatAsState(if (enabled) 1f else 0f)
    val footerTextColor = animateColorAsState(
        if (enabled) theme.accentTextColor else theme.textColor.copy(alpha = 0.75f)
    )
    val glowStrength = animateFloatAsState(if (enabled) 1f else 0f)
    val open = {
        val onOpen = mod.onOpen
        when {
            onOpen != null -> onOpen()
            mod.source == ConfigSource.OC -> host.openModConfig(mod, null)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(ModCardHeight)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, remember(theme.borderColor) {
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
            }, theme.modCardShape)
            .then(
                if (interactive) {
                    Modifier
                        .then(
                            if (clickToOpen) Modifier.onClick(interactionSource) { openModCard(mod, host) }
                            else Modifier.hoverable(interactionSource),
                        )
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
                val icon = remember(mod.icon) { mod.icon?.takeIf(::canRenderIcon) }
                if (mod is HudModCardData && mod.hud !is LegacyHudMarker) {
                    HudCardPreview(
                        LocalDensity.current.density / host.surfaceRatio,
                        mod.hud,
                        mod.title.asRenderText(),
                        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                } else if (icon != null) {
                    Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
                } else {
                    Text(
                        mod.title,
                        color = theme.textColor,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                        softWrap = true,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = ModCardFooterHeight)
                    .background(theme.textColor.copy(alpha = 0.08f * (1f - footerAccent)))
                    .background(AccentBrush, alpha = footerAccent)
                    .then(
                        if (toggle != null && interactive) {
                            Modifier
                                .onClick(footerInteractions) {
                                    if (!enabled && toggle.needsSetup()) {
                                        open()
                                    } else {
                                        toggle.setEnabled(!enabled)
                                        enabled = toggle.isEnabled()
                                    }
                                }
                                .pointerHoverIcon(PointerIcon.Hand)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    mod.title,
                    color = footerTextColor,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (LocalTheme.current.shadowEnabled) {
            val vignetteColor = theme.textColor
            val glowColor = Accent
            val glowEnd = AccentEnd
            val glowGradient = AccentGradient
            val radius = theme.modCardShape.radius
            Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
                val vignette = RadialGradient(
                    listOf(
                        vignetteColor.copy(alpha = 0f),
                        vignetteColor.copy(alpha = 0.04f),
                        vignetteColor.copy(alpha = 0.08f)
                    ),
                    radius = 0.9f
                )
                rectGradient(x, y, w, h, vignette, radius)
            }
            Canvas(Modifier.fillMaxSize().alpha(glowStrength, if (enabled) 1f else 0f)) { x, y, w, h ->
                val glowHeight = min(ModCardGlowHeight.value, h)
                if (!glowGradient) {
                    val glow = LinearGradient(
                        listOf(glowColor.copy(0f), glowColor.copy(0.1f), glowColor.copy(0.2f)),
                        listOf(0f, 0.4f, 1f),
                        angle = 90f,
                    )
                    rectGradient(x, y + h - glowHeight, w, glowHeight, glow, radius)
                } else {
                    val top = y + h - glowHeight
                    saveLayerFade(PolyFade(true, floatArrayOf(top, top + glowHeight * 0.4f, y + h), floatArrayOf(0f, 0.5f, 1f)))
                    rectGradient(x, top, w, glowHeight, Brush.horizontalGradient(glowColor.copy(alpha = 0.2f), glowEnd.copy(alpha = 0.2f)), radius)
                    restore()
                }
            }
        }
        Row(Modifier.padding(4.dp)) {
            FavoriteStar(
                mod = mod,
                favorite = favorite,
                cardInteractions = interactionSource,
                hoverHint = hoverHint,
                interactive = interactive,
            )
            HideToggle(
                mod = mod,
                cardInteractions = interactionSource,
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(11.dp)) {
            TooltipArea(
                tooltip = {
                    val shape = RoundedCornerShape(8.dp)
                    Box(
                        Modifier
                            .background(theme.popupBackground, shape)
                            .border(1.dp, theme.borderColor, shape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (toggle != null) "This mod can be toggled on and off." else "This mod doesn't support being toggled.",
                            color = theme.textColor,
                            fontSize = 12.sp,
                        )
                    }
                },
            ) {
                val accent = Accent
                val accentEnd = AccentEnd
                val circle = theme.circleShape
                Canvas(Modifier.size(ModCardToggleRing)) { x, y, _, _ ->
                    val ringRadius = ModCardToggleRing.value / 2f
                    val cx = x + ringRadius
                    val cy = y + ringRadius
                    val ringStart = if (toggle != null) accent.lerp(ModCardToggleRingShade, 0.42f) else ModCardToggleUnavailable.withAlpha(0.5f)
                    val ringEnd = if (toggle != null) accentEnd.lerp(ModCardToggleRingShade, 0.42f) else ringStart
                    val ring = ringStart.lerp(ringEnd, 0.5f)
                    if (toggle != null) {
                        val glowRadius = (ModCardToggleRing / 2 + ModCardToggleGlow).value
                        val inner = ringRadius / glowRadius
                        val glow = RadialGradient(
                            listOf(ring.withAlpha(0.41f), ring.withAlpha(0.25f), ring.withAlpha(0.09f), ring.withAlpha(0)),
                            listOf(inner, inner + (1f - inner) * 0.4f, inner + (1f - inner) * 0.8f, 1f),
                        )
                        rectGradient(cx - glowRadius, cy - glowRadius, glowRadius * 2f, glowRadius * 2f, glow, glowRadius)
                    }
                    val ringSize = ringRadius * 2f
                    rectGradient(cx - ringRadius, cy - ringRadius, ringSize, ringSize, Brush.linearGradient(listOf(ringStart, ringEnd)), circle.cornerFor(ringSize))
                    val dotRadius = ModCardToggleDot.value / 2f
                    val dotSize = dotRadius * 2f
                    if (toggle != null) {
                        rectGradient(cx - dotRadius, cy - dotRadius, dotSize, dotSize, Brush.linearGradient(listOf(accent, accentEnd)), circle.cornerFor(dotSize))
                    } else {
                        rect(cx - dotRadius, cy - dotRadius, dotSize, dotSize, ModCardToggleUnavailable, circle.cornerFor(dotSize))
                    }
                }
            }
        }
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
    val targetAlpha = when {
        favorite -> 1f
        !hoverHint -> 0f
        hovered -> 1f
        cardHovered -> 0.6f
        else -> 0f
    }
    val alpha = animateFloatAsState(targetAlpha)
    val color = animateColorAsState(if (favorite) theme.favoriteColor else theme.textColor)

    Box(
        modifier = modifier
            .size(24.dp)
            .alpha(alpha, targetAlpha)
            .then(
                if (interactive) {
                    Modifier
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
    val targetAlpha = when {
        hidden || hovered -> 1f
        cardHovered -> 0.6f
        else -> 0f
    }
    val alpha = animateFloatAsState(targetAlpha)

    Box(
        modifier = modifier
            .size(24.dp)
            .alpha(alpha, targetAlpha)
            .onClick(interactionSource) { ModHidden.toggle(mod.id) }
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (hidden) "eye-off" else "eye", color = LocalTheme.current.textColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun HudCardPreview(pxPerDp: Float, hud: Hud, title: String, modifier: Modifier) {
    val state = rememberHudPreview(hud)
    @Suppress("UNUSED_VARIABLE") val revision = HudManager.previewRevision.intValue
    if (!state.ready) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(title, color = LocalTheme.current.textColor, fontSize = 16.sp)
        }
        return
    }
    val nw = state.naturalWidth
    val nh = state.naturalHeight
    val draw: RenderContext.(Float, Float, Float, Float) -> Unit = { x, y, w, h ->
        val s = hudPreviewScale(nw, nh, w * pxPerDp, h * pxPerDp) / pxPerDp
        save()
        clipRect(x, y, w, h)
        translate(x + (w - nw * s) / 2f, y + (h - nh * s) / 2f)
        scale(s, s)
        Snapshot.withoutReadObservation { state.runtime.root.render(this) }
        restore()
    }
    if (hud.alwaysRedraw) Canvas(modifier, live = true, onDraw = draw) else Canvas(modifier, onDraw = draw)
}
