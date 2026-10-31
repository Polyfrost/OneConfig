package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.internal.ui.api.HotwireBridge
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.CheckboxIndicator
import org.polyfrost.oneconfig.internal.ui.navigation.graph.HotwireAddRoute
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.ShellController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyGridState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** Whether mods show as full-width rows rather than cards, kept for the session */
private var listLayout by mutableStateOf(false)

/**
 * The updates Hotwire found on Modrinth, checked once per session and again whenever an update or add made here
 * finishes, so the Hotwire page and the Updates tab share one answer instead of each asking Modrinth
 */
object HotwireUpdates {
    /** Null until the first check has answered */
    var found by mutableStateOf<Result<List<HotwireBridge.StoreUpdate>>?>(null)
        private set

    val modIds: Set<String> get() = found?.getOrNull()?.mapTo(HashSet()) { it.modId }.orEmpty()

    /** The name of the update the running batch is on, or null when no batch is running */
    var updating by mutableStateOf<String?>(null)
        private set

    private var checked = false

    private val scope = CoroutineScope(SupervisorJob() + Executor { Platform.screen().runOnUiThread(it) }.asCoroutineDispatcher())

    /** Checks on first use only */
    fun ensure() {
        if (!checked) refreshAfter {}
    }

    /**
     * Re-checks once the change Hotwire was just given has finished: when it stops working after having started, or
     * after a few seconds if it never reports progress for it
     *
     * Runs on its own thread so leaving the page doesn't cancel it
     */
    fun refreshWhenDone() = refreshAfter(HotwireBridge::awaitIdle)

    private fun refreshAfter(wait: () -> Unit) {
        checked = true
        Thread({
            wait()
            val result = runCatching { HotwireBridge.findUpdates() }
            Platform.screen().runOnUiThread { apply(result) }
        }, "OneConfig Hotwire update check").apply { isDaemon = true }.start()
    }

    /**
     * Applies each update in turn with whatever Hotwire allows it, live or at the next launch, since Hotwire's own
     * batch update waits on confirmations in its screen
     */
    fun updateAll(updates: List<HotwireBridge.StoreUpdate>, onDone: () -> Unit) {
        scope.launch {
            val problems = ArrayList<String>()
            try {
                for (update in updates) {
                    updating = update.name
                    val plan = withContext(Dispatchers.IO) {
                        runCatching { HotwireBridge.planUpdate(HotwireBridge.downloadUpdate(update)) }
                    }
                    val action = plan.getOrNull()?.actions?.firstOrNull { it.primary }
                    if (action == null) {
                        val reason = plan.exceptionOrNull()?.let { it.cause ?: it }?.message ?: plan.getOrNull()?.lines?.firstOrNull()
                        problems += "${update.name}: ${reason.orEmpty()}"
                        continue
                    }
                    val failed = action.attempt()?.let { withContext(Dispatchers.IO) { it.get() } }?.takeUnless { it.ok }
                    if (failed != null) {
                        problems += "${update.name}: ${failed.message}"
                        continue
                    }
                    withContext(Dispatchers.IO) { HotwireBridge.awaitIdle() }
                }
            } finally {
                updating = null
                refreshWhenDone()
            }
            if (problems.isEmpty()) {
                Notifications.success("Hotwire", translated("oneconfig.page.hotwire.add.sent", "Sent to Hotwire. It posts the result in chat."))
                onDone()
            } else {
                Notifications.error("Hotwire", problems.joinToString(" "))
            }
        }
    }

    private fun apply(result: Result<List<HotwireBridge.StoreUpdate>>) {
        // a failed re-check keeps the last list shown, and the next use tries again
        if (result.isFailure) checked = false
        if (result.isSuccess || found == null) found = result
    }
}

/** Extracted icon path per mod id kept across scrolling so each jar is only read once */
private val iconPaths = HashMap<String, String?>()

/**
 * Every mod seen this session by id, starting from the startup list
 *
 * Hotwire takes a mod off the loader's list when it turns it off, so this keeps its icon and parent for the grid
 */
private val seenMods by lazy { ModInfo.loadedMods.associateByTo(HashMap()) { it.id } }

/**
 * The mods the grid shows: the loader's live list, the mods seen earlier that Hotwire still knows about, and any
 * other mod Hotwire knows, such as one it kept off since the last launch
 */
