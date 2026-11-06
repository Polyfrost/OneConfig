package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.translated

/**
 * Hotwire's public API read reflectively, since Hotwire is an optional mod OneConfig does not compile against
 *
 * Every call falls back to "Hotwire is not there" when the API is missing or one of its calls throws
 */
object HotwireBridge {
    class ModState(val id: String, val name: String, val version: String, val on: Boolean, val switchable: Boolean, val offSinceStart: Boolean)

    /** [switched] is every mod the change would move, including the ones Hotwire switches along with those asked for */
    class Preview(val possible: Boolean, val message: String, val switched: List<String> = emptyList())

    /**
     * [needsReopen] means nothing failed but the change only finishes once the world is reopened, which Hotwire's API
     * does not do itself, and [refused] that Hotwire chose not to do it, so nothing went wrong that a report would help with
     */
    class Result(val ok: Boolean, val message: String, val needsReopen: Boolean = false, val refused: Boolean = false)

    /** Bumped whenever Hotwire finishes a switch, whoever started it, or [invalidate] is called */
    var revision by mutableIntStateOf(0)
        private set

    fun invalidate() {
        ModInfo.refreshLoadedMods()
        revision++
    }

    private val apiClass: Class<*>? by lazy { runCatching { Class.forName("dev.moon.hotwire.api.HotwireApi") }.getOrNull() }

    private val api: Any? by lazy {
        val cls = apiClass ?: return@lazy null
        runCatching {
            cls.getMethod("getInstance").invoke(null).also { api ->
                cls.getMethod("addListener", Consumer::class.java).invoke(api, Consumer<Any?> {
                    Platform.screen().runOnUiThread(::invalidate)
                })
            }
        }.getOrNull()
    }

    private fun call(name: String, vararg args: Any): Any? {
        val api = api ?: return null
        val types = args.map {
            when (it) {
                is Collection<*> -> Collection::class.java
                is Int -> Int::class.java
                else -> it.javaClass
            }
        }.toTypedArray()
        return runCatching { apiClass!!.getMethod(name, *types).invoke(api, *args) }.getOrNull()
    }

    private fun Any.get(accessor: String): Any? = javaClass.getMethod(accessor).invoke(this)

    private fun unavailable() = translated("oneconfig.page.hotwire.unavailable", "Hotwire is not available.")

    /** Whether this is Hotwire 0.7.3 or newer, the oldest whose API has everything the page reads */
    val supportsSwitching: Boolean by lazy { (call("getRevision") as? Int ?: 0) >= MIN_REVISION }

    /** Has Hotwire write a bug report zip, with the game log and what it did and personal details taken out */
    @Suppress("UNCHECKED_CAST")
    fun writeReport(reason: String): CompletableFuture<Path> =
        call("writeReport", reason) as CompletableFuture<Path>? ?: CompletableFuture.failedFuture(IllegalStateException(unavailable()))

    /** Every mod Hotwire knows, or null when Hotwire is missing or has not listed its mods yet */
    fun mods(): List<ModState>? = runCatching {
        (call("getMods") as? List<*>)?.takeIf { it.isNotEmpty() }?.map {
            ModState(
                it!!.get("id") as String,
                it.get("name") as String? ?: "",
                it.get("version") as String? ?: "",
                it.get("on") as Boolean,
                it.get("switchable") as Boolean,
                it.get("offSinceStart") as Boolean,
            )
        }
    }.getOrNull()

    fun installedState(): List<Any?> = mods().orEmpty().map { listOf(it.id, it.version, it.on) } + pendingTasks()

    private fun pendingTasks(): List<*> = call("getPendingTasks") as? List<*> ?: emptyList<Any>()

    // only these mean an update is taken care of, not a mod turning off at the next launch or a jar copied for one
    private fun queuedUpdates(): List<*> = pendingTasks().filter { (it!!.get("kind") as Enum<*>).name == "UPDATE" }

