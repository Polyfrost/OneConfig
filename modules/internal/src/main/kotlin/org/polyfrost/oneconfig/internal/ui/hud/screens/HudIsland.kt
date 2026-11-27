package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudGroups
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudPreset
import org.polyfrost.oneconfig.api.ui.v1.OneConfigUI
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.KeyCap
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigDestination
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPages
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.backdropBlur
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.iconResourceBytes
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.rememberSvgResourcePainter
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.hud.HudModCardData
import org.polyfrost.oneconfig.internal.ui.hud.isCompatHud
import org.polyfrost.oneconfig.internal.ui.hud.libraryConfigId
import org.polyfrost.oneconfig.internal.ui.hud.modNameFor
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.screens.PREFERENCES_ID
import org.polyfrost.oneconfig.internal.ui.screens.THEMES_ID
import org.polyfrost.oneconfig.internal.ui.search.GlobalSettingIndex
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.isShownKeybind
import org.polyfrost.oneconfig.internal.ui.search.leadsTo
import org.polyfrost.oneconfig.internal.ui.shell.ShellController
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

internal val ISLAND_WIDTH = 740.dp
internal val ISLAND_TOP = 20.dp
private val ISLAND_PADDING = 6.dp
private val ISLAND_CONTROL = 40.dp
private val ISLAND_PILL_RADIUS = 26.dp
private val ISLAND_PANEL_RADIUS = 20.dp
private val ISLAND_GLASS_ALPHA = 0.66f
private val BRAND_BLUE_SOFT = Color(0xFF5187F1)
private val LAUNCHER_PLATE = Color(0x8C11171C)
private val LAUNCHER_RIM = Color(0xE68CB4FF)
private val LAUNCHER_MARK = Color(0xFF6A9BFF)
private val LAUNCHER_CAPTION = Color(0x9911171C)
private const val DEFAULT_BRAND_ICON = "/assets/oneconfig/brand/oneconfig-icon.svg"

internal fun brandIconPath(): String = OneConfigTheme.brandIconOverride ?: DEFAULT_BRAND_ICON

internal enum class IslandPanel { None, Library, Search }

internal class IslandDragInfo(val title: String, val x: Int, val y: Int, val shortcuts: List<Pair<String, String>>)

internal class IslandHudInfo(
    val title: String,
    val subtitle: String?,
    val hidden: Boolean,
    val locked: Boolean,
    val groupLabel: String?,
    val canDelete: Boolean,
)

@Composable
internal fun Modifier.islandGlass(shape: Shape): Modifier {
    val theme = LocalTheme.current
    val fill = if (theme.isMinecraftStyle) theme.pageBackground else theme.pageBackground.copy(alpha = ISLAND_GLASS_ALPHA)
    return this
        .then(
            if (theme.shadowEnabled) Modifier.shadow(16.dp, shape, clip = false, ambientColor = theme.shadowColor, spotColor = theme.shadowColor)
            else Modifier
        )
        .backdropBlur(shape)
        .background(fill, shape)
        .border(1.dp, theme.borderColor, shape)
}