private fun gridMods(states: Map<String, HotwireBridge.ModState>?): Collection<ModInfo> {
    // read past ModInfo's cache, since Hotwire can add mods after it was filled
    val live = Platform.compatibility().mods
    live.forEach { seenMods[it.id] = it }
    val liveIds = live.mapTo(HashSet()) { it.id }
    val known = seenMods.values.filter { it.id in liveIds || states?.containsKey(it.id) == true }
    val unseen = states.orEmpty().values
        .filter { it.id !in seenMods }
        .map { ModInfo(it.id, it.name.ifBlank { it.id }, it.version, null, null) }
    return known + unseen
}

/** Whether Hotwire is installed, which is the only time its page is offered */
val isHotwireInstalled: Boolean get() = ModInfo.loadedMods.any { it.id == "hotwire" }

/** The line beside the page title, crediting Hotwire's authors from its mod metadata */
fun hotwireTagline(): String {
    val author = ModInfo.loadedMods.firstOrNull { it.id == "hotwire" }?.authors?.takeIf { it.isNotBlank() }
    return listOfNotNull(
        translated("oneconfig.page.hotwire.tagline", "Manage your mods in real time."),
        author?.let { translated("oneconfig.page.hotwire.credit", "Made possible by %s", it) },
    ).joinToString(" ")
}

private val SwitchBarHeight = 56.dp

private const val PromptWaitSeconds = 5

/** The header's "Original menu" action on the Hotwire pages */
val openHotwireScreen = Runnable { HotwireBridge.openScreen() }

/** Reports what Hotwire says came of a change */
internal fun notifyHotwireResult(result: HotwireBridge.Result) {
    when {
        result.ok -> Notifications.success("Hotwire", result.message.ifBlank { "Done." })
        result.needsReopen -> Notifications.info("Hotwire", result.message.ifBlank { "Reopen your world to finish this." })
        else -> Notifications.error("Hotwire", result.message.ifBlank { "That didn't work." })
    }
}

/**
 * Every mod the loader has in a denser grid than the mods page, where picked mods are turned on or off live
 * through Hotwire
 */
@Composable
fun Hotwire() {
    var prompt by remember { mutableStateOf(HotwireBridge.openingPrompt()) }
    prompt?.let {
        OpeningPromptCard(it) { prompt = null }
        return
    }

    val revision = HotwireBridge.revision
    val states = remember(revision) { HotwireBridge.mods()?.associateBy { it.id } }
    LaunchedEffect(Unit) { HotwireUpdates.ensure() }
    val availableUpdates = HotwireUpdates.modIds
    val families = remember(states) {
        modFamilies(gridMods(states)).filterValues { members -> states == null || members.any { states[it.id]?.switchable == true } }
    }
    val query = ShellState.searchQuery.trim()
    val mods = remember(families, query) {
        families
            .filter { (_, members) ->
                query.isEmpty() || members.any { it.name.contains(query, true) || it.id.contains(query, true) }
            }
            .keys
            .sortedBy { it.name.lowercase() }
    }
    // picked families by their root id
    val selected = remember { mutableStateSetOf<String>() }
    val gridState = rememberRestorableLazyGridState("hotwire")
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = PageGutter), horizontalArrangement = Arrangement.End) {
            IconSegments(
                listOf(
                    IconSegment("grid", translated("oneconfig.page.hotwire.layout.grid", "Grid")),
                    IconSegment("list", translated("oneconfig.page.hotwire.layout.list", "List")),
                ),
                selected = if (listLayout) 1 else 0,
            ) { listLayout = it == 1 }
        }
        ScrollbarBox(gridState, PageGutter, Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                state = gridState,
                columns = if (listLayout) GridCells.Fixed(1) else GridCells.Adaptive(140.dp),
                verticalArrangement = Arrangement.spacedBy(if (listLayout) 8.dp else 19.dp),
                horizontalArrangement = Arrangement.spacedBy(19.dp),
                contentPadding = PaddingValues(horizontal = PageGutter),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(mods, key = { it.id }) { mod ->
                    HotwireTile(
                        mod,
                        families.getValue(mod),
                        on = familyOn(families.getValue(mod), states),
                        hasUpdate = families.getValue(mod).any { it.id in availableUpdates },
                        list = listLayout,
                        selected = mod.id in selected,
                        onSelect = { if (!selected.remove(mod.id)) selected.add(mod.id) },
                    )
                }
            }
        }
        Row(
            // a fixed height the switch bar fills, so the button stays centred on it and doesn't move when it appears
            Modifier.fillMaxWidth().padding(start = PageGutter, end = PageGutter, bottom = 19.dp).height(SwitchBarHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            val picked = families.filterKeys { it.id in selected }
            if (picked.isNotEmpty()) SwitchBar(picked, states, Modifier.weight(1f), onClear = { selected.clear() })
            Chip(translated("oneconfig.page.hotwire.add_mods", "Add mods"), selected = true, icon = "plus") {
                ShellController.shared.navigation.navigate(HotwireAddRoute)
            }
        }
    }
}

