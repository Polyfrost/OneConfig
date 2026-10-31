package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.internal.ui.api.HotwireBridge
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private enum class AddSource { File, Modrinth, Updates }

/**
 * Adds or updates a mod through Hotwire from a jar on disk, a Modrinth project, or an update Hotwire found, always
 * showing what Hotwire would do before doing it, as Hotwire's own add and update panels offer
 */
@Composable
fun HotwireAdd() {
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(AddSource.Updates) }
    var reading by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<HotwireBridge.JarPlan?>(null) }
    var planIcon by remember { mutableStateOf<String?>(null) }
    // bumped whenever the preview is left, so a download still running then can't bring it back
    var request by remember { mutableIntStateOf(0) }

    // leaving the page drops a pending add the same way Back does
    DisposableEffect(Unit) { onDispose { plan?.onCancel?.invoke() } }

    fun leavePreview() {
        request++
        reading = false
        plan?.onCancel?.invoke()
        plan = null
    }

    /** Loads a plan and its icon off the UI thread, since they can wait on a dialog, a download or Modrinth */
    fun review(name: String, icon: () -> String?, load: () -> HotwireBridge.JarPlan?) {
        leavePreview()
        val id = request
        reading = true
        planIcon = null
        scope.launch {
            val (iconPath, loaded) = withContext(Dispatchers.IO) { runCatching(icon).getOrNull() to runCatching(load) }
            if (id != request) {
                loaded.getOrNull()?.onCancel?.invoke()
                return@launch
            }
            planIcon = iconPath
            reading = false
            plan = loaded.getOrElse {
                val reason = it.cause ?: it
                HotwireBridge.JarPlan(name, "", listOf(reason.message ?: reason.toString()), emptyList())
            }
        }
    }

    fun show(next: AddSource) {
        leavePreview()
        source = next
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(translated("oneconfig.page.hotwire.source.updates", "Updates"), source == AddSource.Updates, "refresh") { show(AddSource.Updates) }
            Chip(translated("oneconfig.page.hotwire.source.modrinth", "Modrinth"), source == AddSource.Modrinth, "search") { show(AddSource.Modrinth) }
            Chip(translated("oneconfig.page.hotwire.source.file", "From file"), source == AddSource.File, "folder") { show(AddSource.File) }
            Spacer(Modifier.weight(1f))
            Chip(translated("oneconfig.page.hotwire.downloads_folder", "Downloads folder"), selected = false, icon = "external-link") { HotwireBridge.openDownloads() }
        }
        fun reviewUpdate(update: HotwireBridge.StoreUpdate, icon: () -> String?) =
            review(update.name, icon) { HotwireBridge.planUpdate(HotwireBridge.downloadUpdate(update)) }

        if (reading || plan != null) {
            PlanCard(reading, plan, planIcon, onBack = ::leavePreview)
        } else {
            when (source) {
                AddSource.File -> FileSource { jar -> review("", { null }) { jar()?.let { HotwireBridge.planFor(it).get() } } }
                AddSource.Modrinth -> ModrinthSource(
                    onInstall = { hit -> review(hit.title, { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } }) { HotwireBridge.storePlan(hit) } },
                    onUpdate = { update, hit -> reviewUpdate(update) { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } } },
                )
                AddSource.Updates -> UpdatesSource { update -> reviewUpdate(update) { installedIcon(update.modId) } }
            }
        }
    }
}

/** The icon of an installed mod, read out of its jar */
private fun installedIcon(modId: String): String? = ModInfo.loadedMods.firstOrNull { it.id == modId }?.extractIconFile()