    /** The name of each mod whose update waits for the next launch, by id */
    fun queuedMods(): Map<String, String> = runCatching {
        queuedUpdates().associate { it!!.get("modId") as String to it.get("name") as String }
    }.getOrDefault(emptyMap())

    class AppliedUpdates(val now: List<String>, val nextLaunch: List<String>)

    /**
     * What a batch updated live and what waits for the next launch: a live update joins the queue too, to swap its jar
     * when the game closes, so only a mod already running its new version counts as live
     */
    fun whenApplied(modsBefore: List<ModState>, modsAfter: List<ModState>, queuedBefore: Map<String, String>, queuedAfter: Map<String, String>): AppliedUpdates {
        val versions = modsBefore.associate { it.id to it.version }
        val idsAfter = modsAfter.mapTo(HashSet()) { it.id }
        // Hotwire can run an update from a copy under a new id, which takes the old row's place under the same name
        // while the jar swap stays queued under the old id, and the mods that copy needs go along without updating
        val replaced = modsBefore.filter { it.id !in idsAfter }.associateBy { it.name }
        val copies = modsAfter.filter { it.id !in versions && replaced[it.name]?.let { old -> old.version != it.version } == true }
        val live = modsAfter.filter { it.id in versions && versions[it.id] != it.version } + copies
        val liveIds = live.map { it.id } + copies.map { replaced.getValue(it.name).id }
        return AppliedUpdates(live.map { it.name }, (queuedAfter - queuedBefore.keys - liveIds.toSet()).values.toList())
    }

    val isSwitching: Boolean get() = call("isSwitching") as? Boolean ?: false

    /** How far the running switch is, from 0 to 1 */
    fun progress(): Float = runCatching { (call("getProgress")!!.get("fraction") as Double).toFloat() }.getOrDefault(0f)

    /** What turning [ids] on or off would do, including the mods Hotwire switches along with them */
    @Suppress("UNCHECKED_CAST")
    fun preview(ids: Collection<String>, on: Boolean): Preview = runCatching {
        val preview = call(if (on) "previewOn" else "previewOff", ids)!!
        Preview(preview.get("possible") as Boolean, preview.get("message") as String, preview.get("switched") as List<String>)
    }.getOrElse { Preview(false, unavailable()) }

    /** What turning [off] off and [on] on together would do; blocks on Hotwire's plan, so call it off the UI thread */
    @Suppress("UNCHECKED_CAST")
    fun previewTogether(off: List<String>, on: List<String>): Preview? {
        if (off.isEmpty() || on.isEmpty()) return null
        return runCatching {
            val plan = changePlan(off = off, on = on)
            Preview(plan.through("ChangePlan", "canApply") == true, (plan.through("ChangePlan", "getLines") as List<String>).joinToString(" "))
        }.getOrElse {
            Preview(false, translated("oneconfig.page.hotwire.switch.conflict", "Some of these mods need others you're turning off. Switch them separately."))
        }
    }

    /**
     * Hotwire's running downloads, leaving out [others]: Hotwire lists every download it has, its own update among
     * them, so whatever was already running before a plan began isn't that plan's to show or stop
     */
    private fun runningDownloads(others: Set<Int> = emptySet()): List<Any> = runCatching {
        (call("getStoreDownloads") as List<*>).filterNotNull().filter { it.get("over") == false && it.get("id") !in others }
    }.getOrDefault(emptyList())

    fun runningDownloadIds(): Set<Int> = runCatching { runningDownloads().mapTo(HashSet()) { it.get("id") as Int } }.getOrDefault(emptySet())

    /** [fraction] goes from 0 to 1, [paused] says every running download is paused, and [name] is the one being fetched */
    class DownloadProgress(val fraction: Float, val paused: Boolean, val name: String, private val ids: Set<Int>) {
        /** What a bar showing [shown] for [before] moves to: a finished jar leaving must not pull it back, but a new jar starts it over */
        fun shownAfter(before: DownloadProgress?, shown: Float): Float =
            if (before != null && before.ids.containsAll(ids)) maxOf(fraction, shown) else fraction
    }

