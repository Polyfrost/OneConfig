package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.notifications.v1.NotificationAction
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.components.Card
import org.polyfrost.oneconfig.api.ui.v1.components.Checkbox
import org.polyfrost.oneconfig.api.ui.v1.components.Chip
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.DialogTitle
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.ModalDialog
import org.polyfrost.oneconfig.api.ui.v1.components.ProgressBar
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.internal.ui.api.HotwireBridge
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.HotwireAddRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyGridState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/** Whether mods show as full-width rows rather than tiles, kept for the session */
private var listLayout by mutableStateOf(false)

/**
 * The updates Hotwire found on Modrinth, checked once per session and again whenever an update or add made here
 * finishes or Hotwire switched mods since, so the Hotwire page and the Updates tab share one answer instead of each
 * asking Modrinth
 */
object HotwireUpdates {
    // outlives the page, so leaving it doesn't cancel a batch that is being applied
    private val uiScope = CoroutineScope(SupervisorJob() + Executor { Platform.screen().runOnUiThread(it) }.asCoroutineDispatcher())

    /** Null until the first check has answered */
    var found by mutableStateOf<Result<List<HotwireBridge.StoreUpdate>>?>(null)
        private set

    val modIds: Set<String> get() = found?.getOrNull()?.mapTo(HashSet()) { it.modId }.orEmpty()

    /** The name of the update the running batch is on, or null when no batch is running */
    var updating by mutableStateOf<String?>(null)
        private set

    // the revision the last check saw, so one made before Hotwire switched mods is made again
    private var checkedAt: Int? = null

    // so a check that answers after a later one doesn't replace its answer
    private var checks = 0

    /** Checks on first use, and again once Hotwire changed mods since the last check */
    fun ensure() {
        if (checkedAt != HotwireBridge.revision) refresh()
    }

    /** Checks again once the change Hotwire was just given has finished, and has the grid read the changed mods */
    fun refreshWhenDone() {
        HotwireBridge.instance?.awaitIdle()?.whenComplete { _, _ ->
            HotwireBridge.invalidate()
            refresh()
        }
    }

    private fun refresh() {
        val bridge = HotwireBridge.instance ?: return
        checkedAt = HotwireBridge.revision
        val check = ++checks
        bridge.checkUpdates().whenComplete { updates, error ->
            if (check != checks) return@whenComplete
            // a failed check keeps the last list shown, and the next use tries again
            if (error != null) checkedAt = null
            if (error == null || found == null) found = if (error == null) Result.success(updates) else Result.failure(error.cause ?: error)
        }
    }

    /** Update plans waiting to be confirmed, each with the name of what it updates, and what to do with the mods they changed */
    class Review(val plans: List<Pair<String, Result<HotwireBridge.JarPlan>>>, val onDone: (Set<String>) -> Unit)

    var review by mutableStateOf<Review?>(null)
        private set

    /** Whether Hotwire is working out the plans for a [review] */
    var planning by mutableStateOf(false)
        private set

    private var planningJob: Job? = null

    /** The tag of the downloads the planning started */
    var planTag by mutableStateOf("")
        private set

    // the revision the review's plans were made at
    private var reviewedAt = 0

    /** Plans [updates] for a [review], since a plan can change more mods than were picked */
    fun reviewUpdates(updates: List<HotwireBridge.StoreUpdate>, onDone: (Set<String>) -> Unit) {
        val bridge = HotwireBridge.instance ?: return
        val tag = HotwireBridge.newTag()
        planTag = tag
        reviewedAt = HotwireBridge.revision
        planningJob = uiScope.launch {
            planning = true
            try {
                review = Review(bridge.planUpdates(updates, tag), onDone)
            } finally {
                planning = false
            }
        }
    }

    /** Drops a review planned before Hotwire changed mods, whose plans no longer hold */
    fun dropStaleReview() {
        if ((review != null || planning) && reviewedAt != HotwireBridge.revision) dismissReview()
    }

    fun dismissReview() {
        // planning is what downloads the jars, which nothing would use now
        if (planning) HotwireBridge.instance?.stopDownloads(planTag)
        planningJob?.cancel()
        review?.plans?.forEach { it.second.getOrNull()?.onCancel() }
        review = null
    }