/** A row's icon, loaded off the UI thread so scrolling never waits on a jar or the network */
@Composable
private fun RowIcon(key: Any, load: () -> String?) {
    val path by produceState<String?>(null, key) { value = withContext(Dispatchers.IO) { runCatching(load).getOrNull() } }
    // keeps names lined up while an icon loads or when a mod has none
    Box(Modifier.size(36.dp)) {
        path?.let { Icon(it, color = LocalTheme.current.textColor, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Column(
        Modifier.padding(horizontal = PageGutter).fillMaxWidth()
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, theme.borderColor, theme.modCardShape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

/** Picks a jar with the system file dialog, handing back a way to read its path */
@Composable
private fun FileSource(onPicked: (() -> Path?) -> Unit) {
    val theme = LocalTheme.current
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon("plus", color = theme.textColor, modifier = Modifier.size(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(translated("oneconfig.page.hotwire.add.title", "Add or update a mod"), color = theme.textColor, fontSize = 18.sp)
                Text(
                    translated(
                        "oneconfig.page.hotwire.add.hint",
                        "Pick a mod jar. Hotwire shows what it would change before anything happens, and updates the mod if it's already installed.",
                    ),
                    color = theme.textColorSecondary,
                    fontSize = 13.sp,
                )
            }
        }
        Row {
            Chip(translated("oneconfig.page.hotwire.add.pick", "Choose a mod jar"), selected = true, icon = "folder") {
                onPicked {
                    TinyFdApi.getInstance().openFileSelector(
                        translated("oneconfig.page.hotwire.add.pick", "Choose a mod jar"),
                        null,
                        arrayOf("*.jar"),
                        "Mod jars",
                    )
                }
            }
        }
    }
}

/** A name with only what identifies it, so "Fabric Language Kotlin" and fabric-language-kotlin are the same */
private fun String.identity() = lowercase().filter(Char::isLetterOrDigit)

/** A category's name from Hotwire's own translations, or the tag made readable, where a blank tag means every category */
@Composable
private fun categoryLabel(tag: String): String {
    val key = tag.ifBlank { "all" }
    return translated("hotwire.store.category.$key", key.replace('-', ' ').replaceFirstChar { it.uppercase() })
}

/** Modrinth mods matching the header search, or Modrinth's own listing while it is empty */
@Composable
private fun ModrinthSource(
    onInstall: (HotwireBridge.StoreHit) -> Unit,
    onUpdate: (HotwireBridge.StoreUpdate, HotwireBridge.StoreHit) -> Unit,
) {
    val theme = LocalTheme.current
    val query = ShellState.searchQuery.trim()
    val categories = remember { HotwireBridge.storeCategories() }
    var group by remember { mutableStateOf(categories.firstOrNull()) }
    var category by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<Result<List<HotwireBridge.StoreHit>>?>(null) }
    var total by remember { mutableIntStateOf(0) }
    var searched by remember { mutableStateOf<Pair<String, String>?>(null) }
    var offset by remember(query, category) { mutableIntStateOf(0) }
    LaunchedEffect(query, category, offset) {
        // waits out typing so each keystroke doesn't hit Modrinth
        if (offset == 0) delay(350)
        val earlier = if (offset == 0) emptyList() else hits?.getOrNull().orEmpty()
        val page = withContext(Dispatchers.IO) { runCatching { HotwireBridge.searchStore(query, category, offset) } }
        total = page.getOrNull()?.total ?: 0
        hits = page.map { earlier + it.hits }
        searched = query to category
    }
    LaunchedEffect(Unit) { HotwireUpdates.ensure() }

    Row(Modifier.padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach {
            Chip(categoryLabel(it.parts.singleOrNull() ?: it.name), group === it) {
                group = it
                category = it.parts.firstOrNull() ?: it.name
            }
        }
    }
    // only a group of several categories has anything to choose between
    group?.parts?.takeIf { it.size > 1 }?.let { parts ->
        Row(Modifier.padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { Chip(categoryLabel(it), category == it) { category = it } }
        }
    }

    val current = hits
    when {
        current == null -> Card { Text(translated("oneconfig.page.hotwire.modrinth.loading", "Searching Modrinth…"), color = theme.textColorSecondary, fontSize = 14.sp) }
        current.isFailure -> Card {
            val reason = current.exceptionOrNull()?.let { it.cause ?: it }
            Text(
                translated("oneconfig.page.hotwire.modrinth.failed", "Couldn't reach Modrinth: %s", reason?.message ?: ""),
                color = theme.textColorSecondary,
                fontSize = 14.sp,
            )
        }
        else -> {
            val updates = HotwireUpdates.found?.getOrNull().orEmpty()
            val shown = remember(current, updates) {
                // Hotwire only knows jars Modrinth recognises by hash, so also match what the loader says is running by
                // id and name, which catches dev builds and projects whose slug isn't their mod id
                val installed = HashSet<String>()
                Platform.compatibility().mods.forEach { installed += it.id.identity(); installed += it.name.identity() }
                HotwireBridge.mods()?.forEach { installed += it.id.identity(); installed += it.name.identity() }
                current.getOrThrow().mapNotNull { hit ->
                    val update = updates.firstOrNull { it.projectId == hit.projectId }
                    val isInstalled = hit.installed || listOf(hit.slug, hit.projectId, hit.title).any { it.identity() in installed }
                    // installed projects only show when there's a newer version to update to
                    if (update == null && isInstalled) null else hit to update
                }
            }
            val loaded = current.getOrThrow().size
            val footer: @Composable () -> Unit = {
                if (searched == query to category && offset < loaded && loaded < total) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Chip(translated("oneconfig.page.hotwire.modrinth.more", "Load more"), selected = false) { offset = loaded }
                    }
                }
            }
            ResultList(shown, footer) { (hit, update) ->
                RowIcon(hit.projectId) { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(hit.title, color = theme.textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(hit.author.takeIf { it.isNotBlank() }?.let { translated("oneconfig.header.byline", "by %s", it) }, hit.description)
                            .filterNotNull().joinToString(" · "),
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(translated("oneconfig.page.hotwire.modrinth.downloads", "%s downloads", "%,d".format(hit.downloads)), color = theme.textColorSecondary, fontSize = 12.sp)
                when {
                    update != null -> Chip(translated("oneconfig.page.hotwire.modrinth.update", "Update"), selected = true) { onUpdate(update, hit) }
                    else -> Chip(translated("oneconfig.page.hotwire.modrinth.install", "Install"), selected = true) { onInstall(hit) }
                }
            }
        }
    }
}

/** Updates Hotwire found on Modrinth for the installed mods, from the session's shared [HotwireUpdates] */
@Composable
private fun UpdatesSource(onUpdate: (HotwireBridge.StoreUpdate) -> Unit) {
    val theme = LocalTheme.current
    LaunchedEffect(Unit) { HotwireUpdates.ensure() }

    val current = HotwireUpdates.found
    when {
        current == null -> Card { Text(translated("oneconfig.page.hotwire.updates.checking", "Checking for updates…"), color = theme.textColorSecondary, fontSize = 14.sp) }
        current.isFailure -> Card {
            Text(
                translated("oneconfig.page.hotwire.updates.failed", "Couldn't check for updates: %s", current.exceptionOrNull()?.message ?: ""),
                color = theme.textColorSecondary,
                fontSize = 14.sp,
            )
        }
        current.getOrThrow().isEmpty() -> Card {
            Text(translated("oneconfig.page.hotwire.updates.none", "Everything is up to date."), color = theme.textColorSecondary, fontSize = 14.sp)
        }
        else -> ResultList(current.getOrThrow()) { update ->
            RowIcon(update.modId) { installedIcon(update.modId) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(update.name, color = theme.textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${update.installedVersion} → ${update.newVersion}", color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Chip(translated("oneconfig.page.hotwire.modrinth.update", "Update"), selected = true) { onUpdate(update) }
        }
    }
}

/** A scrolling column of rows styled like the Hotwire page's list view */
@Composable
private fun <T> ResultList(items: List<T>, footer: @Composable () -> Unit = {}, row: @Composable RowScope.(T) -> Unit) {
    val theme = LocalTheme.current
    val listState = rememberLazyListState()
    ScrollbarBox(listState, PageGutter, Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = PageGutter),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items) { item ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp)
                        .background(theme.modCardBackground, theme.modCardShape)
                        .border(1.dp, theme.borderColor, theme.modCardShape)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) { row(item) }
            }
            item { footer() }
        }
    }
}