@Composable
internal fun HudIslandSurface(
    expanded: Boolean,
    collapsed: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = OneConfigTheme.shape(if (expanded) ISLAND_PANEL_RADIUS else ISLAND_PILL_RADIUS)
    val progress by Motion.animateFloatAsState(
        if (collapsed) 1f else 0f,
        tween(ISLAND_COLLAPSE_MS, easing = FastOutSlowInEasing),
        label = "islandCollapse",
    )
    val expandLabel = translated("oneconfig.hud.island.expand", "Show the bar")
    Layout(
        content = {
            if (progress < 1f) {
                Column(
                    Modifier
                        .layoutId(ISLAND_BAR_ID)
                        .graphicsLayer { alpha = (1f - progress * 2f).coerceIn(0f, 1f) }
                        .animateContentSize()
                        .padding(ISLAND_PADDING),
                    verticalArrangement = Arrangement.spacedBy(ISLAND_PADDING),
                    content = content,
                )
            }
            if (progress > 0f) {
                IslandPillContent(Modifier.layoutId(ISLAND_PILL_ID).graphicsLayer { alpha = (progress * 2f - 1f).coerceIn(0f, 1f) })
            }
            if (collapsed || progress > 0f) {
                Box(Modifier.layoutId(ISLAND_CATCHER_ID).oneConfigClickable(expandLabel, onExpand, role = A11yRole.Button, indication = false))
            }
        },
        modifier = modifier
            .widthIn(max = ISLAND_WIDTH)
            .islandGlass(shape)
            .clip(shape)
            .then(if (interactionSource != null) Modifier.hoverable(interactionSource) else Modifier),
    ) { measurables, constraints ->
        val bar = measurables.firstOrNull { it.layoutId == ISLAND_BAR_ID }?.measure(constraints.copy(minWidth = constraints.maxWidth))
        val pill = measurables.firstOrNull { it.layoutId == ISLAND_PILL_ID }?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val barWidth = bar?.width ?: pill?.width ?: 0
        val barHeight = bar?.height ?: pill?.height ?: 0
        val width = (barWidth + ((pill?.width ?: barWidth) - barWidth) * progress).roundToInt()
        val height = (barHeight + ((pill?.height ?: barHeight) - barHeight) * progress).roundToInt()
        val catcher = measurables.firstOrNull { it.layoutId == ISLAND_CATCHER_ID }?.measure(Constraints.fixed(width, height))
        layout(width, height) {
            bar?.place((width - bar.width) / 2, (height - bar.height) / 2)
            pill?.place((width - pill.width) / 2, (height - pill.height) / 2)
            catcher?.place(0, 0)
        }
    }
}

@Composable
private fun IslandButton(
    text: String,
    icon: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ControlStyle = ControlStyle.Secondary,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = OneConfigTheme.shape(ISLAND_CONTROL / 2)
    if (icon != null) {
        TextButton(text, icon, onClick, modifier.height(ISLAND_CONTROL), style = style, size = ControlSize.Large, trailing = trailing, shape = shape)
    } else {
        TextButton(text, onClick, modifier.height(ISLAND_CONTROL), style = style, size = ControlSize.Large, trailing = trailing, shape = shape)
    }
}

@Composable
private fun IslandDivider() {
    Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(26.dp).background(LocalTheme.current.borderColor))
}

@Composable
private fun IslandTitle(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Column(modifier.padding(start = 14.dp, end = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = theme.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) Text(subtitle, color = theme.textColorSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    val painter = rememberSvgResourcePainter(brandIconPath()) ?: return
    Image(painter, contentDescription = null, colorFilter = ColorFilter.tint(BRAND_BLUE_SOFT), modifier = modifier.size(size))
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    val painter = rememberSvgResourcePainter(LocalTheme.current.logoPath) ?: return
    Image(painter, contentDescription = null, modifier = modifier.height(11.dp).width(92.dp), alignment = Alignment.CenterStart)
}

@Composable
internal fun IslandIdleRow(
    modCount: Int,
    onMods: () -> Unit,
    onAdd: () -> Unit,
    onSearch: () -> Unit,
    onPreferences: () -> Unit,
    onCollapse: () -> Unit,
) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ISLAND_PADDING)) {
        Row(Modifier.padding(start = 8.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrandMark(26.dp)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Wordmark()
                Text(
                    translated("oneconfig.hud.island.hint", "Drag a HUD to move it"),
                    color = theme.textColorSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
        IslandDivider()
        IslandButton(
            translated("oneconfig.hud.island.mods", "Mods"),
            null,
            onMods,
            Modifier.weight(1f),
            style = ControlStyle.Primary,
            trailing = { Text(modCount.toString(), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) },
        )
        IslandButton(translated("oneconfig.hud.island.add", "Add HUD"), "plus", onAdd)
        IslandButton(translated("oneconfig.hud.island.search", "Search"), "search", onSearch)
        IslandButton(translated("oneconfig.hud.island.preferences", "Preferences"), "cog", onPreferences)
        IslandIconButton("up", translated("oneconfig.hud.island.collapse", "Hide the bar"), onCollapse)
    }
}

@Composable
private fun IslandIconButton(icon: String, label: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    Box(
        Modifier
            .size(ISLAND_CONTROL)
            .clip(OneConfigTheme.shape(ISLAND_CONTROL / 2))
            .background(if (hovered) theme.textColor.copy(alpha = 0.08f) else Color.Transparent)
            .oneConfigClickable(label, onClick, role = A11yRole.Button, indication = false, interactionSource = source),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, modifier = Modifier.size(16.dp), color = theme.textColorSecondary)
    }
}

@Composable
private fun IslandPillContent(modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Row(
        modifier.padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrandMark(20.dp)
        Icon("down", modifier = Modifier.size(14.dp), color = theme.textColorSecondary)
    }
}

@Composable
internal fun IslandHudRow(
    info: IslandHudInfo,
    onSettings: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleLocked: () -> Unit,
    onGroupAction: () -> Unit,
    onDelete: () -> Unit,
    onCollapse: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(ISLAND_CONTROL), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ISLAND_PADDING)) {
        IslandTitle(info.title, info.subtitle, Modifier.width(150.dp))
        IslandDivider()
        IslandButton(translated("oneconfig.hud.menu.settings", "Settings"), "settings", onSettings, Modifier.weight(1f))
        IslandButton(
            if (info.hidden) translated("oneconfig.hud.menu.show", "Show") else translated("oneconfig.hud.menu.hide", "Hide"),
            if (info.hidden) "eye-off" else "eye",
            onToggleHidden,
            Modifier.weight(1f),
        )
        IslandButton(
            if (info.locked) translated("oneconfig.hud.menu.unlock", "Unlock") else translated("oneconfig.hud.menu.lock", "Lock"),
            if (info.locked) "lock" else "unlock",
            onToggleLocked,
            Modifier.weight(1f),
        )
        if (info.groupLabel != null) IslandButton(info.groupLabel, "group", onGroupAction, Modifier.weight(1f))
        if (info.canDelete) {
            IslandButton(translated("oneconfig.hud.island.remove", "Remove"), "trash", onDelete, Modifier.weight(1f), style = ControlStyle.Danger)
        }
        IslandIconButton("up", translated("oneconfig.hud.island.collapse", "Hide the bar"), onCollapse)
    }
}

