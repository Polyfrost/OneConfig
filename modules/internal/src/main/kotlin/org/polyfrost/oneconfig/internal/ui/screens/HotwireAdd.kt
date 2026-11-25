package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.api.ui.v1.components.Card
import org.polyfrost.oneconfig.api.ui.v1.components.Chip
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.ProgressBar
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.internal.ui.api.HotwireBridge
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

private enum class AddSource { File, Modrinth, Updates }

/**
 * Adds or updates a mod through Hotwire from a jar on disk, a Modrinth project, or an update Hotwire found, always
 * showing what Hotwire would do before doing it, as Hotwire's own add and update panels offer
 */
@Composable
fun HotwireAdd() {
    val bridge = HotwireBridge.instance ?: return HotwireUnavailable()
    // planning a change is refused until the prompt was shown, and this page can be reached without the Hotwire page
    var prompt by remember { mutableStateOf(bridge.openingPrompt()) }
    prompt?.let {
        OpeningPromptCard(it) { prompt = null }
        return
    }

    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf(AddSource.Modrinth) }
    val browse = remember { ModrinthBrowse() }
    var reading by remember { mutableStateOf(false) }
    var plan by remember { mutableStateOf<HotwireBridge.JarPlan?>(null) }
    var planIcon by remember { mutableStateOf<String?>(null) }
    // bumped whenever the preview is left, so a download still running then can't bring it back
    var request by remember { mutableIntStateOf(0) }
    // the tag of the downloads the plan being read starts, which are the ones its wait shows and stops
    var tag by remember { mutableStateOf("") }
    // whether one of the plan's actions was picked, after which the card shows how it went
    var started by remember { mutableStateOf(false) }
    val revision = HotwireBridge.revision
    var planRevision by remember { mutableIntStateOf(revision) }

    fun dropPending() {
        if (reading) bridge.stopDownloads(tag)
        plan?.onCancel?.invoke()
    }

    // leaving the page drops a pending add the same way Back does
    DisposableEffect(Unit) { onDispose { dropPending() } }

    fun leavePreview() {
        request++
        dropPending()
        reading = false
        plan = null
        started = false
    }

    // a plan only holds for the mods there were when it was made, so one not acted on is left once Hotwire changed mods
    LaunchedEffect(revision) {
        if (revision != planRevision && !started && (reading || plan != null)) leavePreview()
        planRevision = revision
    }

    /** Loads a plan and its icon off the UI thread, since they can wait on a dialog, a download or Modrinth */
    fun review(name: String, icon: () -> String?, load: (String) -> CompletableFuture<HotwireBridge.JarPlan>?) {
        leavePreview()
        val id = request
        val planTag = HotwireBridge.newTag()
        tag = planTag
        planRevision = HotwireBridge.revision
        reading = true
        planIcon = null
        scope.launch {
            val iconPath = withContext(Dispatchers.IO) { runCatching(icon).getOrNull() }
            if (id == request) planIcon = iconPath
        }
        scope.launch {
            val loaded = HotwireBridge.catching { withContext(Dispatchers.IO) { load(planTag) }?.await() }
            if (id != request) {
                loaded.getOrNull()?.onCancel?.invoke()
                return@launch
            }
            reading = false
            plan = loaded.fold({ it }) {
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
            Chip(translated("oneconfig.page.hotwire.source.modrinth", "Modrinth"), source == AddSource.Modrinth, { show(AddSource.Modrinth) }, icon = "search")
            Chip(translated("oneconfig.page.hotwire.source.updates", "Updates"), source == AddSource.Updates, { show(AddSource.Updates) }, icon = "refresh")
            Chip(translated("oneconfig.page.hotwire.source.file", "From file"), source == AddSource.File, { show(AddSource.File) }, icon = "folder")
            Spacer(Modifier.weight(1f))
            Chip(translated("oneconfig.page.hotwire.downloads_folder", "Downloads folder"), false, bridge::openDownloads, icon = "external-link")
        }
        fun reviewUpdate(update: HotwireBridge.StoreUpdate, icon: () -> String?) = review(update.name, icon) { bridge.planUpdate(listOf(update), it) }

        if (reading || plan != null) {
            PlanCard(bridge, reading, plan, planIcon, tag, started, onStart = { started = true }, onBack = ::leavePreview)
        } else {
            when (source) {
                AddSource.File -> FileSource { jar -> review("", { null }) { jar()?.let(bridge::planFor) } }
                AddSource.Modrinth -> ModrinthSource(
                    bridge,
                    browse,
                    onInstall = { hit -> review(hit.title, { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } }) { bridge.storePlan(hit, it) } },
                    onUpdate = { update, hit -> reviewUpdate(update) { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } } },
                )
                AddSource.Updates -> UpdatesSource(bridge) { update -> reviewUpdate(update) { modIconPath(bridge, update.modId, update.installedVersion) } }
            }
        }
    }
}