/** What Hotwire would do with the chosen jar and the choices it offers, then its progress once one is picked */
@Composable
private fun PlanCard(reading: Boolean, plan: HotwireBridge.JarPlan?, icon: String?, onBack: () -> Unit) {
    val theme = LocalTheme.current
    var started by remember(plan) { mutableStateOf(false) }
    // the outcome of an action that reports one, and whether one is on its way
    var outcome by remember(plan) { mutableStateOf<HotwireBridge.Result?>(null) }
    var awaiting by remember(plan) { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(started) {
        while (started) withFrameNanos {
            working = HotwireBridge.isWorking()
            fraction = HotwireBridge.progress()
        }
    }

    Card {
        if (reading || plan == null) {
            Text(translated("oneconfig.page.hotwire.add.reading", "Reading the jar…"), color = theme.textColorSecondary, fontSize = 14.sp)
            return@Card
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (icon != null) Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plan.title, color = theme.textColor, fontSize = 18.sp)
                if (plan.detail.isNotBlank()) Text(plan.detail, color = theme.textColorSecondary, fontSize = 13.sp)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            plan.lines.forEach { Text("• $it", color = theme.textColor, fontSize = 13.sp) }
        }
        if (started) {
            Text(
                when {
                    outcome != null -> outcome!!.message
                    working -> translated("oneconfig.page.hotwire.add.working", "Hotwire is working on it… %s%%", (fraction * 100).toInt())
                    awaiting -> translated("oneconfig.page.hotwire.add.downloading", "Downloading…")
                    else -> translated("oneconfig.page.hotwire.add.sent", "Sent to Hotwire. It posts the result in chat.")
                },
                color = theme.textColorSecondary,
                fontSize = 13.sp,
            )
            if (working) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(theme.borderColor, theme.modCardShape)) {
                    Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Accent, theme.modCardShape))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!started) {
                plan.actions.forEach { action ->
                    Chip(action.label, selected = action.primary) {
                        started = true
                        awaiting = true
                        val pending = action.attempt()
                        awaiting = pending != null
                        pending?.whenComplete { result, error ->
                            Platform.screen().runOnUiThread {
                                val done = result ?: HotwireBridge.Result(false, (error?.cause ?: error)?.message ?: "That didn't work.")
                                outcome = done
                                awaiting = false
                                notifyHotwireResult(done)
                                HotwireUpdates.refreshWhenDone()
                            }
                        }
                        if (pending == null) HotwireUpdates.refreshWhenDone()
                    }
                }
            }
            Chip(translated("oneconfig.page.hotwire.add.back", "Back"), selected = false, onClick = onBack)
        }
    }
}
