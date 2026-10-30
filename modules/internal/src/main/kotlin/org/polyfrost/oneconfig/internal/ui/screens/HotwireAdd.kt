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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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

    fun leavePreview() {
        request++
        reading = false
        plan = null
    }

    /** Gets a jar and its icon off the UI thread, then asks Hotwire what it would do with it */
    fun review(name: String, icon: () -> String?, update: Boolean = false, jar: () -> Path?) {
        leavePreview()
        val id = request
        reading = true
        planIcon = null
        scope.launch {
            val (iconPath, picked) = withContext(Dispatchers.IO) { runCatching(icon).getOrNull() to runCatching(jar) }
            if (id != request) return@launch
            planIcon = iconPath
            val path = picked.getOrNull()
            if (path == null) {
                reading = false
                picked.exceptionOrNull()?.let { plan = HotwireBridge.JarPlan(name, "", listOf(it.message ?: it.toString()), emptyList()) }
                return@launch
            }
            HotwireBridge.planFor(path, update).whenComplete { result, _ ->
                Platform.screen().runOnUiThread {
                    if (id != request) return@runOnUiThread
                    reading = false
                    plan = result ?: HotwireBridge.JarPlan(name, "", listOf("Hotwire couldn't read this jar."), emptyList())
                }
            }
        }
    }

    fun show(next: AddSource) {
        leavePreview()
        source = next
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(Modifier.padding(horizontal = PageGutter), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(translated("oneconfig.page.hotwire.source.updates", "Updates"), source == AddSource.Updates, "refresh") { show(AddSource.Updates) }
            Chip(translated("oneconfig.page.hotwire.source.modrinth", "Modrinth"), source == AddSource.Modrinth, "search") { show(AddSource.Modrinth) }
            Chip(translated("oneconfig.page.hotwire.source.file", "From file"), source == AddSource.File, "folder") { show(AddSource.File) }
        }
        if (reading || plan != null) {
            PlanCard(reading, plan, planIcon, onBack = ::leavePreview)
        } else {
            when (source) {
                AddSource.File -> FileSource { jar -> review("", { null }, jar = jar) }
                AddSource.Modrinth -> ModrinthSource { hit, update ->
                    review(hit.title, { hit.iconUrl?.let { HotwireBridge.iconFor(it).toString() } }, update) { HotwireBridge.downloadLatest(hit.projectId) }
                }
                AddSource.Updates -> UpdatesSource { update ->
                    review(update.name, { installedIcon(update.modId) }, update = true) { HotwireBridge.downloadUpdate(update) }
                }
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

/** Modrinth projects matching the header search, or Modrinth's own listing while it is empty */
@Composable
private fun ModrinthSource(onInstall: (HotwireBridge.StoreHit, Boolean) -> Unit) {
    val theme = LocalTheme.current
    val query = ShellState.searchQuery.trim()
    var hits by remember { mutableStateOf<Result<List<HotwireBridge.StoreHit>>?>(null) }
    LaunchedEffect(query) {
        // waits out typing so each keystroke doesn't hit Modrinth
        delay(350)
        hits = withContext(Dispatchers.IO) { runCatching { HotwireBridge.searchModrinth(query) } }
    }
    // a failed check shows every hit rather than none
    val installed by produceState<Result<HotwireBridge.InstalledProjects>?>(null) {
        value = withContext(Dispatchers.IO) { runCatching { HotwireBridge.installedProjects() } }
    }

    val current = hits
    val known = installed
    when {
        current == null || known == null -> Card { Text(translated("oneconfig.page.hotwire.modrinth.loading", "Searching Modrinth…"), color = theme.textColorSecondary, fontSize = 14.sp) }
        current.isFailure -> Card {
            Text(
                translated("oneconfig.page.hotwire.modrinth.failed", "Couldn't reach Modrinth: %s", current.exceptionOrNull()?.message ?: ""),
                color = theme.textColorSecondary,
                fontSize = 14.sp,
            )
        }
        else -> {
            // installed projects only show when there's a newer version to update to; the value says which is which
            val shown = remember(current, known) {
                val projects = known.getOrNull()
                current.getOrThrow().mapNotNull { hit ->
                    val installedHit = projects?.isInstalled(hit) == true
                    if (installedHit && hit.projectId !in projects.updatable) null else hit to installedHit
                }
            }
            known.exceptionOrNull()?.let {
                Text(
                    translated("oneconfig.page.hotwire.modrinth.installed_failed", "Couldn't check which mods are installed, so all results are shown: %s", it.message ?: it.toString()),
                    color = theme.textColorSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = PageGutter),
                )
            }
            ModrinthResults(shown, onInstall)
        }
    }
}

@Composable
private fun ModrinthResults(shown: List<Pair<HotwireBridge.StoreHit, Boolean>>, onInstall: (HotwireBridge.StoreHit, Boolean) -> Unit) {
    val theme = LocalTheme.current
    ResultList(shown) { (hit, update) ->
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
            Chip(
                if (update) translated("oneconfig.page.hotwire.modrinth.update", "Update") else translated("oneconfig.page.hotwire.modrinth.install", "Install"),
                selected = true,
            ) { onInstall(hit, update) }
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
private fun <T> ResultList(items: List<T>, row: @Composable androidx.compose.foundation.layout.RowScope.(T) -> Unit) {
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
        }
    }
}

/** What Hotwire would do with the chosen jar and the choices it offers, then its progress once one is picked */
@Composable
private fun PlanCard(reading: Boolean, plan: HotwireBridge.JarPlan?, icon: String?, onBack: () -> Unit) {
    val theme = LocalTheme.current
    var started by remember(plan) { mutableStateOf(false) }
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
                if (working) {
                    translated("oneconfig.page.hotwire.add.working", "Hotwire is working on it… %s%%", (fraction * 100).toInt())
                } else {
                    translated("oneconfig.page.hotwire.add.sent", "Sent to Hotwire. It posts the result in chat.")
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
                        action.run()
                        HotwireUpdates.refreshWhenDone()
                    }
                }
            }
            Chip(translated("oneconfig.page.hotwire.add.back", "Back"), selected = false, onClick = onBack)
        }
    }
}