/** A row's icon, loaded off the UI thread so scrolling never waits on a jar or the network */
@Composable
private fun RowIcon(key: Any, load: () -> String?) {
    val path by produceState<String?>(null, key) {
        // a row reused for another item mustn't keep the last one's icon while this one loads
        value = null
        value = withContext(Dispatchers.IO) { runCatching(load).getOrNull() }
    }
    // keeps names lined up while an icon loads or when a mod has none
    Box(Modifier.size(36.dp)) {
        path?.let { Icon(it, color = LocalTheme.current.textColor, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
private fun AddCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.padding(horizontal = PageGutter).fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

/** Picks a jar with the system file dialog, handing back a way to read its path */
@Composable
private fun FileSource(onPicked: (() -> Path?) -> Unit) {
    val theme = LocalTheme.current
    AddCard {
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
            TextButton(translated("oneconfig.page.hotwire.add.pick", "Choose a mod jar"), "folder", {
                onPicked {
                    TinyFdApi.getInstance().openFileSelector(
                        translated("oneconfig.page.hotwire.add.pick", "Choose a mod jar"),
                        null,
                        arrayOf("*.jar"),
                        translated("oneconfig.page.hotwire.add.pick_filter", "Mod jars"),
                    )
                }
            }, style = ControlStyle.Primary)
        }
    }
}

/** A category's or [section]'s name from Hotwire's own translations, or the tag made readable, where a blank tag means every category */
private fun categoryLabel(tag: String, section: Boolean = false): String {
    val key = tag.ifBlank { "all" }
    return translated("hotwire.store.${if (section) "section" else "category"}.$key", key.replace('-', ' ').replaceFirstChar { it.uppercase() })
}

/** What the Modrinth tab found and where it was, kept while a plan takes its place */
private class ModrinthBrowse {
    /** The name of the chosen group of categories, or null for the first */
    var group by mutableStateOf<String?>(null)
    var category by mutableStateOf("")
    var hits by mutableStateOf<Result<List<HotwireBridge.StoreHit>>?>(null)
    var total by mutableIntStateOf(0)

    /** The search [hits] answer, or null until one has */
    var searched by mutableStateOf<List<Any>?>(null)

    /** Where the next page starts once Load more is pressed, and where the last page [hits] hold started */
    var offset by mutableIntStateOf(0)
    var loadedFrom = -1
    var list by mutableStateOf(LazyListState())
}

/** Modrinth mods matching the header search, or Modrinth's own listing while it is empty */
@Composable
private fun ModrinthSource(
    bridge: HotwireBridge,
    browse: ModrinthBrowse,
    onInstall: (HotwireBridge.StoreHit) -> Unit,
    onUpdate: (HotwireBridge.StoreUpdate, HotwireBridge.StoreHit) -> Unit,
) {
    // Hotwire refuses a download from Modrinth until its warning was read
    var warning by remember { mutableStateOf(bridge.downloadsWarning()) }
    warning?.let {
        OpeningPromptCard(it) { warning = null }
        return
    }

    val query = pageSearchQuery().trim()
    val categories = remember { bridge.storeCategories() }
    // what is installed decides what shows, so a change Hotwire made searches again
    val search = listOf(query, browse.category, HotwireBridge.revision)
    LaunchedEffect(search, browse.offset) {
        when {
            // a new search starts the list over, which runs this again
            browse.searched != search && browse.offset != 0 -> browse.offset = 0
            browse.searched != search -> {
                // waits out typing so each keystroke doesn't hit Modrinth
                delay(350)
                val page = HotwireBridge.catching { bridge.searchStore(query, browse.category, 0).await() }
                browse.total = page.getOrNull()?.total ?: 0
                browse.hits = page.map { it.hits }
                browse.list = LazyListState()
                browse.loadedFrom = 0
                // a failed search is tried again once the tab is back
                browse.searched = search.takeIf { page.isSuccess }
            }
            browse.offset > browse.loadedFrom -> {
                val earlier = browse.hits?.getOrNull().orEmpty()
                val page = HotwireBridge.catching { bridge.searchStore(query, browse.category, browse.offset).await() }
                browse.total = page.getOrNull()?.total ?: 0
                browse.hits = page.map { earlier + it.hits }
                browse.loadedFrom = browse.offset
            }
        }
    }
    LaunchedEffect(HotwireBridge.revision) { HotwireUpdates.ensure() }

    val group = categories.firstOrNull { it.name == browse.group } ?: categories.firstOrNull()
    Row(Modifier.padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach {
            val single = it.parts.singleOrNull()
            // only a group of several categories is a section, while one of none or one is a category itself
            val label = if (it.parts.size > 1) categoryLabel(it.name, section = true) else categoryLabel(single ?: it.name)
            Chip(label, group?.name == it.name, {
                browse.group = it.name
                browse.category = it.parts.firstOrNull() ?: it.name
            })
        }
    }
    // only a group of several categories has anything to choose between
    group?.parts?.takeIf { it.size > 1 }?.let { parts ->
        Row(Modifier.padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { Chip(categoryLabel(it), browse.category == it, { browse.category = it }) }
        }
    }

    val theme = LocalTheme.current
    val current = browse.hits
    when {
        current == null -> EmptyState(translated("oneconfig.page.hotwire.modrinth.loading", "Searching Modrinth…"), Modifier.fillMaxWidth(), state = LoadState.Loading)
        current.isFailure -> {
            val reason = current.exceptionOrNull()?.let { it.cause ?: it }
            EmptyState(translated("oneconfig.page.hotwire.modrinth.failed", "Couldn't reach Modrinth: %s", reason?.message ?: ""), Modifier.fillMaxWidth(), icon = "alert-circle")
        }
        else -> {
            val updates = HotwireUpdates.found?.getOrNull().orEmpty()
            val shown = remember(current, updates) {
                current.getOrThrow().mapNotNull { hit ->
                    val update = updates.firstOrNull { it.projectId == hit.projectId }
                    if (update == null && hit.installed) null else hit to update
                }
            }
            val loaded = current.getOrThrow().size
            // what is already installed is left out, so a search can come back with nothing to show
            if (shown.isEmpty() && loaded >= browse.total) {
                EmptyState(translated("oneconfig.page.hotwire.modrinth.none", "Nothing to add here. Mods you already have are left out."), Modifier.fillMaxWidth(), icon = "search")
                return
            }
            val footer: @Composable () -> Unit = {
                if (browse.searched == search && browse.offset == browse.loadedFrom && loaded < browse.total) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(translated("oneconfig.page.hotwire.modrinth.more", "Load more"), { browse.offset = loaded })
                    }
                }
            }
            ResultList(shown, footer, browse.list) { (hit, update) ->
                RowIcon(hit.projectId) { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(hit.title, color = theme.textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(hit.author.takeIf { it.isNotBlank() }?.let { translated("oneconfig.header.byline", "by %s", it) }, hit.description).joinToString(" · "),
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(translated("oneconfig.page.hotwire.modrinth.downloads", "%s downloads", "%,d".format(hit.downloads)), color = theme.textColorSecondary, fontSize = 12.sp)
                if (update != null) {
                    TextButton(translated("oneconfig.page.hotwire.modrinth.update", "Update"), { onUpdate(update, hit) }, style = ControlStyle.Primary)
                } else {
                    TextButton(translated("oneconfig.page.hotwire.modrinth.install", "Install"), { onInstall(hit) }, style = ControlStyle.Primary)
                }
            }
        }
    }
}

/** Updates Hotwire found on Modrinth for the installed mods, from the session's shared [HotwireUpdates] */
@Composable
private fun UpdatesSource(bridge: HotwireBridge, onUpdate: (HotwireBridge.StoreUpdate) -> Unit) {
    val theme = LocalTheme.current
    LaunchedEffect(HotwireBridge.revision) { HotwireUpdates.ensure() }

    val query = pageSearchQuery().trim()
    val current = HotwireUpdates.found
    when {
        current == null -> EmptyState(translated("oneconfig.page.hotwire.updates.checking", "Checking for updates…"), Modifier.fillMaxWidth(), state = LoadState.Loading)
        current.isFailure -> EmptyState(
            translated("oneconfig.page.hotwire.updates.failed", "Couldn't check for updates: %s", current.exceptionOrNull()?.message ?: ""),
            Modifier.fillMaxWidth(),
            icon = "alert-circle",
        )
        current.getOrThrow().isEmpty() -> EmptyState(translated("oneconfig.page.hotwire.updates.none", "Everything is up to date."), Modifier.fillMaxWidth(), icon = "check-circle")
        else -> {
            val matching = current.getOrThrow().filter { query.isEmpty() || it.name.contains(query, true) || it.modId.contains(query, true) }
            if (matching.isEmpty()) {
                EmptyState(translated("oneconfig.page.hotwire.no_match", "Nothing matches."), Modifier.fillMaxWidth(), icon = "search")
                return
            }
            ResultList(matching) { update ->
                RowIcon(update.modId to update.installedVersion) { modIconPath(bridge, update.modId, update.installedVersion) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(update.name, color = theme.textColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${update.installedVersion} → ${update.newVersion}", color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(translated("oneconfig.page.hotwire.modrinth.update", "Update"), { onUpdate(update) }, style = ControlStyle.Primary)
            }
        }
    }
}

/** A scrolling column of rows styled like the Hotwire page's list view */
@Composable
private fun <T> ResultList(items: List<T>, footer: @Composable () -> Unit = {}, listState: LazyListState = rememberLazyListState(), row: @Composable RowScope.(T) -> Unit) {
    ScrollContainer(listState, Modifier.fillMaxSize(), gutter = PageGutter) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = PageGutter),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items) { item ->
                Card(Modifier.fillMaxWidth().height(56.dp)) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) { row(item) }
                }
            }
            item { footer() }
        }
    }
}