@Composable
internal fun IslandDragRow(info: IslandDragInfo) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth().height(ISLAND_CONTROL), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ISLAND_PADDING)) {
        IslandTitle(info.title, "X ${info.x}  ·  Y ${info.y}", Modifier.width(150.dp))
        IslandDivider()
        for ((key, label) in info.shortcuts) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                KeyCap(key)
                Text(label, color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun IslandFieldRow(onClose: () -> Unit, field: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(ISLAND_CONTROL), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ISLAND_PADDING)) {
        Box(Modifier.size(ISLAND_CONTROL), contentAlignment = Alignment.Center) { BrandMark(24.dp) }
        field()
        IslandCloseButton(onClose)
    }
}

@Composable
private fun IslandCloseButton(onClose: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    Box(
        Modifier
            .size(ISLAND_CONTROL)
            .clip(OneConfigTheme.shape(ISLAND_CONTROL / 2))
            .background(if (hovered) theme.textColor.copy(alpha = 0.08f) else Color.Transparent)
            .oneConfigClickable(translated("oneconfig.button.close", "Close"), onClose, role = A11yRole.Button, indication = false, interactionSource = source),
        contentAlignment = Alignment.Center,
    ) {
        Icon("close", modifier = Modifier.size(18.dp), color = theme.textColor)
    }
}

internal enum class LibraryFilter { All, OnScreen }

internal val ISLAND_LIBRARY_HEIGHT = 330.dp
internal const val ISLAND_LINGER_MS = 700L
internal const val CHROME_LAUNCHER = "launcher"
internal val LAUNCHER_NEAR_RADIUS = 160.dp
internal val ISLAND_PANEL_TOP_INSET = 84.dp
internal const val LAUNCHER_FADE_MS = 220
internal const val ISLAND_COLLAPSE_MS = 280
private const val ISLAND_BAR_ID = "bar"
private const val ISLAND_PILL_ID = "pill"
private const val ISLAND_CATCHER_ID = "catcher"