    /** How far Hotwire's running downloads but [others] are together, or null while none is running */
    @JvmOverloads
    fun downloadProgress(others: Set<Int> = emptySet()): DownloadProgress? = runCatching {
        val running = runningDownloads(others).ifEmpty { return null }
        fun sum(field: String) = running.sumOf { it.get(field) as Long }
        DownloadProgress(
            (sum("done").toFloat() / sum("total").coerceAtLeast(1)).coerceAtMost(1f),
            running.all { (it.get("state") as Enum<*>).name == "PAUSED" },
            (running.firstOrNull { (it.get("state") as Enum<*>).name == "GETTING" } ?: running.first()).get("name") as String,
            running.mapTo(HashSet()) { it.get("id") as Int },
        )
    }.getOrNull()

    // shortcut: Hotwire doesn't say who started a download, so a plan begun beside [others] can't pause or stop anything,
    // as stopping the next jar of one of those would delete its partial file, and a plan begun alone also controls
    // whatever started after it; narrow this if its API ever names an owner
    private fun controlDownloads(action: String, others: Set<Int>) {
        if (others.isNotEmpty()) return
        runCatching { runningDownloads().forEach { call(action, it.get("id") as Int) } }
    }

    @JvmOverloads
    fun pauseDownloads(paused: Boolean, others: Set<Int> = emptySet()) = controlDownloads(if (paused) "pauseDownload" else "resumeDownload", others)

    /** Ends the running downloads unless [others] were running too, and Hotwire then answers whatever was waiting on them with nothing to add */
    @JvmOverloads
    fun stopDownloads(others: Set<Int> = emptySet()) = controlDownloads("stopDownload", others)

    /** Whether Hotwire is running a switch or add right now */
    fun isWorking(): Boolean = runCatching { call("getProgress")!!.get("running") as Boolean }.getOrDefault(false)

    fun isStarting(ids: Collection<String>): Boolean = isWorking() && ids.any { call("isAddedThisSession", it) != true }

    fun isUpdating(): Boolean = runCatching { (call("getStatus")!!.get("state") as Enum<*>).name == "BUSY" }.getOrDefault(false)

    fun awaitIdle() {
        val start = System.nanoTime()
        var sawWorking = false
        while (true) {
            Thread.sleep(100)
            val working = isWorking()
            if (working) sawWorking = true
            if (sawWorking && !working || !sawWorking && System.nanoTime() - start > 3_000_000_000L) return
        }
    }

    /** [run] hands back the outcome when Hotwire reports one, and null when Hotwire only posts it to chat */
    class JarAction(val label: String, val primary: Boolean, private val run: () -> CompletableFuture<Result>?) {
        /** Runs the action from a click, where a failure inside Hotwire comes back as a failed result, not a throw */
        fun attempt(): CompletableFuture<Result>? =
            runCatching(run).getOrElse { CompletableFuture.completedFuture(failure(it)) }?.exceptionally(::failure)
    }

    private fun failure(error: Throwable): Result {
        val reason = error.cause ?: error
        return Result(false, reason.message ?: reason.toString())
    }

    /** What Hotwire would do with a mod, and the choices it offers, as its own add and update panels show */
    class JarPlan(
        val title: String,
        val detail: String,
        val lines: List<String>,
        val actions: List<JarAction>,
        val onCancel: (() -> Unit)? = null,
        val reopens: Boolean = false,
    )

    // Hotwire implements its API interfaces with private classes, which reflection can only reach through the interface
    private fun Any.through(apiInterface: String, method: String): Any? =
        Class.forName("dev.moon.hotwire.api.$apiInterface").getMethod(method).invoke(this)

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asResults(): CompletableFuture<Result> = (this as CompletableFuture<Any>).thenApply { result ->
        val kind = (result.get("kind") as Enum<*>).name
        Result(kind == "DONE", result.get("message") as String? ?: "", needsReopen = kind == "NEEDS_REOPEN", refused = kind == "REFUSED")
    }