    /** Applies the plans in the [review] with whatever Hotwire allows them, live or at the next launch */
    fun acceptReview() {
        val accepted = review ?: return
        val bridge = HotwireBridge.instance ?: return
        review = null
        uiScope.launch {
            val problems = ArrayList<String>()
            val now = ArrayList<String>()
            val nextLaunch = ArrayList<String>()
            try {
                for ((name, plan) in accepted.plans) {
                    updating = name
                    val skipped = HotwireBridge.skipReason(plan)
                    if (skipped != null) {
                        plan.getOrNull()?.onCancel()
                        problems += "$name: $skipped"
                        continue
                    }
                    val result = plan.getOrThrow().actions.first { it.primary }.attempt().await()
                    if (!result.ok) problems += "$name: ${result.message}"
                    now += result.appliedNow
                    nextLaunch += result.atNextStart
                }
            } finally {
                updating = null
                refreshWhenDone()
            }
            val summary = bridge.applied(now, nextLaunch)
            accepted.onDone((now + nextLaunch).toSet())
            if (problems.isEmpty()) {
                Notifications.success("Hotwire", summary.ifBlank { translated("oneconfig.page.hotwire.updates.done", "Updates finished.") })
            } else {
                if (summary.isNotBlank()) Notifications.success("Hotwire", summary)
                Notifications.error("Hotwire", problems.joinToString(" "))
            }
        }
    }
}

/** Whether Hotwire is installed, which is the only time its page is offered */
val isHotwireInstalled: Boolean get() = ModInfo.loadedMods.any { it.id == "hotwire" }

/** Hotwire's mod info, for crediting its authors beside the page title */
fun hotwireInfo(): ModInfo? = ModInfo.loadedMods.firstOrNull { it.id == "hotwire" }

val openHotwireScreen = Runnable { HotwireBridge.instance?.openScreen() }

/** Reports what Hotwire says came of a change */
internal fun notifyHotwireResult(bridge: HotwireBridge, result: HotwireBridge.Result) {
    val message = bridge.describe(result)
    when {
        result.ok -> Notifications.success("Hotwire", message)
        result.needsReopen -> Notifications.info("Hotwire", message)
        else -> {
            Notifications.builder("Hotwire", message).type(NotificationType.ERROR).apply {
                // it stays up so there's time to press the button, which dismisses it
                if (!result.refused) {
                    persistent()
                    action(NotificationAction(translated("oneconfig.page.hotwire.report.copy", "Copy bug report"), true) { copyHotwireReport(message) })
                }
            }.send()
        }
    }
}

/** A file for the clipboard, so a report pastes as an attachment */
private class FileSelection(private val file: Path) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(DataFlavor.javaFileListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.javaFileListFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
        return listOf(file.toFile())
    }
}

/** Has Hotwire write a bug report about [reason] and copies it, or where it is when the clipboard can't hold a file */
private fun copyHotwireReport(reason: String) {
    val report = HotwireBridge.instance?.writeReport(reason) ?: return
    report.whenComplete { file, error ->
        Platform.screen().runOnUiThread {
            when {
                file == null -> Notifications.error(
                    "Hotwire",
                    translated("oneconfig.page.hotwire.report.failed", "The bug report couldn't be written: %s", (error?.cause ?: error)?.message ?: ""),
                )
                ClipboardHelper.setTransferable(FileSelection(file)) ->
                    Notifications.success("Hotwire", translated("oneconfig.page.hotwire.report.copied", "Bug report copied. Paste it wherever you're asking for help."))
                ClipboardHelper.setString(file.toString()) ->
                    Notifications.info("Hotwire", translated("oneconfig.page.hotwire.report.path_copied", "Bug report written, and its location copied."))
                else -> Notifications.info("Hotwire", translated("oneconfig.page.hotwire.report.written", "Your bug report is at %s.", file))
            }
        }
    }
}

/** Icon files per mod id and version, blank for none, so each jar is read once and each icon is one file wherever it shows */
private val iconPaths = ConcurrentHashMap<Pair<String, String>, String>()

/** The file holding a mod's icon, or null when its jar has none; reads the jar the first time, so not on the UI thread */
internal fun modIconPath(bridge: HotwireBridge, id: String, version: String): String? =
    iconPaths.getOrPut(id to version) { runCatching { bridge.icon(id) }.getOrNull().orEmpty() }.ifEmpty { null }