@Composable
internal fun IslandLibrarySidebar(
    total: Int,
    onScreen: Int,
    mods: List<Triple<String?, String, Int>>,
    filter: LibraryFilter,
    activeModId: String?,
    otherSelected: Boolean,
    onAll: () -> Unit,
    onOnScreen: () -> Unit,
    onMod: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    LazyColumn(modifier.width(168.dp).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { SidebarEntry("grid", translated("oneconfig.hud.island.all", "All"), total, filter == LibraryFilter.All && activeModId == null && !otherSelected, onClick = onAll) }
        item { SidebarEntry("eye", translated("oneconfig.hud.island.on_screen", "On screen"), onScreen, filter == LibraryFilter.OnScreen, onClick = onOnScreen) }
        item {
            Column {
                Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth().height(1.dp).background(theme.borderColor))
                Text(
                    translated("oneconfig.hud.island.mods_heading", "Mods").uppercase(),
                    color = theme.textColorSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp),
                )
            }
        }
        items(mods, key = { it.first ?: "" }) { (modId, name, count) ->
            val selected = filter == LibraryFilter.All && activeModId == modId && (modId != null || otherSelected)
            SidebarEntry(null, name, count, selected, modId = modId) { onMod(modId) }
        }
    }
}

@Composable
private fun SidebarEntry(icon: String?, label: String, count: Int, selected: Boolean, modId: String? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val shape = theme.sideBarNavigationEntryShape
    val content = if (selected) Color.White else theme.textColor
    Row(
        Modifier
            .fillMaxWidth()
            .height(33.dp)
            .clip(shape)
            .background(
                when {
                    selected -> Accent
                    hovered -> theme.textColor.copy(alpha = 0.07f)
                    else -> Color.Transparent
                }
            )
            .oneConfigClickable(label, onClick, role = A11yRole.Button, selected = selected, indication = false, interactionSource = source)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) Icon(icon, modifier = Modifier.size(16.dp), color = content)
        else Icon(libraryIcon(modId), modifier = Modifier.size(16.dp), color = content)
        Text(label, color = content, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(count.toString(), color = content.copy(alpha = 0.6f), fontSize = 11.sp)
    }
}

private fun libraryIcon(modId: String?): String =
    modId?.let { HudManager.iconFor(it) ?: ConfigRegistry.findById(it)?.icon } ?: "qol"

