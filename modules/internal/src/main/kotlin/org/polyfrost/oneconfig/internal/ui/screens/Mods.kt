package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositeShader
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.ModCardTypes
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.api.ui.v1.ModsPageDecorator
import org.polyfrost.oneconfig.api.ui.v1.ModsPageDecorators
import org.polyfrost.oneconfig.api.ui.v1.ReadOnlyModToggle
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonStyle
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.ModCardMenuContributors
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.hiddenUntilHover
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.api.ModCardTypeCollapseStore
import org.polyfrost.oneconfig.internal.ui.api.ModFavorites
import org.polyfrost.oneconfig.internal.ui.api.ModGridEntry
import org.polyfrost.oneconfig.internal.ui.api.ModHidden
import org.polyfrost.oneconfig.internal.ui.api.ModOrder
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.api.ThirdPartyModCategories
import org.polyfrost.oneconfig.internal.ui.api.buildModGridEntries
import org.polyfrost.oneconfig.internal.ui.api.modCardOrder
import org.polyfrost.oneconfig.internal.ui.api.modFavoriteAt
import org.polyfrost.oneconfig.internal.ui.api.modGroupBounds
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.CollapsibleHeader
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.accentSheen
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.rememberGridReorderState
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.rememberModEnabled
import org.polyfrost.oneconfig.internal.ui.components.reorderContainer
import org.polyfrost.oneconfig.internal.ui.components.reorderOverlay
import org.polyfrost.oneconfig.internal.ui.components.reorderableItem
import org.polyfrost.oneconfig.internal.ui.components.settings.identitySet
import org.polyfrost.oneconfig.internal.ui.components.settings.resetOption
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.ShellNavigation
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyGridState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.AccentEnd
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.internal.ui.themes.cornerRadiusOrNull
import org.polyfrost.oneconfig.internal.ui.themes.contrastRatio
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper

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

    val label: String get() = translated("oneconfig.mods.category.${name.lowercase()}", title)
}