@Composable
internal fun ModIcon(bridge: HotwireBridge, id: String, version: String, size: Int) {
    val key = id to version
    val path by produceState(iconPaths[key]?.ifEmpty { null }, key) {
        value = withContext(Dispatchers.IO) { modIconPath(bridge, id, version) }
    }
    // keeps names lined up while an icon loads or when a mod has none
    Box(Modifier.size(size.dp)) {
        path?.let { Icon(it, color = LocalTheme.current.textColor, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun HotwireUnavailable(text: String = translated("oneconfig.page.hotwire.unavailable", "Hotwire is not available.")) {
    Box(Modifier.fillMaxSize().padding(horizontal = PageGutter), contentAlignment = Alignment.Center) {
        EmptyState(text, icon = "lightning-01")
    }
}

/** Every mod Hotwire can switch, in a denser grid than the mods page, where picked mods are turned on or off live */
@Composable
fun Hotwire() {
    val bridge = HotwireBridge.instance ?: return HotwireUnavailable()

    var prompt by remember { mutableStateOf(bridge.openingPrompt()) }
    prompt?.let {
        OpeningPromptCard(it) { prompt = null }
        return
    }

    val revision = HotwireBridge.revision
    val states = remember(revision) { bridge.mods() }
    if (states.isEmpty()) return HotwireUnavailable(translated("oneconfig.page.hotwire.switch.starting", "Hotwire is still starting, so mods can't be switched yet."))
    LaunchedEffect(revision) {
        HotwireUpdates.ensure()
        HotwireUpdates.dropStaleReview()
    }
    // a plan only holds for now, so it doesn't wait for the page to be opened again
    DisposableEffect(Unit) { onDispose { HotwireUpdates.dismissReview() } }
    val availableUpdates = HotwireUpdates.modIds
    var onlyUpdates by remember { mutableStateOf(false) }
    val query = pageSearchQuery().trim()
    val mods = remember(states, query, onlyUpdates, availableUpdates) {
        states
            .filter { it.switchable }
            .filter { (query.isEmpty() || it.name.contains(query, true) || it.id.contains(query, true)) && (!onlyUpdates || it.id in availableUpdates) }
            .sortedBy { it.name.lowercase() }
    }
    val selected = remember { mutableStateSetOf<String>() }
    val gridState = rememberRestorableLazyGridState("hotwire")
    val navigation = LocalShell.current.navigation
    val theme = LocalTheme.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = PageGutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(translated("oneconfig.page.hotwire.filter.updates", "Has update"), onlyUpdates, { onlyUpdates = !onlyUpdates }, icon = "refresh")
                Spacer(Modifier.weight(1f))
                IconSegments(
                    listOf(
                        IconSegment(false, "grid", translated("oneconfig.page.hotwire.layout.grid", "Grid")),
                        IconSegment(true, "list", translated("oneconfig.page.hotwire.layout.list", "List")),
                    ),
                    listLayout,
                ) { listLayout = it }
            }
            // Hotwire's off and a mod card's switch look alike but do different things
            Text(
                translated(
                    "oneconfig.page.hotwire.hint",
                    "Mods turned off here are unloaded for every profile. The switch on a mod's card only pauses it for the current profile.",
                ),
                Modifier.padding(horizontal = PageGutter),
                theme.textColorSecondary,
                fontSize = 12.sp,
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (mods.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(horizontal = PageGutter), contentAlignment = Alignment.Center) {
                        // with only updates shown, an empty grid can also mean the updates aren't known yet
                        val checked = HotwireUpdates.found
                        when {
                            !onlyUpdates || checked?.isSuccess == true -> EmptyState(translated("oneconfig.page.hotwire.no_match", "Nothing matches."), icon = "search")
                            checked == null -> EmptyState(translated("oneconfig.page.hotwire.updates.checking", "Checking for updates…"), state = LoadState.Loading)
                            else -> EmptyState(
                                translated("oneconfig.page.hotwire.updates.failed", "Couldn't check for updates: %s", checked.exceptionOrNull()?.message ?: ""),
                                icon = "alert-circle",
                            )
                        }
                    }
                }
                ScrollContainer(gridState, Modifier.fillMaxSize(), gutter = PageGutter) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = if (listLayout) GridCells.Fixed(1) else GridCells.Adaptive(140.dp),
                        verticalArrangement = Arrangement.spacedBy(if (listLayout) 8.dp else 19.dp),
                        horizontalArrangement = Arrangement.spacedBy(19.dp),
                        // the last row can still scroll clear of the bar drawn over the grid
                        contentPadding = PaddingValues(start = PageGutter, end = PageGutter, bottom = SwitchBarHeight + 38.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(mods, key = { it.id }) { mod ->
                            HotwireTile(bridge, mod, mod.id in availableUpdates, listLayout, mod.id in selected) {
                                if (!selected.remove(mod.id)) selected.add(mod.id)
                            }
                        }
                    }
                }
                Row(
                    // a fixed height the switch bar fills, so the button stays centred on it and doesn't move when it appears
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = PageGutter, end = PageGutter, bottom = 19.dp).height(SwitchBarHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    val picked = states.filter { it.id in selected }
                    if (picked.isNotEmpty()) SwitchBar(bridge, picked, Modifier.weight(1f), onDeselect = { selected.removeAll(it) })
                    TextButton(translated("oneconfig.page.hotwire.add_mods", "Add mods"), "plus", { navigation.navigate(HotwireAddRoute) }, style = ControlStyle.Primary)
                }
            }
        }
        HotwireUpdates.review?.let { UpdateReviewDialog(it) }
    }
}

