package org.polyfrost.oneconfig.internal.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeCloses
import org.polyfrost.oneconfig.api.ui.v1.components.KeyCap
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.popupSurface
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.StyledText
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.resolve
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.pageRoute
import org.polyfrost.oneconfig.internal.ui.navigation.registeredPages
import org.polyfrost.oneconfig.internal.ui.screens.PREFERENCES_ID
import org.polyfrost.oneconfig.internal.ui.screens.THEMES_ID
import org.polyfrost.oneconfig.internal.ui.search.GlobalSettingIndex
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.isShownKeybind
import org.polyfrost.oneconfig.internal.ui.search.leadsTo
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val PaletteWidth = 760.dp
private val PaletteTop = 70.dp
private val PaletteResultsHeight = 400.dp
private val PaletteRowHeight = 40.dp
private const val HOVER_AFTER_KEYS_NANOS = 400_000_000L
private val ScrimColor = Color(0x8C06090C)

private enum class PaletteGroup(val key: String, val title: String) {
    Pages("pages", "Pages"),
    Mods("mods", "Mods"),
    ModSettings("mod_settings", "Mod settings"),
    OneConfig("oneconfig", "OneConfig settings"),
    Keybinds("keybinds", "Keybinds"),
    ;

    val label: String get() = translated("oneconfig.palette.group.$key", title)
}

private class PaletteHit(
    val key: String,
    val group: PaletteGroup,
    val title: String,
    val location: String?,
    val icon: String?,
    val open: (ShellNavigation, ShellSearch) -> Unit,
)

@Composable
internal fun BoxScope.CommandPaletteOverlay() {
    val palette = LocalShell.current.palette
    val entry by LocalShellNavController.current.currentBackStackEntryAsState()
    LaunchedEffect(entry?.id) { palette.hide() }
    DisposableEffect(Unit) { onDispose { palette.hide() } }
    AnimatedVisibility(palette.open, Modifier.matchParentSize(), enter = Motion.enter(fadeIn()), exit = Motion.exit(fadeOut())) {
        Box(
            Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .pointerInput(Unit) { detectTapGestures { palette.hide() } },
        )
    }
    AnimatedVisibility(
        palette.open,
        Modifier.align(Alignment.TopCenter).padding(top = PaletteTop),
        enter = Motion.enter(fadeIn() + scaleIn(initialScale = 0.97f)),
        exit = Motion.exit(fadeOut() + scaleOut(targetScale = 0.97f)),
    ) {
        EscapeCloses { palette.hide() }
        CommandPalette { palette.hide() }
    }
}

