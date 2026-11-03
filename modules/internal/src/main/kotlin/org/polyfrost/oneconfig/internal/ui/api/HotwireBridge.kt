package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
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

    /** [needsReopen] means nothing failed but the change only finishes once the world is reopened, which Hotwire's API does not do itself */
    class Result(val ok: Boolean, val message: String, val needsReopen: Boolean = false)

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
        val types = args.map { if (it is Collection<*>) Collection::class.java else it.javaClass }.toTypedArray()
        return runCatching { apiClass!!.getMethod(name, *types).invoke(api, *args) }.getOrNull()
    }

    private fun Any.get(accessor: String): Any? = javaClass.getMethod(accessor).invoke(this)

    private fun supports(call: String) = apiClass?.methods?.any { it.name == call } == true

    /**
     * Whether this Hotwire has the change plans every part of the page goes through, and the fields [mods] and
     * [findUpdates] read, which arrived without a new API revision
     */
    val supportsSwitching: Boolean by lazy {
        supports("planChange") && runCatching { Class.forName("dev.moon.hotwire.api.ModState").getMethod("offSinceStart") }.isSuccess
    }

    val supportsOriginalScreen: Boolean by lazy { supports("openScreen") }

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

    val isSwitching: Boolean get() = call("isSwitching") as? Boolean ?: false

    /** How far the running switch is, from 0 to 1 */
    fun progress(): Float = runCatching { (call("getProgress")!!.get("fraction") as Double).toFloat() }.getOrDefault(0f)

    /** What turning [ids] on or off would do, including the mods Hotwire switches along with them */
    @Suppress("UNCHECKED_CAST")
    fun preview(ids: Collection<String>, on: Boolean): Preview = runCatching {
        val preview = call(if (on) "previewOn" else "previewOff", ids)!!
        Preview(preview.get("possible") as Boolean, preview.get("message") as String, preview.get("switched") as List<String>)
    }.getOrElse { Preview(false, "Hotwire is not available.") }

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
        Result(kind == "DONE", result.get("message") as String? ?: "", needsReopen = kind == "NEEDS_REOPEN")
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
            Files.write(file, connection.getInputStream().use { it.readNBytes(MAX_ICON_BYTES) })
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
     * [offset] results; blocks on the network, so call it off the UI thread
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
            if (canAdd) listOf(now, later, save) else listOf(save),
            onCancel = { if (!accepted) ask("cancel") },
        )
    }

    /** Hotwire's update check against Modrinth for the installed mods; blocks on the network */
    fun findUpdates(): List<StoreUpdate> {
        val found = (call("checkUpdates") as CompletableFuture<*>).get()!!
        (found.get("failure") as String).takeIf { it.isNotBlank() }?.let { error(it) }
        val queued = pendingTasks().map { it!!.get("modId") }
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

    /** The choices a change plan offers, labelled from the [kind] of change: "add", "update" or "change" */
    @Suppress("UNCHECKED_CAST")
    private fun planFrom(kind: String, title: String, detail: String, plan: Any): JarPlan {
        fun ask(method: String) = plan.through("ChangePlan", method)
        val reopens = ask("needsReopen") == true
        val restartOnly = (ask("getReason") as Enum<*>).name == "NEEDS_RESTART"
        val nextStart = translated("oneconfig.page.hotwire.$kind.next_start", "Apply at next launch")
        val now = JarAction(
            when {
                reopens -> translated("oneconfig.page.hotwire.$kind.reopen", "Save and apply")
                restartOnly -> nextStart
                else -> translated("oneconfig.page.hotwire.$kind.now", "Apply now")
            },
            true,
        ) { ask("accept").asResults() }
        val later = JarAction(nextStart, false) { ask("acceptAtNextStart").asResults() }
        val actions = when {
            ask("canApply") != true -> emptyList()
            !restartOnly && ask("canWaitForNextStart") == true -> listOf(now, later)
            else -> listOf(now)
        }
        return JarPlan(title, detail, ask("getLines") as List<String>, actions, reopens = reopens)
    }

    fun isSelf(update: StoreUpdate) = update.modId == "hotwire" || update.modId.startsWith("hotwire-")

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

    fun switch(ids: Collection<String>, on: Boolean): CompletableFuture<Result> = runCatching {
        call(if (on) "turnOn" else "turnOff", ids.toList()).asResults()
    }.getOrElse { CompletableFuture.completedFuture(Result(false, "Hotwire is not available.")) }

    private const val MAX_ICON_BYTES = 8 * 1024 * 1024

    private const val ICON_TIMEOUT_MS = 10_000

    private const val MODRINTH_LOADER = "fabric"
}