private val SwitchBarHeight = 56.dp

/** Everything the planned updates would change, which needs confirming as it can go beyond the mods that were picked */
@Composable
private fun UpdateReviewDialog(review: HotwireUpdates.Review) {
    val theme = LocalTheme.current
    ModalDialog(
        onDismiss = HotwireUpdates::dismissReview,
        title = { DialogTitle(translated("oneconfig.page.hotwire.updates.review", "Review these updates")) },
        buttons = {
            TextButton(translated("oneconfig.page.hotwire.add.back", "Back"), HotwireUpdates::dismissReview)
            if (review.plans.any { (_, plan) -> HotwireBridge.skipReason(plan) == null }) {
                TextButton(translated("oneconfig.page.hotwire.modrinth.update", "Update"), HotwireUpdates::acceptReview, style = ControlStyle.Primary)
            }
        },
    ) {
        review.plans.forEach { (name, plan) ->
            Text(name, color = theme.textColorSecondary, fontSize = 13.sp)
            val lines = plan.getOrNull()?.lines.orEmpty()
            // a refusal comes as the plan's own lines, so only a reason of another kind is added to them
            val reason = HotwireBridge.skipReason(plan)?.takeIf { it != lines.joinToString(" ") }
            (lines + listOfNotNull(reason)).forEach {
                Text("• $it", color = theme.textColor, fontSize = 13.sp)
            }
        }
    }
}

/** Hotwire's warning, which it requires be shown before it takes requests, with its reading delay */
@Composable
internal fun OpeningPromptCard(prompt: HotwireBridge.OpeningPrompt, onDone: () -> Unit) {
    val theme = LocalTheme.current
    var wait by remember { mutableIntStateOf(prompt.waitSeconds) }
    LaunchedEffect(Unit) {
        while (wait > 0) {
            delay(1000)
            wait--
        }
    }
    Box(Modifier.fillMaxSize().padding(horizontal = PageGutter), contentAlignment = Alignment.Center) {
        Card(Modifier.widthIn(max = 560.dp), contentPadding = PaddingValues(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(prompt.title, color = theme.textColor, fontSize = 18.sp, textAlign = TextAlign.Center)
                prompt.lines.forEach { Text(it, color = theme.textColor, fontSize = 13.sp, textAlign = TextAlign.Center) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        if (wait > 0) "${prompt.acceptText} ($wait)" else prompt.acceptText,
                        {
                            prompt.accept()
                            onDone()
                        },
                        style = ControlStyle.Primary,
                        enabled = wait == 0,
                    )
                    TextButton(
                        prompt.openHotwireText,
                        {
                            prompt.acceptAndOpenHotwire()
                            onDone()
                        },
                        enabled = wait == 0,
                    )
                }
            }
        }
    }
}