    class OpeningPrompt(
        val title: String,
        val lines: List<String>,
        val acceptText: String,
        val openHotwireText: String,
        val waitSeconds: Int,
        val accept: () -> Unit,
        val acceptAndOpenHotwire: () -> Unit,
    )

    /** Hotwire's warning, which refuses this mod's requests until it has been shown; null once it's been accepted */
    fun openingPrompt(): OpeningPrompt? = runCatching {
        val prompt = call("findOpeningPrompt") ?: run {
            // accepted in an earlier session, which Hotwire only counts as shown for this mod once it's asked to show it
            call("showPrompt")
            return null
        }
        fun text(method: String) = prompt.through("OpeningPrompt", method) as String
        fun act(method: String): () -> Unit = {
            prompt.through("OpeningPrompt", method)
            revision++
        }
        @Suppress("UNCHECKED_CAST")
        OpeningPrompt(
            text("getTitle"),
            prompt.through("OpeningPrompt", "getLines") as List<String>,
            text("getAcceptText"),
            text("getOpenHotwireText"),
            prompt.through("OpeningPrompt", "getWaitSeconds") as Int,
            act("accept"),
            act("acceptAndOpenHotwire"),
        )
    }.getOrNull()

    /** Opens the folder Hotwire saves downloads to in the system's file manager */
    fun openDownloads() {
        call("openDownloads")
    }

    /** Opens Hotwire's own screen */
    fun openScreen() {
        call("openScreen")
    }

    class StoreHit(
        val projectId: String,
        val slug: String,
        val title: String,
        val author: String,
        val description: String,
        val downloads: Long,
        val iconUrl: String?,
        val installed: Boolean,
    )

    class StorePage(val hits: List<StoreHit>, val total: Int)

    /**
     * Hotwire only learns of a project from a jar's hash when its store first opens, so it misses mods added since then
     * and any jar Modrinth didn't serve; a project named after one of the [mods] present counts as installed too, though
     * only Hotwire's own answer should hide a project, since a different project can share a mod's name
     */
    fun isInstalled(hit: StoreHit, mods: Collection<ModInfo>): Boolean =
        hit.installed || mods.any { it.id == hit.slug || it.name.equals(hit.title, ignoreCase = true) }

    class StoreUpdate(
        val modId: String,
        val projectId: String,
        val name: String,
        val installedVersion: String,
        val newVersion: String,
    )