/** Hotwire's warning, which it requires be shown before it takes requests from this mod, with its reading delay */
@Composable
private fun OpeningPromptCard(prompt: HotwireBridge.OpeningPrompt, onDone: () -> Unit) {
    val theme = LocalTheme.current
    var wait by remember { mutableIntStateOf(minOf(prompt.waitSeconds, PromptWaitSeconds)) }
    LaunchedEffect(Unit) {
        while (wait > 0) {
            delay(1000)
            wait--
        }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = PageGutter), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 560.dp)
                .background(theme.modCardBackground, theme.modCardShape)
                .border(1.dp, theme.borderColor, theme.modCardShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(prompt.title, color = theme.textColor, fontSize = 18.sp, textAlign = TextAlign.Center)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                prompt.lines.forEach { Text(it, color = theme.textColor, fontSize = 13.sp, textAlign = TextAlign.Center) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (wait > 0) "${prompt.acceptText} ($wait)" else prompt.acceptText, selected = wait == 0) {
                    if (wait == 0) {
                        prompt.accept()
                        onDone()
                    }
                }
                Chip(prompt.openHotwireText, selected = false) {
                    if (wait == 0) {
                        prompt.acceptAndOpenHotwire()
                        onDone()
                    }
                }
            }
        }
    }
}

/**
 * Each top-level mod with every mod under it, however deep, so the grid shows one entry per family
 *
 * Members are listed root first, and a parent that is not loaded itself, as Mod Menu allows, still gets an entry
 * named after its id
 */
private fun modFamilies(mods: Collection<ModInfo>): Map<ModInfo, List<ModInfo>> {
    val byId = mods.associateBy { it.id }
    fun rootId(mod: ModInfo): String {
        var id = mod.id
        val seen = HashSet<String>()
        while (seen.add(id)) id = byId[id]?.parentId ?: break
        return id
    }
    return byId.values.groupBy(::rootId).entries.associate { (rootId, members) ->
        (byId[rootId] ?: ModInfo(rootId, rootId, "", null, null)) to members.sortedBy { it.id != rootId }
    }
}

/** Whether a family counts as on: its first member Hotwire can switch decides, which is the root unless that can't be */
private fun familyOn(family: List<ModInfo>, states: Map<String, HotwireBridge.ModState>?): Boolean? =
    family.firstNotNullOfOrNull { mod -> states?.get(mod.id)?.takeIf { it.switchable } }?.on