@Composable
internal fun IslandFooter(left: String, right: String?) {
    val theme = LocalTheme.current
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, color = theme.textColorSecondary, fontSize = 11.sp, maxLines = 1)
        if (right != null) Text(right, color = theme.textColorSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

internal enum class IslandHitKind(val key: String, val title: String) {
    Huds("huds", "HUDs"),
    Mods("mods", "Mods"),
    Settings("settings", "Settings"),
    Keybinds("keybinds", "Keybinds"),
    ;

    val label: String get() = translated("oneconfig.hud.island.group.$key", title)
}

internal class IslandHit(
    val key: String,
    val kind: IslandHitKind,
    val title: String,
    val location: String?,
    val icon: String,
    val hud: Hud? = null,
    val hudMod: Hud? = null,
    val preset: HudPreset? = null,
    val open: () -> Unit = {},
)

private fun islandSearchHits(query: String, providers: Collection<Hud>, hudLabel: (Hud) -> String): List<IslandHit> {
    val known = providers.toHashSet()
    val out = ArrayList<IslandHit>()
    val seen = HashSet<String>()
    for (document in SearchCorpus.search(query, setOf(SearchScope.Huds, SearchScope.Mods, SearchScope.Options, SearchScope.Keybinds))) {
        val payload = document.payload
        val hit = when {
            payload is Hud && payload in known -> {
                val onScreen = if (isCompatHud(payload)) payload in HudManager.activeInstances else HudManager.hasHudOfType(payload::class.java)
                val mod = payload.configId?.let(::modNameFor)
                val location = listOfNotNull(mod, if (onScreen) translated("oneconfig.hud.island.on_screen", "On screen") else null).joinToString("  ·  ")
                IslandHit("hud:${System.identityHashCode(payload)}", IslandHitKind.Huds, hudLabel(payload), location.ifBlank { null }, "hud", hud = payload)
            }
            payload is HudPreset -> {
                if (!HudGroups.canAdd(payload)) continue
                val mod = payload.libraryConfigId?.let(::modNameFor)
                val size = translated("oneconfig.hud.island.group_title", "%d HUDs", payload.members.size)
                IslandHit("preset:${payload.id}", IslandHitKind.Huds, payload.name, listOfNotNull(mod, size).joinToString("  ·  "), payload.icon?.takeIf(::canRenderIcon) ?: "group", preset = payload)
            }
            SearchScope.Mods in document.scopes && payload is ConfigData -> {
                val title = document.metadata.title ?: continue
                val icon = payload.icon?.takeIf(::canRenderIcon) ?: "settings"
                IslandHit("mod:${payload.id}", IslandHitKind.Mods, title, null, icon, hudMod = (payload as? HudModCardData)?.hud) { openMod(payload) }
            }
            SearchScope.Keybinds in document.scopes -> {
                if (!isShownKeybind(payload)) continue
                val title = document.metadata.title ?: continue
                IslandHit("keybind:${document.id}", IslandHitKind.Keybinds, title, document.metadata.modTitle, "keyboard") { openKeybind(title) }
            }
            SearchScope.Options in document.scopes -> {
                val title = document.metadata.title ?: continue
                val row = GlobalSettingIndex.rowOf(document)?.takeIf { it.leadsTo(payload) } ?: continue
                val location = listOfNotNull(row.modTitle, row.groupLabel?.takeIf { it.isNotBlank() }).joinToString(" › ").ifBlank { null }
                IslandHit("option:${document.id}", IslandHitKind.Settings, title, location, "settings") { openOption(row, payload) }
            }
            else -> null
        } ?: continue
        if (seen.add(hit.key)) out += hit
    }
    return out.groupBy { it.kind }.toSortedMap(compareBy { it.ordinal }).values.flatten()
}

@Suppress("DEPRECATION")
internal fun openModsPage() = OneConfigUI.open(ModsGraph)

@Suppress("DEPRECATION")
private fun openKeybind(title: String) {
    ShellController.shared.navigation.searchAfterNextNavigation(title)
    OneConfigUI.open(KeybindsGraph)
}

@Suppress("DEPRECATION")
internal fun openPreferencesPage() = OneConfigUI.open(PreferencesGraph)

private fun openMod(mod: ConfigData) {
    val onOpen = mod.onOpen
    if (onOpen != null) onOpen() else OneConfigUI.open(OneConfigDestination.ModConfig(OneConfigPages.modConfig(mod.id)))
}

@Suppress("DEPRECATION")
private fun openOption(row: SearchRow, anchor: Any?) {
    val id = row.mod?.id ?: return
    val route: Any = when {
        row.submenu != null -> ModConfigRoute(id, submenu = row.submenu)
        id == PREFERENCES_ID -> PreferencesGraph
        id == THEMES_ID -> ThemesGraph
        else -> ModConfigRoute(id)
    }
    anchor?.let(ShellController.shared.navigation::jumpAfterNextNavigation)
    OneConfigUI.open(route)
}

@Composable
internal fun IslandSearchResults(
    query: String,
    providers: Collection<Hud>,
    hudLabel: (Hud) -> String,
    submittedQuery: String?,
    onSubmitHandled: () -> Unit,
    onHud: (Hud) -> Unit,
    onHudMod: (Hud) -> Unit,
    onPreset: (HudPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    var hits by remember { mutableStateOf(emptyList<IslandHit>()) }
    var hitsQuery by remember { mutableStateOf<String?>("") }
    LaunchedEffect(query, providers, SearchCorpus.revision) {
        hitsQuery = null
        hits = if (query.isBlank()) emptyList() else withContext(Dispatchers.Default) { islandSearchHits(query.trim(), providers, hudLabel) }
        hitsQuery = query
    }

    fun activate(hit: IslandHit) {
        if (hitsQuery != query) return
        when {
            hit.hud != null -> onHud(hit.hud)
            hit.hudMod != null -> onHudMod(hit.hudMod)
            hit.preset != null -> onPreset(hit.preset)
            else -> hit.open()
        }
    }
    LaunchedEffect(submittedQuery, hitsQuery) {
        val wanted = submittedQuery ?: return@LaunchedEffect
        if (hitsQuery != query) return@LaunchedEffect
        onSubmitHandled()
        if (wanted == query) hits.firstOrNull()?.let(::activate)
    }
    if (query.isBlank()) return
    if (hits.isEmpty()) {
        Text(
            translated("oneconfig.hud.island.no_results", "Nothing matches that search."),
            color = theme.textColorSecondary,
            fontSize = 12.sp,
            modifier = modifier.padding(horizontal = 14.dp, vertical = 14.dp),
        )
        return
    }
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var lastKind: IslandHitKind? = null
        for (hit in hits) {
            if (hit.kind != lastKind) {
                val kind = hit.kind
                val top = if (lastKind == null) 6.dp else 12.dp
                item(key = "header:${kind.key}") {
                    Text(
                        kind.label.uppercase(),
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 10.dp, top = top, bottom = 6.dp),
                    )
                }
                lastKind = kind
            }
            item(key = hit.key) {
                SearchHitRow(hit) { activate(hit) }
            }
        }
    }
}