/** The wait for a plan, which for a Modrinth mod is Hotwire downloading it and what it needs */
@Composable
private fun PlanLoading(bridge: HotwireBridge, tag: String, onCancel: () -> Unit) {
    val theme = LocalTheme.current
    val download = rememberDownloadWatch(bridge, tag)
    val current = download.progress
    val percent = (download.fraction * 100).toInt()
    Text(
        when {
            current == null -> translated("oneconfig.page.hotwire.add.reading", "Reading the jar…")
            current.paused -> translated("oneconfig.page.hotwire.add.paused", "Paused at %s%%", percent)
            else -> translated("oneconfig.page.hotwire.add.downloading", "Downloading… %s%%", percent)
        },
        color = theme.textColorSecondary,
        fontSize = 14.sp,
    )
    if (current != null) ProgressBar(download.fraction, Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (current != null) {
            val label = if (current.paused) translated("oneconfig.page.hotwire.add.resume", "Resume") else translated("oneconfig.page.hotwire.add.pause", "Pause")
            TextButton(label, { bridge.pauseDownloads(tag, !current.paused) })
        }
        TextButton(translated("oneconfig.page.hotwire.add.cancel", "Cancel"), onCancel)
    }
}

/** What Hotwire would do with the chosen jar and the choices it offers, then its progress once one is picked */
@Composable
private fun PlanCard(
    bridge: HotwireBridge,
    reading: Boolean,
    plan: HotwireBridge.JarPlan?,
    icon: String?,
    tag: String,
    started: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalTheme.current
    // the outcome of the chosen action once Hotwire reports it
    var outcome by remember(plan) { mutableStateOf<HotwireBridge.Result?>(null) }
    var working by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(started, outcome) {
        working = false
        while (started && outcome == null) {
            withFrameNanos {
                val progress = bridge.progress()
                working = progress != null
                fraction = progress ?: 0f
            }
        }
    }

    AddCard {
        if (reading || plan == null) {
            PlanLoading(bridge, tag, onCancel = onBack)
            return@AddCard
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (icon != null) Icon(icon, color = theme.textColor, modifier = Modifier.size(48.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plan.title, color = theme.textColor, fontSize = 18.sp)
                if (plan.detail.isNotBlank()) Text(plan.detail, color = theme.textColorSecondary, fontSize = 13.sp)
            }
        }
        // scrolls on its own, so a plan with many lines can't push its buttons out of the card
        ScrollContainer(modifier = Modifier.weight(1f, fill = false)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                plan.lines.forEach { Text("• $it", color = theme.textColor, fontSize = 13.sp) }
            }
        }
        if (started) {
            val done = outcome
            Text(
                when {
                    done != null -> bridge.describe(done)
                    working -> translated("oneconfig.page.hotwire.add.working", "Hotwire is working on it… %s%%", (fraction * 100).toInt())
                    else -> translated("oneconfig.page.hotwire.add.waiting", "Waiting for Hotwire…")
                },
                color = theme.textColorSecondary,
                fontSize = 13.sp,
            )
            if (working) ProgressBar(fraction, Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(translated("oneconfig.page.hotwire.add.back", "Back"), onBack)
            if (!started) {
                plan.actions.forEach { action ->
                    TextButton(action.label, {
                        onStart()
                        action.attempt().whenComplete { result, _ ->
                            Platform.screen().runOnUiThread {
                                outcome = result
                                notifyHotwireResult(bridge, result)
                                HotwireUpdates.refreshWhenDone()
                            }
                        }
                    }, style = if (action.primary) ControlStyle.Primary else ControlStyle.Secondary)
                }
            }
        }
    }
}