/** How far the downloads of a plan are, or null [progress] while none is; [fraction] is what a bar should show */
internal class DownloadWatch {
    var progress by mutableStateOf<HotwireBridge.DownloadProgress?>(null)
    var fraction by mutableFloatStateOf(0f)
}

/** Follows the downloads of [tag] for as long as [active] */
@Composable
internal fun rememberDownloadWatch(bridge: HotwireBridge, tag: String, active: Boolean = true): DownloadWatch {
    val watch = remember { DownloadWatch() }
    LaunchedEffect(active, tag) {
        watch.progress = null
        watch.fraction = 0f
        while (active) {
            val latest = bridge.downloadProgress(tag)
            watch.fraction = latest?.shownAfter(watch.progress, watch.fraction) ?: 0f
            watch.progress = latest
            delay(DOWNLOAD_POLL_MILLIS)
        }
    }
    return watch
}

internal const val DOWNLOAD_POLL_MILLIS = 100L

/** The picked mods and what switching them does, with the button that does it, as Hotwire's own screen offers */
@Composable
private fun SwitchBar(bridge: HotwireBridge, picked: List<HotwireBridge.ModState>, modifier: Modifier = Modifier, onDeselect: (Set<String>) -> Unit) {
    val theme = LocalTheme.current
    val turnOff = remember(picked) { picked.filter { it.on }.map { it.id } }
    val turnOn = remember(picked) { picked.filterNot { it.on }.map { it.id } }
    val mixed = turnOff.isNotEmpty() && turnOn.isNotEmpty()
    // a plan rather than a switch, as only a plan says what goes along and can save and reopen the world around it;
    // remembered with what it was made for, so one for another selection or from before Hotwire changed mods is never offered
    val planState = remember(turnOff, turnOn, HotwireBridge.revision) { mutableStateOf<Result<HotwireBridge.Plan>?>(null) }
    LaunchedEffect(planState) {
        planState.value = HotwireBridge.catching { bridge.plan(HotwireBridge.Change(off = turnOff, on = turnOn)).await() }
    }
    val planned = planState.value
    val plan = planned?.getOrNull()
    val pickedIds = picked.mapTo(HashSet()) { it.id }
    val updates = HotwireUpdates.found?.getOrNull().orEmpty().filter { it.modId in pickedIds }
    val updating = HotwireUpdates.updating
    var switching by remember { mutableStateOf(false) }
    val planning = HotwireUpdates.planning
    val running = switching || updating != null || planning
    var fraction by remember { mutableFloatStateOf(0f) }
    // also what isn't OneConfig's, as a change made elsewhere can end without telling anyone
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos {
                val progress = bridge.progress()
                fraction = progress ?: 0f
                busy = progress != null
            }
        }
    }
    // planning is what downloads the jars, so that's what there is to show until a plan is ready
    val download = rememberDownloadWatch(bridge, HotwireUpdates.planTag, planning)

    val tooMany = picked.size > MaxSwitchedMods
    val label = when {
        plan?.needsReopen == true -> translated("oneconfig.page.hotwire.switch.reopen", "Save and switch %s", picked.size)
        mixed -> translated("oneconfig.page.hotwire.switch.mixed", "Switch %s", picked.size)
        turnOff.isNotEmpty() -> translated("oneconfig.page.hotwire.switch.off", "Turn off %s", turnOff.size)
        else -> translated("oneconfig.page.hotwire.switch.on", "Turn on %s", turnOn.size)
    }
    val message = when {
        updating != null -> translated("oneconfig.page.hotwire.updates.running", "Updating %s…", updating)
        // named, since Hotwire fetches one jar at a time and the bar starts over for each
        planning -> download.progress?.let { translated("oneconfig.page.hotwire.updates.downloading", "Downloading %s… %s%%", it.name, (download.fraction * 100).toInt()) }
            ?: translated("oneconfig.page.hotwire.switch.checking", "Checking what this changes…")
        running -> translated("oneconfig.page.hotwire.switch.running", "Switching… %s%%", (fraction * 100).toInt())
        tooMany -> translated("oneconfig.page.hotwire.switch.too_many", "Only %s mods can be switched at once.", MaxSwitchedMods)
        planned == null -> translated("oneconfig.page.hotwire.switch.checking", "Checking what this changes…")
        else -> planned.fold({ it.lines.joinToString(" ") }) { (it.cause ?: it).message.orEmpty() }
    }
    val canSwitch = !running && !tooMany && plan?.canApply == true && !busy

    fun start(plan: HotwireBridge.Plan) {
        // a second click before the bar redraws mustn't switch again
        if (switching) return
        switching = true
        // mods picked while this runs stay picked
        val started = pickedIds.toSet()
        // Hotwire answers for these before it has added them back, so the grid has to look again once it's done
        val bringsBack = picked.any { !it.on && it.offSinceStart }
        plan.accept().whenComplete { result, error ->
            Platform.screen().runOnUiThread {
                switching = false
                notifyHotwireResult(bridge, result ?: HotwireBridge.Result(false, (error?.cause ?: error)?.message ?: ""))
                // a change that failed partway leaves picked only what it didn't switch
                if (result?.ok == true) onDeselect(started) else result?.let { onDeselect(it.appliedNow.toSet()) }
                if (bringsBack) HotwireUpdates.refreshWhenDone()
            }
        }
    }

    Row(
        modifier = modifier
            // opaque, since the card background would let the tiles underneath show through the text
            .background(theme.popupBackground, theme.modCardShape)
            .fillMaxHeight()
            // it sits over the grid, so a click on the bar mustn't pick the tile underneath
            .pointerInput(Unit) {}
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(translated("oneconfig.page.hotwire.switch.selected", "%s selected", picked.size), color = theme.textColor, fontSize = 14.sp)
            if (message.isNotBlank()) {
                // the whole of a long plan shows on hovering it
                TooltipBox(message) {
                    // focusable, so the whole plan can be read from a keyboard or controller too
                    Text(message, Modifier.focusable(), theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (running) ProgressBar(if (planning) download.fraction else fraction, Modifier.fillMaxWidth())
        }
        TextButton(translated("oneconfig.page.hotwire.switch.clear", "Clear"), {
            // the bar goes with the selection, so nothing would show the plan still coming
            if (planning) HotwireUpdates.dismissReview()
            onDeselect(pickedIds)
        })
        if (!running && updates.isNotEmpty()) {
            TextButton(translated("oneconfig.page.hotwire.updates.bar", "Update %s", updates.size), { HotwireUpdates.reviewUpdates(updates, onDeselect) })
        }
        if (canSwitch) TextButton(label, { start(plan) }, style = ControlStyle.Primary)
    }
}

private const val MaxSwitchedMods = 5

@Composable
private fun HotwireTile(
    bridge: HotwireBridge,
    mod: HotwireBridge.ModState,
    hasUpdate: Boolean,
    list: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val theme = LocalTheme.current
    // mods Hotwire has turned off are greyed out but stay selectable so they can be turned back on
    val contentAlpha = if (mod.on) 1f else 0.4f
    val nameColor = if (mod.on) theme.textColor else theme.textColorSecondary
    // the dimming that shows a mod is off is read out too
    val label = if (mod.on) mod.name else translated("oneconfig.page.hotwire.tile.off", "%s, turned off", mod.name)
    Card(Modifier.fillMaxWidth().height(if (list) 56.dp else 124.dp), onClick = onSelect, label = label, selected = selected) {
        if (list) {
            Row(
                modifier = Modifier.fillMaxSize().alpha(contentAlpha).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Checkbox(selected, { onSelect() })
                ModIcon(bridge, mod.id, mod.version, 36)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(mod.name, color = nameColor, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    mod.authors?.takeIf { it.isNotBlank() }?.let {
                        Text(translated("oneconfig.header.byline", "by %s", it), color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (hasUpdate) Text(translated("oneconfig.page.hotwire.update_available", "Update available"), color = Accent, fontSize = 12.sp, maxLines = 1)
                if (mod.version.isNotBlank()) Text(mod.version, color = theme.textColorSecondary, fontSize = 12.sp, maxLines = 1)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().alpha(contentAlpha).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModIcon(bridge, mod.id, mod.version, 56)
                Text(mod.name, Modifier.fillMaxWidth(), nameColor, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Checkbox(selected, { onSelect() })
            }
            if (hasUpdate) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    TooltipBox(translated("oneconfig.page.hotwire.update_available", "Update available")) {
                        Box(Modifier.size(8.dp).background(Accent, theme.circleShape))
                    }
                }
            }
        }
    }
}