@Composable
private fun SearchHitRow(hit: IslandHit, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(theme.buttonShape)
            .background(if (hovered) theme.textColor.copy(alpha = 0.07f) else Color.Transparent)
            .oneConfigClickable(hit.title, onClick, role = A11yRole.Button, indication = false, interactionSource = source)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(hit.icon, modifier = Modifier.size(16.dp), color = theme.textColor)
        Text(hit.title, color = theme.textColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(0.01f))
        if (hit.location != null) Text(hit.location, color = theme.textColorSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun HudLauncherLogo(onClick: () -> Unit, keyName: String?, interactive: Boolean, modifier: Modifier = Modifier) {
    val iconPath = brandIconPath()
    val painter = rememberSvgResourcePainter(iconPath) ?: return
    val outline = remember(iconPath) { iconResourceBytes(iconPath)?.let(::svgOutline) }
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val label = translated("oneconfig.hud.island.open_mods", "Open mods")
    Column(
        if (interactive) modifier.oneConfigClickable(label, onClick, role = A11yRole.Button, indication = false, interactionSource = source) else modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val aspect = outline?.aspect ?: 1.12f
        Box(Modifier.size(112.dp, 112.dp / aspect), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val points = outline?.points
                if (points == null || points.size < 3) {
                    drawCircle(LAUNCHER_PLATE)
                    drawCircle(LAUNCHER_RIM, style = Stroke(1.dp.toPx()))
                    return@Canvas
                }
                val path = Path().apply {
                    moveTo(points[0].x * size.width, points[0].y * size.height)
                    for (i in 1 until points.size) lineTo(points[i].x * size.width, points[i].y * size.height)
                    close()
                }
                drawPath(path, LAUNCHER_PLATE)
                drawPath(path, LAUNCHER_RIM, style = Stroke(1.dp.toPx()))
            }
            Image(
                painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(LAUNCHER_MARK),
                modifier = Modifier.size(60.dp, 60.dp / aspect).graphicsLayer { alpha = if (hovered) 1f else 0.92f },
            )
        }
        Row(
            Modifier
                .background(LAUNCHER_CAPTION, OneConfigTheme.shape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), OneConfigTheme.shape(16.dp))
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1)
            if (keyName != null) KeyCap(keyName)
        }
    }
}

internal class SvgOutline(val points: List<Offset>, val aspect: Float)

private val viewBoxRegex = Regex("viewBox\\s*=\\s*\"([^\"]+)\"")
private val sizeRegex = Regex("\\b(width|height)\\s*=\\s*\"([\\d.]+)")
private val pathDataRegex = Regex("\\sd\\s*=\\s*\"([^\"]+)\"")
private val pathTokenRegex = Regex("[A-Za-z]|-?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?")

internal fun svgOutline(bytes: ByteArray): SvgOutline? = runCatching {
    val svg = bytes.toString(Charsets.UTF_8)
    val box = viewBoxRegex.find(svg)?.groupValues?.get(1)?.trim()?.split(Regex("[\\s,]+"))?.mapNotNull { it.toFloatOrNull() }
    val sizes = sizeRegex.findAll(svg).associate { it.groupValues[1] to it.groupValues[2].toFloat() }
    val (bw, bh) = if (box != null && box.size == 4) box.subList(2, 4) else listOf(sizes["width"] ?: return@runCatching null, sizes["height"] ?: return@runCatching null)
    if (bw <= 0f || bh <= 0f) return@runCatching null
    val samples = ArrayList<Offset>()
    for (match in pathDataRegex.findAll(svg)) samplePath(match.groupValues[1], samples)
    if (samples.size < 3) return@runCatching null
    val hull = convexHull(samples)
    val minX = hull.minOf { it.x }
    val maxX = hull.maxOf { it.x }
    val minY = hull.minOf { it.y }
    val maxY = hull.maxOf { it.y }
    val w = (maxX - minX).takeIf { it > 0f } ?: bw
    val h = (maxY - minY).takeIf { it > 0f } ?: bh
    SvgOutline(hull.map { Offset((it.x - minX) / w, (it.y - minY) / h) }, (w / h).coerceIn(0.5f, 2f))
}.getOrNull()