/** The picked mods and what switching them does, with the button that does it, as Hotwire's own screen offers */
@Composable
private fun SwitchBar(
    picked: Map<ModInfo, List<ModInfo>>,
    states: Map<String, HotwireBridge.ModState>?,
    modifier: Modifier = Modifier,
    onClear: () -> Unit,
) {
    val theme = LocalTheme.current
    // a picked entry switches as a whole, so every member Hotwire can switch goes along with its root
    fun idsWhere(on: Boolean) = picked.values
        .filter { familyOn(it, states) == on }
        .flatten()
        .filter { mod -> states?.get(mod.id)?.let { it.switchable && it.on == on } == true }
        .map { it.id }
    val turnOff = remember(picked, states) { idsWhere(true) }
    val turnOn = remember(picked, states) { idsWhere(false) }
    val previews = remember(turnOff, turnOn) {
        listOfNotNull(
            turnOff.takeIf { it.isNotEmpty() }?.let { HotwireBridge.preview(it, false) },
            turnOn.takeIf { it.isNotEmpty() }?.let { HotwireBridge.preview(it, true) },
        )
    }
    val pickedIds = picked.values.flatten().mapTo(HashSet()) { it.id }
    val updates = HotwireUpdates.found?.getOrNull().orEmpty().filter { it.modId in pickedIds }
    val updating = HotwireUpdates.updating
    var switching by remember { mutableStateOf(false) }
    val running = switching || updating != null
    var fraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        while (running) withFrameNanos { fraction = HotwireBridge.progress() }
    }

    val offCount = picked.values.count { familyOn(it, states) == true }
    val onCount = picked.values.count { familyOn(it, states) == false }
    val label = when {
        offCount > 0 && onCount > 0 -> "Switch ${offCount + onCount}"
        offCount > 0 -> "Turn off $offCount"
        else -> "Turn on $onCount"
    }
    val message = when {
        states == null -> "Hotwire is still starting, so mods can't be switched yet."
        updating != null -> translated("oneconfig.page.hotwire.updates.running", "Updating %s…", updating)
        running -> "Switching… ${(fraction * 100).toInt()}%"
        else -> previews.joinToString(" ") { it.message }
    }
    val canSwitch = !running && previews.isNotEmpty() && previews.all { it.possible } && !HotwireBridge.isSwitching

    fun start() {
        switching = true
        val off = if (turnOff.isEmpty()) done("") else HotwireBridge.switch(turnOff, false)
        off.thenCompose { first ->
            if (!first.ok || turnOn.isEmpty()) return@thenCompose CompletableFuture.completedFuture(first)
            HotwireBridge.switch(turnOn, true).thenApply { second ->
                HotwireBridge.Result(
                    second.ok,
                    listOf(first.message, second.message).filter { it.isNotBlank() }.joinToString(" "),
                    second.needsReopen,
                )
            }
        }.whenComplete { result, error ->
            Platform.screen().runOnUiThread {
                switching = false
                notifyHotwireResult(result ?: HotwireBridge.Result(false, error?.message ?: ""))
                if (result?.ok == true) onClear()
            }
        }
    }

    Row(
        modifier = modifier
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, theme.borderColor, theme.modCardShape)
            .fillMaxHeight()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${picked.size} selected", color = theme.textColor, fontSize = 14.sp)
            if (message.isNotBlank()) {
                Text(message, color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (running) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(theme.borderColor, theme.modCardShape)) {
                    Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Accent, theme.modCardShape))
                }
            }
        }
        Chip("Clear", selected = false, onClick = onClear)
        if (!running && updates.isNotEmpty()) {
            Chip(translated("oneconfig.page.hotwire.updates.bar", "Update %s", updates.size), selected = true) { HotwireUpdates.updateAll(updates, onClear) }
        }
        if (canSwitch) Chip(label, selected = true, onClick = ::start)
    }
}

private fun done(message: String) = CompletableFuture.completedFuture(HotwireBridge.Result(true, message))

@Composable
private fun HotwireTile(
    mod: ModInfo,
    family: List<ModInfo>,
    on: Boolean?,
    hasUpdate: Boolean,
    list: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val theme = LocalTheme.current
    val icon = remember(mod.id) { iconPaths.getOrPut(mod.id) { mod.extractIconFile() } }
    // mods Hotwire has turned off are greyed out but stay selectable so they can be turned back on
    val contentAlpha = if (on == false) 0.4f else 1f
    val nameColor = if (on == false) theme.textColorSecondary else theme.textColor

    Box(
        modifier = Modifier.fillMaxWidth().height(if (list) 56.dp else 124.dp)
            .background(theme.modCardBackground, theme.modCardShape)
            .then(
                if (selected) {
                    Modifier.border(2.dp, Accent, theme.modCardShape)
                } else {
                    Modifier.border(1.dp, remember(theme.borderColor) {
                        Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
                    }, theme.modCardShape)
                },
            )
            .onClick(rememberInteractionSource(), onClick = onSelect)
            .pointerHoverIcon(PointerIcon.Hand),
    ) {
        if (list) {
            Row(
                modifier = Modifier.fillMaxSize().alpha(contentAlpha).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CheckboxIndicator(selected, onToggle = onSelect)
                // keeps names lined up when a mod has no icon
                Box(Modifier.size(36.dp)) {
                    if (icon != null) Icon(icon, color = theme.textColor, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(mod.name, color = nameColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    mod.authors?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            translated("oneconfig.header.byline", "by %s", it),
                            color = theme.textColorSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (hasUpdate) {
                    Text(translated("oneconfig.page.hotwire.update_available", "Update available"), color = Accent, fontSize = 12.sp, maxLines = 1)
                }
                if (mod.version.isNotBlank()) {
                    Text(mod.version, color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().alpha(contentAlpha).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) Icon(icon, color = theme.textColor, modifier = Modifier.size(56.dp))
                Text(
                    mod.name,
                    color = nameColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                CheckboxIndicator(selected, onToggle = onSelect)
            }
            if (hasUpdate) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    Tooltip(
                        text = { Text(translated("oneconfig.page.hotwire.update_available", "Update available"), color = theme.textColor, fontSize = 12.sp) },
                        modifier = Modifier,
                        anchor = Alignment.TopCenter,
                    ) {
                        Box(Modifier.size(8.dp).background(Accent, theme.circleShape))
                    }
                }
            }
        }
    }
}