    /** Downloads a Modrinth icon once and keeps it on disk for later opens; blocks on the network */
    fun iconFor(url: String): Path {
        val dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "oneconfig-hotwire", "icons"))
        // named by the url so a remote path can never pick where the file lands
        val ext = url.substringAfterLast('.').lowercase().takeIf { it.length in 3..4 && it.all(Char::isLetter) } ?: "png"
        val file = dir.resolve("${Integer.toHexString(url.hashCode())}-${url.length}.$ext")
        if (!Files.exists(file)) {
            val uri = URI(url)
            require(uri.scheme == "https") { "Icons only load over https." }
            val connection = uri.toURL().openConnection().apply {
                connectTimeout = ICON_TIMEOUT_MS
                readTimeout = ICON_TIMEOUT_MS
            }
            val bytes = connection.getInputStream().use { it.readNBytes(MAX_ICON_BYTES) }
            // moved into place whole, so an icon fetched twice at once is never read half-written
            val temp = Files.createTempFile(dir, file.fileName.toString(), ".tmp")
            try {
                Files.write(temp, bytes)
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally {
                Files.deleteIfExists(temp)
            }
        }
        return file
    }

    private fun storeQuery(words: String): Any =
        Class.forName("dev.moon.hotwire.api.StoreQuery").getMethod("of", String::class.java).invoke(null, words)

    private fun directDownloadQuery(): Any = storeQuery("").let {
        it.javaClass.getMethod("withBrowsing", String::class.java, String::class.java).invoke(it, "", MODRINTH_LOADER)
    }

    class StoreCategory(val name: String, val parts: List<String>)

    /** Modrinth's mod categories as Hotwire groups them, where [StoreCategory.parts] are what a search can filter by */
    @Suppress("UNCHECKED_CAST")
    fun storeCategories(): List<StoreCategory> = runCatching {
        val mod = Class.forName("dev.moon.hotwire.api.StoreKind").enumConstants.first { (it as Enum<*>).name == "MOD" }
        (call("getStoreCategories", mod) as List<*>).map { StoreCategory(it!!.get("name") as String, it.get("parts") as List<String>) }
    }.getOrDefault(emptyList())

    /**
     * Searches Modrinth for mods matching [words] in [category], or every category when blank, skipping the first
     * [offset] results; blocks on the network and on Hotwire's check of what's installed, so call it off the UI thread
     */
    fun searchStore(words: String, category: String, offset: Int): StorePage {
        val query = storeQuery(words)
            .let { it.javaClass.getMethod("withCategory", String::class.java).invoke(it, category) }
            .let { it.javaClass.getMethod("withOffset", Int::class.java).invoke(it, offset) }
        val page = (call("searchStore", query) as CompletableFuture<*>).get()!!
        val hits = (page.get("mods") as List<*>).map {
            StoreHit(
                it!!.get("id") as String,
                it.get("slug") as String? ?: "",
                it.get("name") as String,
                it.get("author") as String? ?: "",
                it.get("description") as String? ?: "",
                it.get("downloads") as Long,
                (it.get("iconUrl") as String?)?.takeIf { url -> url.isNotBlank() },
                it.get("installed") as Boolean,
            )
        }
        return StorePage(hits, page.get("total") as Int)
    }

    /** The newest version of a project that fits this game; blocks on the network */
    private fun newestVersion(projectId: String): String {
        val project = (call("findStoreProject", storeQuery(""), projectId) as CompletableFuture<*>).get() as Optional<*>
        val version = (project.orElse(null)?.get("versions") as List<*>?)?.firstOrNull()
            ?: error(translated("oneconfig.page.hotwire.modrinth.no_version", "No version of this fits this game."))
        return version.get("id") as String
    }

    /** What adding [hit] would do, including what Hotwire adds along with it; blocks on the network */
    @Suppress("UNCHECKED_CAST")
    fun storePlan(hit: StoreHit): JarPlan {
        val version = newestVersion(hit.projectId)
        val add = (call("requestAdd", hit.projectId, version) as CompletableFuture<*>).get()!!
        fun ask(method: String) = add.through("StoreAdd", method)
        val canAdd = ask("canAdd") == true
        var accepted = false
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), false) {
            accepted = true
            ask("cancel")
            call("queueForNextStart", hit.projectId, version).asResults()
        }
        val save = JarAction(translated("oneconfig.page.hotwire.add.save", "Save to Downloads"), false) {
            accepted = true
            ask("cancel")
            val query = if (canAdd) storeQuery("") else directDownloadQuery()
            call("saveToDownloads", query, hit.slug.ifBlank { hit.projectId }, version).asResults()
        }
        val now = JarAction(
            if (ask("needsReopen") == true) {
                translated("oneconfig.page.hotwire.add.reopen", "Save and add")
            } else {
                translated("oneconfig.page.hotwire.add.now", "Add now")
            },
            true,
        ) {
            accepted = true
            ask("accept").asResults()
        }
        return JarPlan(
            ask("getTitle") as String,
            "",
            ask("getLines") as List<String>,
            // queueing skips what made Hotwire refuse, so an installed mod would start next to its own older jar
            if (canAdd) listOf(now, later, save) else listOf(save),
            onCancel = { if (!accepted) ask("cancel") },
        )
    }

    /** Hotwire's update check against Modrinth for the installed mods; blocks on the network */
    fun findUpdates(): List<StoreUpdate> {
        val found = (call("checkUpdates") as CompletableFuture<*>).get()!!
        (found.get("failure") as String).takeIf { it.isNotBlank() }?.let { error(it) }
        val queued = queuedUpdates().map { it!!.get("modId") }
        return (found.get("updates") as List<*>).filterNot { it!!.get("id") in queued }.map {
            StoreUpdate(it!!.get("id") as String, it.get("projectId") as String, it.get("name") as String, it.get("installedVersion") as String, it.get("newVersion") as String)
        }
    }

    private fun changePlan(
        off: List<String> = emptyList(),
        on: List<String> = emptyList(),
        update: List<String> = emptyList(),
        jars: List<Path> = emptyList(),
    ): Any {
        val request = Class.forName("dev.moon.hotwire.api.ChangeRequest").constructors.single().newInstance(off, on, update, emptyList<String>(), jars)
        return (call("planChange", request) as CompletableFuture<*>).get()!!
    }

    /**
     * The choices a change plan offers, labelled from the [kind] of change: "add", "update" or "change"
     *
     * The main one does what each line says, live where it can and at the next launch where it can't, so a plan that
     * needs a restart for part of it isn't labelled as all live or all waiting
     */
    @Suppress("UNCHECKED_CAST")
    private fun planFrom(kind: String, title: String, detail: String, plan: Any): JarPlan {
        fun ask(method: String) = plan.through("ChangePlan", method)
        val reopens = ask("needsReopen") == true
        val restartOnly = (ask("getReason") as Enum<*>).name == "NEEDS_RESTART"
        val now = JarAction(
            when {
                reopens -> translated("oneconfig.page.hotwire.$kind.reopen", "Save and apply")
                restartOnly -> translated("oneconfig.page.hotwire.$kind.as_planned", "Apply")
                else -> translated("oneconfig.page.hotwire.$kind.now", "Apply now")
            },
            true,
        ) {
            saveBeforeGoing()
            ask("accept").asResults()
        }
        val later = JarAction(translated("oneconfig.page.hotwire.$kind.next_start", "Apply at next launch"), false) { ask("acceptAtNextStart").asResults() }
        val actions = when {
            ask("canApply") != true -> emptyList()
            ask("canWaitForNextStart") == true -> listOf(now, later)
            else -> listOf(now)
        }
        return JarPlan(title, detail, ask("getLines") as List<String>, actions, reopens = reopens)
    }

    fun isSelf(update: StoreUpdate) = update.modId == "hotwire"

    /** What applying [updates] together would do, with what they newly need; blocks on the network */
    fun planUpdate(updates: List<StoreUpdate>): JarPlan {
        val only = updates.singleOrNull()
        val detail = only?.let { "${it.installedVersion} → ${it.newVersion}" }.orEmpty()
        if (only != null && isSelf(only)) {
            return JarPlan(
                only.name,
                detail,
                listOf(translated("oneconfig.page.hotwire.update.self", "Hotwire can't update itself while the game runs, so it updates at the next launch.")),
                listOf(
                    JarAction(translated("oneconfig.page.hotwire.update.next_start", "Update at next launch"), true) {
                        call("updateHotwire")
                        null
                    },
                ),
            )
        }
        return planFrom("update", updates.joinToString(", ") { it.name }, detail, changePlan(update = updates.map { it.modId }))
    }

    /** What Hotwire would do with [jar], adding it or updating the mod it already is; blocks on Hotwire reading it */
    fun planFor(jar: Path): JarPlan = planFrom("change", jar.fileName.toString(), "", changePlan(jars = listOf(jar)))

    // a mod's own save only runs while it's on, and a mod that stays off until the game closes would lose this change
    private fun saveBeforeGoing() {
        runCatching { ConfigManager.active().saveAll() }
    }

    fun switch(ids: Collection<String>, on: Boolean): CompletableFuture<Result> = runCatching {
        if (!on) saveBeforeGoing()
        call(if (on) "turnOn" else "turnOff", ids.toList()).asResults()
    }.getOrElse { CompletableFuture.completedFuture(Result(false, unavailable())) }

    private const val MAX_ICON_BYTES = 8 * 1024 * 1024

    private const val ICON_TIMEOUT_MS = 10_000

    private const val MODRINTH_LOADER = "fabric"

    private const val MIN_REVISION = 8
}