private fun samplePath(data: String, out: MutableList<Offset>) {
    val tokens = pathTokenRegex.findAll(data).map { it.value }.toList()
    var i = 0
    var cmd = 'M'
    var x = 0f
    var y = 0f
    var startX = 0f
    var startY = 0f
    var lastCx = 0f
    var lastCy = 0f
    fun num(): Float = tokens[i++].toFloat()
    fun hasNum() = i < tokens.size && !tokens[i][0].isLetter()
    fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, ex: Float, ey: Float) {
        for (step in 1..8) {
            val t = step / 8f
            val u = 1 - t
            out += Offset(
                u * u * u * x + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * ex,
                u * u * u * y + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * ey,
            )
        }
        lastCx = x2; lastCy = y2; x = ex; y = ey
    }
    while (i < tokens.size) {
        if (tokens[i][0].isLetter()) cmd = tokens[i++][0]
        val rel = cmd.isLowerCase()
        val ox = if (rel) x else 0f
        val oy = if (rel) y else 0f
        when (cmd.uppercaseChar()) {
            'M' -> {
                x = ox + num(); y = oy + num(); startX = x; startY = y; out += Offset(x, y)
                cmd = if (rel) 'l' else 'L'
            }
            'L' -> { x = ox + num(); y = oy + num(); out += Offset(x, y) }
            'H' -> { x = ox + num(); out += Offset(x, y) }
            'V' -> { y = oy + num(); out += Offset(x, y) }
            'C' -> cubic(ox + num(), oy + num(), ox + num(), oy + num(), ox + num(), oy + num())
            'S' -> cubic(2 * x - lastCx, 2 * y - lastCy, ox + num(), oy + num(), ox + num(), oy + num())
            'Q' -> {
                val qx = ox + num(); val qy = oy + num(); val ex = ox + num(); val ey = oy + num()
                cubic(x + 2f / 3f * (qx - x), y + 2f / 3f * (qy - y), ex + 2f / 3f * (qx - ex), ey + 2f / 3f * (qy - ey), ex, ey)
            }
            'T' -> { x = ox + num(); y = oy + num(); out += Offset(x, y) }
            'A' -> { repeat(5) { num() }; x = ox + num(); y = oy + num(); out += Offset(x, y) }
            'Z' -> { x = startX; y = startY }
            else -> i++
        }
        if (cmd.uppercaseChar() != 'C' && cmd.uppercaseChar() != 'S') { lastCx = x; lastCy = y }
        if (!hasNum() && i < tokens.size && !tokens[i][0].isLetter()) i++
    }
}

private fun convexHull(points: List<Offset>): List<Offset> {
    val sorted = points.distinct().sortedWith(compareBy<Offset> { it.x }.thenBy { it.y })
    if (sorted.size < 3) return sorted
    fun cross(o: Offset, a: Offset, b: Offset) = (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lower = ArrayList<Offset>()
    for (p in sorted) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0f) lower.removeAt(lower.lastIndex)
        lower += p
    }
    val upper = ArrayList<Offset>()
    for (p in sorted.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0f) upper.removeAt(upper.lastIndex)
        upper += p
    }
    return lower.dropLast(1) + upper.dropLast(1)
}

internal fun screenRegionLabel(centerX: Float, centerY: Float, width: Float, height: Float): String? {
    if (width <= 0f || height <= 0f) return null
    val col = (centerX / width * 3).toInt().coerceIn(0, 2)
    val row = (centerY / height * 3).toInt().coerceIn(0, 2)
    val key = listOf("top", "middle", "bottom")[row] + "_" + listOf("left", "center", "right")[col]
    val fallback = when (key) {
        "middle_center" -> "Middle"
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    return translated("oneconfig.hud.island.region.$key", fallback)
}