@Composable
fun Mods() {
    Box {
        DecoratedModsPage(ModsPageDecorators.all(), 0)
        ModSortControl(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun DecoratedModsPage(decorators: List<ModsPageDecorator>, index: Int) {
    val decorator = decorators.getOrNull(index) ?: return ModsPage()
    decorator.Decorate { DecoratedModsPage(decorators, index + 1) }
}

@Composable
private fun ModsPage() {
    var activeCategory by remember { mutableStateOf(ModCategory.All) }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = PageGutter),
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
                        label = it.label,
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
    if (filtered.isEmpty() && (category.favoritesOnly || category.hiddenOnly)) {
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (category.hiddenOnly) {
                EmptyState(
                    translated("oneconfig.mods.empty.hidden", "No hidden mods"),
                    icon = "eye-off",
                    text = translated("oneconfig.mods.empty.hidden.text", "Mods you hide from their card show up here."),
                )
            } else {
                EmptyState(
                    translated("oneconfig.mods.empty.favorites", "No favorite mods"),
                    icon = "star",
                    text = translated("oneconfig.mods.empty.favorites.text", "Star a mod on its card to keep it here."),
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

    val gridState = rememberRestorableLazyGridState("mods")
    val navigation = LocalShell.current.navigation
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    // Only enable placement animations after the grid has been laid out for the current size
    var animateItems by remember(windowSize, density) { mutableStateOf(false) }
    val reorderState = rememberGridReorderState(
        gridState = gridState,
        onMove = { from, to -> entries.add(to, entries.removeAt(from)) },
        onDrop = { index -> commitDrop(entries, index) },
        dragBounds = { index -> modGroupBounds(entries, index) },
        onClick = { key -> entries.cardData(key)?.let { openModCard(it, navigation) } },
        horizontalPadding = PageGutter,
    )
    val dropFavorite = reorderState.draggingKey
        ?.let { key -> entries.indexOfFirst { it.key == key } }
        ?.takeIf { it >= 0 }
        ?.let { index -> modFavoriteAt(entries, index, ModFavorites::isFavorite) }
    // the dragged card's index no longer applies once the list is rebuilt
    DisposableEffect(entries) {
        onDispose { reorderState.cancel() }
    }

    // The overlay card is drawn as a sibling of the grid, but the page pane that wraps the nav host
    // clips its content, so it would be cut off at the pane edge. Drop that clip for as long as an
    // overlay card exists (dragging and the settle-back animation) so it can be seen in full.
    val overlayActive = reorderState.overlayKey != null
    val layout = LocalShell.current.layout
    DisposableEffect(overlayActive) {
        layout.markModDragging(overlayActive)
        onDispose { if (overlayActive) layout.markModDragging(false) }
    }

    // override scroll to stay in place instead of following the first visible item
    DisposableEffect(favoriteRevision) {
        gridState.requestScrollToItem(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        onDispose { }
    }

    val focusManager = LocalFocusManager.current
    ScrollbarBox(
        gridState,
        PageGutter,
        modifier = Modifier
            .weight(1f)
            // see overlayActive above: the overlay card has to be able to leave the grid while it exists
            .then(if (overlayActive) Modifier else Modifier.clipToBounds())
            // cards and their buttons consume their clicks, so only a click on empty space unfocuses the search field
            .pointerInput(focusManager) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (waitForUpOrCancellation() != null) focusManager.clearFocus()
                }
            }
            .reorderContainer(reorderState),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(19.dp),
            horizontalArrangement = Arrangement.spacedBy(19.dp),
            contentPadding = PaddingValues(horizontal = PageGutter),
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
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
                        modifier = if (animateItems) Modifier.animateItem(placementSpec = Motion.itemPlacementSpec()) else Modifier,
                        onToggle = { ModCardTypeCollapseStore.toggle(entry.type.id) },
                    )

                    is ModGridEntry.Card -> {
                        val mod = entry.data
                        val dragging = reorderState.draggingKey == mod.id
                        val outlineAlpha = Motion.animateFloatAsState(if (dragging) 1f else 0f)
                        val favoriteSlot = if (dragging) dropFavorite == true else ModFavorites.isFavorite(mod.id)
                        ModCard(
                            mod,
                            modifier = Modifier
                                .then(
                                    if (animateItems) {
                                        Modifier.animateItem(
                                            placementSpec = if (dragging) null else Motion.itemPlacementSpec(),
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
    val title = localizedLabel(entry.type.title) ?: entry.type.title

    CollapsibleHeader(
        label = title,
        expanded = entry.expanded,
        onToggle = onToggle,
        modifier = modifier,
        trailing = { Rule(Modifier.weight(1f)) },
    ) {
        Rule(Modifier.weight(1f))
        remember(entry.type.icon) { entry.type.icon?.takeIf(::canRenderIcon) }?.let { icon ->
            Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
        }
        Text(
            title,
            color = theme.textColorSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        if (!entry.expanded && entry.cardCount > 0) {
            Text("(${entry.cardCount})", color = theme.textColorSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Rule(modifier: Modifier) {
    Box(modifier.height(1.dp).background(LocalTheme.current.borderColor))
}

private fun List<ModGridEntry>.cardData(key: Any): ConfigData? =
    firstNotNullOfOrNull { (it as? ModGridEntry.Card)?.data?.takeIf { mod -> mod.id == key } }

private fun openModCard(mod: ConfigData, navigation: ShellNavigation) {
    val onOpen = mod.onOpen
    when {
        onOpen != null -> onOpen()
        mod.source == ConfigSource.OC -> navigation.navigate(ModConfigRoute(mod.id))
    }
}

private fun modCardMenu(mod: ConfigData, navigation: ShellNavigation): List<MenuEntry> {
    val toggle = mod.toggle
    val favorite = ModFavorites.isFavorite(mod.id)
    val hidden = ModHidden.isHidden(mod.id)
    val tree = ConfigRegistry.findTree(mod.id)
    val resettable = mod.source == ConfigSource.OC && tree != null && tree.getMetadata<Boolean>("ui_only") != true
    val hasHud = HudManager.providers().any { it.configId == mod.id } || HudManager.instances().any { it.configId == mod.id }
    val contributed = ModCardMenuContributors.entriesFor(mod.id)
    return buildList {
        add(
            MenuAction(
                translated("oneconfig.mods.menu.open", "Open settings"),
                { openModCard(mod, navigation) },
                icon = "settings",
                enabled = mod.onOpen != null || mod.source == ConfigSource.OC,
            ),
        )
        add(
            MenuAction(
                if (favorite) translated("oneconfig.mods.menu.unfavorite", "Remove from favorites")
                else translated("oneconfig.mods.menu.favorite", "Add to favorites"),
                { ModFavorites.toggle(mod.id) },
                icon = if (favorite) "star-filled" else "star",
            ),
        )
        add(
            MenuAction(
                if (hidden) translated("oneconfig.mods.menu.show", "Show mod") else translated("oneconfig.mods.menu.hide", "Hide mod"),
                { ModHidden.toggle(mod.id) },
                icon = if (hidden) "eye" else "eye-off",
            ),
        )
        if (toggle != null) {
            val on = toggle.isEnabled()
            add(
                MenuAction(
                    if (on) translated("oneconfig.mods.menu.disable", "Disable") else translated("oneconfig.mods.menu.enable", "Enable"),
                    {
                        if (!on && toggle.needsSetup()) openModCard(mod, navigation) else toggle.setEnabled(!on)
                    },
                    icon = if (on) "x-circle" else "check-circle",
                    enabled = toggle !is ReadOnlyModToggle,
                    disabledReason = (toggle as? ReadOnlyModToggle)?.lockedReason?.takeIf { it.isNotBlank() }
                        ?: translated("oneconfig.mods.toggle.locked", "This mod is switched on and off somewhere else."),
                ),
            )
        }
        if (hasHud || resettable) add(MenuSeparator)
        if (hasHud) {
            add(MenuAction(translated("oneconfig.mods.menu.hud_editor", "Open HUD editor"), { HudManager.openEditor() }, icon = "hud"))
        }
        if (resettable) {
            add(
                MenuAction(
                    translated("oneconfig.mods.menu.reset", "Reset to defaults"),
                    { confirmReset(mod, tree) },
                    icon = "refresh",
                    danger = true,
                ),
            )
        }
        add(MenuSeparator)
        add(MenuAction(translated("oneconfig.mods.menu.copy_id", "Copy mod id"), { ClipboardHelper.setString(mod.id) }, icon = "copy", detail = mod.id))
        if (contributed.isNotEmpty()) {
            add(MenuSeparator)
            addAll(contributed)
        }
    }
}

private fun confirmReset(mod: ConfigData, tree: Tree) {
    val ownSwitch = (mod.toggle as? PropertyModToggle)?.property
    Confirm.ask(
        title = translated("oneconfig.mods.menu.reset.title", "Reset %s?", mod.title.asRenderText()),
        text = translated("oneconfig.mods.menu.reset.text", "Every setting of this mod goes back to its default. This can't be undone."),
        confirmLabel = translated("oneconfig.mods.menu.reset.confirm", "Reset settings"),
        onConfirm = { resetTree(tree, identitySet().also { seen -> ownSwitch?.let(seen::add) }) },
        icon = "refresh",
    )
}

private fun resetTree(tree: Tree, seen: MutableSet<Property<*>>) {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> resetOption(node, seen)
            is Tree -> resetTree(node, seen)
        }
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

private val ModCardInsetFooterInset = 6.dp

private val ModCardToggleDot = 6.dp
private val ModCardToggleRing = 12.dp
private val ModCardToggleGlow = 5.dp

private const val ModCardToggleRingDarken = 0.42f
private val ModCardToggleRingShade = Color(0xFF181600)

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
    val footerInteractions = rememberInteractionSource()
    val theme = LocalTheme.current

    val toggleRevision = ModToggles.revision
    val toggle = remember(mod, toggleRevision) { mod.toggle }
    var enabled by rememberModEnabled(toggle)
    val locked = toggle is ReadOnlyModToggle
    if (toggle is ReadOnlyModToggle) {
        LaunchedEffect(toggle) {
            while (true) {
                withFrameMillis { enabled = toggle.isEnabled() }
            }
        }
    }
    val footerAccent by Motion.animateFloatAsState(if (enabled) 1f else 0f)
    val insetFooter = theme.insetModFooter
    val insetFooterShape = theme.modCardShape.concentric(ModCardInsetFooterInset)
    val footerTextColor by Motion.animateColorAsState(
        if (enabled) (if (insetFooter && contrastRatio(theme.textColor, Accent) >= 3f) theme.textColor else theme.accentTextColor) else theme.textColor.copy(alpha = 0.75f)
    )
    val glowStrength by Motion.animateFloatAsState(if (enabled) 0.5f else 0f)
    val navigation = LocalShell.current.navigation
    val open = {
        val onOpen = mod.onOpen
        when {
            onOpen != null -> onOpen()
            mod.source == ConfigSource.OC -> navigation.navigate(ModConfigRoute(mod.id))
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().height(140.dp)
            .hoverGlow(theme.modCardShape)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, remember(theme.borderColor) {
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
            }, theme.modCardShape)
            .then(
                if (interactive) {
                    // cards never take focus, otherwise they slide in from off screen when scrolled back into view
                    Modifier
                        .focusProperties { canFocus = false }
                        .contextMenu { modCardMenu(mod, navigation) }
                        .oneConfigClickable(mod.title.asRenderText(), { openModCard(mod, navigation) }, indication = false, interactionSource = interactionSource)
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
                val preview = if (LocalShell.current.warming) null else mod.preview
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
                    .then(
                        if (insetFooter) {
                            Modifier
                                .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
                                .glassHover(insetFooterShape, pull = 0.75.dp)
                                .liquidGlass(insetFooterShape)
                                .clip(insetFooterShape)
                        } else {
                            Modifier
                        },
                    )
                    .fillMaxWidth()
                    .heightIn(min = ModCardFooterHeight)
                    .background(theme.textColor.copy(alpha = 0.08f * (1f - footerAccent)))
                    .background(AccentBrush, alpha = footerAccent)
                    .accentSheen(alpha = footerAccent)
                    .then(
                        if (toggle != null && interactive && !locked) {
                            Modifier
                                .focusProperties { canFocus = false }
                                .oneConfigClickable(
                                    mod.title.asRenderText(),
                                    {
                                        if (!enabled && toggle.needsSetup()) {
                                            open()
                                        } else {
                                            toggle.setEnabled(!enabled)
                                            enabled = toggle.isEnabled()
                                        }
                                    },
                                    role = A11yRole.Switch,
                                    checked = enabled,
                                    indication = false,
                                    interactionSource = footerInteractions,
                                )
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (LocalTheme.current.shadowEnabled && !insetFooter) {
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
                    val glow = ShaderBrush(
                        CompositeShader(
                            LinearGradientShader(Offset.Zero, Offset(size.width, 0f), listOf(Accent, AccentEnd)),
                            LinearGradientShader(
                                Offset(0f, glowTop), Offset(0f, size.height),
                                listOf(Accent.copy(0f), Accent.copy(0.2f), Accent.copy(0.4f)),
                                listOf(0f, 0.4f, 1f),
                            ),
                            BlendMode.DstIn,
                        )
                    )
                    onDrawBehind {
                        drawRect(vignette)
                        drawRect(
                            glow,
                            topLeft = Offset(0f, glowTop),
                            size = Size(size.width, glowHeight),
                            alpha = glowStrength,
                        )
                    }
                }
            )
        }
        Row(Modifier.padding(CardToggleInset)) {
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
                interactive = interactive,
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(11.dp)) {
            TooltipBox(
                when {
                    toggle is ReadOnlyModToggle -> toggle.lockedReason?.takeIf { it.isNotBlank() }
                        ?: translated("oneconfig.mods.toggle.locked", "This mod is switched on and off somewhere else.")
                    toggle != null -> translated("oneconfig.mods.toggle.available", "This mod can be toggled on and off.")
                    else -> translated("oneconfig.mods.toggle.unavailable", "This mod doesn't support being toggled.")
                },
                placement = TooltipPlacement.AboveCenter,
            ) {
                if (locked) {
                    Icon(
                        "lock",
                        color = if (enabled) Accent else theme.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(ModCardToggleRing),
                    )
                } else Box(Modifier.size(ModCardToggleRing), contentAlignment = Alignment.Center) {
                    val shade = ModCardToggleRingShade
                    val unavailable = theme.controlTrackColor
                    val ringStart = if (toggle != null) lerp(Accent, shade, ModCardToggleRingDarken) else unavailable.copy(alpha = 0.5f)
                    val ringEnd = if (toggle != null) lerp(AccentEnd, shade, ModCardToggleRingDarken) else ringStart
                    val ring = lerp(ringStart, ringEnd, 0.5f)
                    val dot = unavailable
                    // drawn around one centre, since laying the dot out inside the ring lands it on a pixel boundary off-centre
                    Box(
                        Modifier.fillMaxSize().drawWithCache {
                            val glowRadius = (ModCardToggleRing / 2 + ModCardToggleGlow).toPx()
                            val inner = size.minDimension / 2f / glowRadius
                            val glow = Brush.radialGradient(
                                inner to ring.copy(alpha = 0.41f),
                                inner + (1f - inner) * 0.4f to ring.copy(alpha = 0.25f),
                                inner + (1f - inner) * 0.8f to ring.copy(alpha = 0.09f),
                                1f to Color.Transparent,
                                center = size.center, radius = glowRadius,
                            )
                            val dotSize = ModCardToggleDot.toPx()
                            val dotInset = (size.minDimension - dotSize) / 2f
                            val ringOutline = theme.circleShape.createOutline(size, layoutDirection, this)
                            val dotOutline = theme.circleShape.createOutline(Size(dotSize, dotSize), layoutDirection, this)
                            val ringBrush = Brush.linearGradient(listOf(ringStart, ringEnd), Offset.Zero, Offset(size.width, size.height))
                            val dotBrush = Brush.linearGradient(listOf(Accent, AccentEnd), Offset.Zero, Offset(dotSize, dotSize))
                            onDrawBehind {
                                if (toggle != null) drawCircle(glow, radius = glowRadius)
                                drawOutline(ringOutline, ringBrush)
                                translate(dotInset, dotInset) {
                                    if (toggle != null) {
                                        drawOutline(dotOutline, dotBrush)
                                    } else {
                                        drawOutline(dotOutline, dot)
                                    }
                                }
                            }
                        }
                    )
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
    val cardHovered by cardInteractions.collectIsHoveredAsState()
    val color by Motion.animateColorAsState(if (favorite) theme.favoriteColor else theme.textColor)
    CardCornerToggle(
        icon = if (favorite) "star-filled" else "star",
        label = if (favorite) translated("oneconfig.mods.menu.unfavorite", "Remove from favorites")
        else translated("oneconfig.mods.menu.favorite", "Add to favorites"),
        color = color,
        shown = favorite || hoverHint && cardHovered,
        emphasized = favorite,
        interactive = interactive,
        modifier = modifier,
        shape = cardCornerShape(),
    ) { ModFavorites.toggle(mod.id) }
}

@Composable
private fun HideToggle(
    mod: ConfigData,
    cardInteractions: InteractionSource,
    interactive: Boolean,
    modifier: Modifier = Modifier,
) {
    val hidden = ModHidden.isHidden(mod.id)
    val cardHovered by cardInteractions.collectIsHoveredAsState()
    CardCornerToggle(
        icon = if (hidden) "eye-off" else "eye",
        label = if (hidden) translated("oneconfig.mods.menu.show", "Show mod") else translated("oneconfig.mods.menu.hide", "Hide mod"),
        color = LocalTheme.current.textColor,
        shown = hidden || cardHovered,
        emphasized = hidden,
        interactive = interactive,
        modifier = modifier,
    ) { ModHidden.toggle(mod.id) }
}

@Composable
private fun CardCornerToggle(
    icon: String,
    label: String,
    color: Color,
    shown: Boolean,
    emphasized: Boolean,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = IconButtonDefaults.shape(IconButtonStyle.Ghost),
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val emphasis by Motion.animateFloatAsState(if (emphasized || hovered) 1f else CardToggleHintAlpha)
    val toggleModifier = modifier
        .size(CardToggleSize)
        .hiddenUntilHover(shown)
        .focusProperties { canFocus = false }
        .graphicsLayer { alpha = emphasis }
    if (!interactive) {
        Box(toggleModifier, contentAlignment = Alignment.Center) {
            Icon(icon, color = color, modifier = Modifier.size(CardToggleIconSize))
        }
        return
    }
    IconButton(
        icon,
        label,
        onClick,
        toggleModifier,
        size = ControlSize.Large,
        tooltip = null,
        tint = color,
        interactionSource = source,
        shape = shape,
    )
}

@Composable
private fun cardCornerShape(): Shape {
    val theme = LocalTheme.current
    val base = IconButtonDefaults.shape(IconButtonStyle.Ghost)
    val outer = theme.modCardShape.cornerRadiusOrNull() ?: return base
    val button = theme.buttonShape.cornerRadiusOrNull() ?: return base
    val inner = minOf(button, CardToggleSize / 4)
    val corner = (outer - CardToggleInset).coerceAtLeast(inner)
    return remember(corner, inner) { RoundedCornerShape(topStart = corner, topEnd = inner, bottomEnd = inner, bottomStart = inner) }
}

private val CardToggleInset = 4.dp
private val CardToggleSize = 24.dp
private val CardToggleIconSize = 18.dp
private const val CardToggleHintAlpha = 0.6f