@Composable
private fun CommandPalette(onClose: () -> Unit) {
    val theme = LocalTheme.current
    val shell = LocalShell.current
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf(emptyList<PaletteHit>()) }
    var queryGen by remember { mutableIntStateOf(0) }
    var hitsGen by remember { mutableIntStateOf(0) }
    var scrolledGen by remember { mutableIntStateOf(0) }
    var submitPending by remember { mutableStateOf(false) }
    var selected by remember { mutableIntStateOf(0) }
    var keyboardMoves by remember { mutableIntStateOf(0) }
    var keyboardAt by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    LaunchedEffect(query, SearchCorpus.revision) {
        val gen = queryGen
        hitsGen = -1
        hits = if (query.isBlank()) emptyList() else withContext(Dispatchers.Default) { paletteHits(query.trim()) }
        hitsGen = gen
        selected = selected.coerceAtMost((hits.size - 1).coerceAtLeast(0))
        if (scrolledGen != gen) {
            scrolledGen = gen
            if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
        }
    }
    LaunchedEffect(keyboardMoves) {
        if (keyboardMoves == 0) return@LaunchedEffect
        val row = rowIndexOf(hits, selected) ?: return@LaunchedEffect
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == row }
        val viewport = info.viewportEndOffset - info.viewportStartOffset
        when {
            item == null && row < (info.visibleItemsInfo.firstOrNull()?.index ?: 0) -> listState.animateScrollToItem((row - 1).coerceAtLeast(0))
            item == null -> listState.animateScrollToItem(row, -(viewport - (info.visibleItemsInfo.lastOrNull()?.size ?: 0)))
            item.offset < info.viewportStartOffset -> listState.animateScrollToItem((row - 1).coerceAtLeast(0))
            item.offset + item.size > info.viewportEndOffset -> listState.animateScrollBy((item.offset + item.size - info.viewportEndOffset).toFloat())
        }
    }

    fun open(hit: PaletteHit) {
        if (hitsGen != queryGen) return
        onClose()
        hit.open(shell.navigation, shell.search)
    }

    LaunchedEffect(submitPending, hitsGen) {
        if (!submitPending || hitsGen != queryGen) return@LaunchedEffect
        submitPending = false
        hits.getOrNull(selected)?.let(::open)
    }

    val shape = theme.popupShape
    Column(
        Modifier
            .width(PaletteWidth)
            .popupSurface(shape)
            .clip(shape),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = theme.textColorSecondary, modifier = Modifier.size(18.dp))
            TextField(
                query,
                {
                    if (it != query) {
                        queryGen++
                        selected = 0
                        submitPending = false
                    }
                    query = it
                },
                Modifier
                    .weight(1f)
                    .trackTextInputFocus()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || hits.isEmpty() || hitsGen != queryGen) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                selected = (selected + 1) % hits.size
                                keyboardMoves++
                                keyboardAt = System.nanoTime()
                                true
                            }
                            Key.DirectionUp -> {
                                selected = (selected - 1 + hits.size) % hits.size
                                keyboardMoves++
                                keyboardAt = System.nanoTime()
                                true
                            }
                            else -> false
                        }
                    },
                placeholder = translated("oneconfig.palette.placeholder", "Search mods, settings, keybinds and pages"),
                size = ControlSize.Large,
                framed = false,
                fontSize = 16.sp,
                focusRequester = focusRequester,
                onSubmit = { if (hitsGen == queryGen) hits.getOrNull(selected)?.let(::open) else submitPending = true },
            )
            KeyCap(translated("oneconfig.palette.key.escape", "Esc"))
        }
        if (query.isNotBlank()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor))
            if (hits.isEmpty()) {
                Text(
                    translated("oneconfig.palette.empty", "Nothing matches \"%s\"", query.trim()),
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
                    color = theme.textColorSecondary,
                    fontSize = 13.sp,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = PaletteResultsHeight).padding(horizontal = 6.dp),
                ) {
                    var index = 0
                    hits.groupBy { it.group }.forEach { (group, groupHits) ->
                        item(key = "group:${group.key}") {
                            Text(
                                group.label.uppercase(),
                                Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                                color = theme.textColorSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        groupHits.forEach { hit ->
                            val at = index++
                            item(key = hit.key) {
                                PaletteRow(hit, query.trim(), at == selected, {
                                    val quiet = System.nanoTime() - keyboardAt > HOVER_AFTER_KEYS_NANOS
                                    if (quiet && !listState.isScrollInProgress && hitsGen == queryGen) selected = at
                                }) { open(hit) }
                            }
                        }
                    }
                    item(key = "bottom") { Box(Modifier.height(6.dp)) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor))
        Row(
            Modifier.fillMaxWidth().height(38.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterHint(listOf("↑", "↓"), translated("oneconfig.palette.hint.move", "Move"))
            FooterHint(listOf(translated("oneconfig.palette.key.enter", "Enter")), translated("oneconfig.palette.hint.open", "Open"))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(
                    translated("oneconfig.palette.hint.scope", "Searches mods, settings, keybinds and pages"),
                    color = theme.textColorSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun rowIndexOf(hits: List<PaletteHit>, selected: Int): Int? {
    if (selected !in hits.indices) return null
    var row = 0
    var index = 0
    var group: PaletteGroup? = null
    for (hit in hits.groupBy { it.group }.values.flatten()) {
        if (hit.group != group) {
            group = hit.group
            row++
        }
        if (index == selected) return row
        row++
        index++
    }
    return null
}

@Composable
private fun FooterHint(keys: List<String>, label: String) {
    val theme = LocalTheme.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        keys.forEach { KeyCap(it) }
        Text(label, Modifier.padding(start = 2.dp), color = theme.textColorSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun PaletteRow(hit: PaletteHit, query: String, highlighted: Boolean, onHover: () -> Unit, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    LaunchedEffect(hovered) { if (hovered) onHover() }
    val fill by Motion.animateColorAsState(if (highlighted) Accent.copy(alpha = 0.18f) else Accent.copy(alpha = 0f))
    val shape = theme.buttonShape
    Row(
        Modifier
            .fillMaxWidth()
            .height(PaletteRowHeight)
            .clip(shape)
            .background(fill, shape)
            .oneConfigClickable(hit.title, onClick, role = A11yRole.Option, selected = highlighted, indication = false, interactionSource = source)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            hit.icon?.let { Icon(it, color = theme.textColorSecondary, modifier = Modifier.size(18.dp)) }
        }
        StyledText(
            highlightMatch(hit.title, query),
            Modifier.weight(1f),
            theme.textColor,
            theme.typography.body.copy(fontSize = 14.sp),
            1,
            TextOverflow.Ellipsis,
            false,
            null,
            null,
            null,
        )
        hit.location?.let {
            Text(it, color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun highlightMatch(text: String, query: String): AnnotatedString {
    val start = if (query.isEmpty()) -1 else text.indexOf(query, ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color.White)) { append(text.substring(start, start + query.length)) }
        append(text.substring(start + query.length))
    }
}

private fun paletteHits(query: String): List<PaletteHit> {
    val out = ArrayList<PaletteHit>()
    val seen = HashSet<String>()
    fun add(hit: PaletteHit) {
        if (seen.add(hit.key)) out += hit
    }

    registeredPages().filter { it.showInSidebar }.forEach { page ->
        val title = runCatching { page.title.resolve() }.getOrNull() ?: return@forEach
        if (!searchMatches(title, query)) return@forEach
        val route = pageRoute(page)
        add(PaletteHit("page:${page.id}", PaletteGroup.Pages, title, null, page.icon) { nav, _ -> nav.navigate(route) })
    }

    val documents = SearchCorpus.search(query, setOf(SearchScope.Mods, SearchScope.Options, SearchScope.Keybinds))
    for (document in documents) {
        val title = document.metadata.title ?: continue
        val payload = document.payload
        when {
            SearchScope.Mods in document.scopes && payload is ConfigData -> {
                val icon = payload.icon?.takeIf(::canRenderIcon)
                add(PaletteHit("mod:${payload.id}", PaletteGroup.Mods, title, null, icon ?: "settings") { nav, _ -> openMod(payload, nav) })
            }
            SearchScope.Keybinds in document.scopes -> {
                if (!isShownKeybind(payload)) continue
                add(
                    PaletteHit("keybind:${document.id}", PaletteGroup.Keybinds, title, document.metadata.modTitle, "keyboard") { nav, search ->
                        nav.navigate(KeybindsGraph)
                        search.update(title)
                    },
                )
            }
            SearchScope.Options in document.scopes -> {
                val row = GlobalSettingIndex.rowOf(document)?.takeIf { it.leadsTo(payload) } ?: continue
                val id = row.mod?.id
                val group = if (id == PREFERENCES_ID || id == THEMES_ID) PaletteGroup.OneConfig else PaletteGroup.ModSettings
                val location = listOfNotNull(row.modTitle, row.groupLabel?.takeIf { it.isNotBlank() }).joinToString(" › ").ifBlank { null }
                add(PaletteHit("option:${document.id}", group, title, location, "settings") { nav, _ -> openOption(row, payload, nav) })
            }
        }
    }
    return out
        .sortedWith(compareBy<PaletteHit> { it.group.ordinal }.thenBy { matchRank(it.title, query) })
        .groupBy { it.group }
        .values
        .flatten()
}

private fun matchRank(title: String, query: String): Int = when {
    title.startsWith(query, ignoreCase = true) -> 0
    title.split(' ').any { it.startsWith(query, ignoreCase = true) } -> 1
    title.contains(query, ignoreCase = true) -> 2
    else -> 3
}

private fun openMod(mod: ConfigData, navigation: ShellNavigation) {
    val onOpen = mod.onOpen
    if (onOpen != null) onOpen() else navigation.navigate(ModConfigRoute(mod.id))
}

private fun openOption(row: SearchRow, anchor: Any?, navigation: ShellNavigation) {
    val id = row.mod?.id ?: return
    anchor?.let(navigation::jumpAfterNextNavigation)
    when {
        row.submenu != null -> navigation.navigate(ModConfigRoute(id, submenu = row.submenu))
        id == PREFERENCES_ID -> navigation.navigate(PreferencesGraph)
        id == THEMES_ID -> navigation.navigate(ThemesGraph)
        else -> navigation.navigate(ModConfigRoute(id))
    }
}

internal fun paletteKeyHint(): String? = OneConfigConfig.paletteKeybind?.takeIf { it.isBound }?.displayName()
