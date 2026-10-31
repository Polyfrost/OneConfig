package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.translated

/**
 * Hotwire's public API read reflectively, since Hotwire is an optional mod OneConfig does not compile against
 *
 * Every call falls back to "Hotwire is not there" when the API is missing or one of its calls throws
 */
object HotwireBridge {
    class ModState(val id: String, val name: String, val version: String, val on: Boolean, val switchable: Boolean)

    class Preview(val possible: Boolean, val message: String)

    /** [needsReopen] means nothing failed but the change only finishes once the world is reopened, which Hotwire's API does not do itself */
    class Result(val ok: Boolean, val message: String, val needsReopen: Boolean = false)

    /** Bumped whenever Hotwire finishes a switch, whoever started it */
    var revision by mutableIntStateOf(0)
        private set

    private val apiClass: Class<*>? by lazy { runCatching { Class.forName("dev.moon.hotwire.api.HotwireApi") }.getOrNull() }

    private val api: Any? by lazy {
        val cls = apiClass ?: return@lazy null
        runCatching {
            cls.getMethod("getInstance").invoke(null).also { api ->
                cls.getMethod("addListener", Consumer::class.java).invoke(api, Consumer<Any?> {
                    Platform.screen().runOnUiThread { revision++ }
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

    /** Whether this Hotwire can hand over the warning the page must show before it switches anything */
    val supportsSwitching: Boolean by lazy { supports("findOpeningPrompt") }

    val supportsOriginalScreen: Boolean by lazy { supports("openScreen") }

    /** Whether this Hotwire has every store call, which arrived over several revisions */
    val supportsStore: Boolean by lazy { (call("getRevision") as? Int ?: 0) >= STORE_REVISION }

    /** Every mod Hotwire knows, or null when Hotwire is missing or has not listed its mods yet */
    fun mods(): List<ModState>? = runCatching {
        (call("getMods") as? List<*>)?.takeIf { it.isNotEmpty() }?.map {
            ModState(
                it!!.get("id") as String,
                it.get("name") as String? ?: "",
                it.get("version") as String? ?: "",
                it.get("on") as Boolean,
                it.get("switchable") as Boolean,
            )
        }
    }.getOrNull()

    val isSwitching: Boolean get() = call("isSwitching") as? Boolean ?: false

    /** How far the running switch is, from 0 to 1 */
    fun progress(): Float = runCatching { (call("getProgress")!!.get("fraction") as Double).toFloat() }.getOrDefault(0f)

    /** What turning [ids] on or off would do, including the mods Hotwire switches along with them */
    fun preview(ids: Collection<String>, on: Boolean): Preview = runCatching {
        val preview = call(if (on) "previewOn" else "previewOff", ids.toList())!!
        Preview(preview.get("possible") as Boolean, preview.get("message") as String? ?: "")
    }.getOrElse { Preview(false, "Hotwire is not available.") }

    /** Whether Hotwire is running a switch, add or update right now */
    fun isWorking(): Boolean = runCatching { call("getProgress")!!.get("running") as Boolean }.getOrDefault(false)

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

    /**
     * What Hotwire would do with a mod, and the choices it offers, as its own add and update panels show
     *
     * [upToDate] means the mod already has this version, so there is nothing to do
     */
    class JarPlan(
        val title: String,
        val detail: String,
        val lines: List<String>,
        val actions: List<JarAction>,
        val onCancel: (() -> Unit)? = null,
        val upToDate: Boolean = false,
    )

    // shortcut: Hotwire's public API can't add a jar from disk or list updates, so those reach into its engine, which
    // may break on a Hotwire update, until Hotwire's API gains them
    // the engine lives in a package per game version, the same one as the API implementation
    private fun engine(): Any? = runCatching {
        Class.forName("${api!!.javaClass.packageName}.HotwireClient").getMethod("findEngine").invoke(null)
    }.getOrNull()

    private fun Any.callMethod(name: String, vararg args: Any): Any? =
        javaClass.methods.first { it.name == name && it.parameterCount == args.size }.invoke(this, *args)

    /** The constant named [constant] of the enum a two-argument [method] of the engine takes last */
    private fun Any.enumArg(method: String, constant: String): Any =
        javaClass.methods.first { it.name == method && it.parameterCount == 2 }.parameterTypes[1]
            .enumConstants.first { (it as Enum<*>).name == constant }

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
        val prompt = call("findOpeningPrompt") ?: return null
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
        internal val offer: Any,
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
            Files.write(file, uri.toURL().openStream().use { it.readNBytes(MAX_ICON_BYTES) })
        }
        return file
    }

    private fun storeQuery(words: String): Any =
        Class.forName("dev.moon.hotwire.api.StoreQuery").getMethod("of", String::class.java).invoke(null, words)

    class StoreCategory(val name: String, val parts: List<String>)

    /** Modrinth's mod categories as Hotwire groups them, where [StoreCategory.parts] are what a search can filter by */
    @Suppress("UNCHECKED_CAST")
    fun storeCategories(): List<StoreCategory> = runCatching {
        val mod = Class.forName("dev.moon.hotwire.api.StoreKind").enumConstants.first { (it as Enum<*>).name == "MOD" }
        (call("getStoreCategories", mod) as List<*>).map { StoreCategory(it!!.get("name") as String, it.get("parts") as List<String>) }
    }.getOrDefault(emptyList())

    private var storeChecked = false

    /** Hotwire's offers from the last update check, which it reuses when fetching what an update needs */
    @Volatile
    private var offers: List<*> = emptyList<Any>()

    /**
     * Searches Modrinth for mods matching [words] in [category], or every category when blank, skipping the first
     * [offset] results; blocks on the network, so call it off the UI thread
     */
    fun searchStore(words: String, category: String, offset: Int): StorePage {
        val query = storeQuery(words)
            .let { it.javaClass.getMethod("withCategory", String::class.java).invoke(it, category) }
            .let { it.javaClass.getMethod("withOffset", Int::class.java).invoke(it, offset) }
        fun search(): StorePage {
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
        val first = search()
        if (storeChecked) return first
        storeChecked = true
        // the first search creates Hotwire's store, whose installed check is still queued on its single worker, so
        // this runs once that has finished and only then are the installed flags right
        val model = engine()?.callMethod("getStore")?.callMethod("getModel") ?: return first
        val checked = CompletableFuture<Unit>()
        model.callMethod("run", Runnable { checked.complete(Unit) })
        checked.get(60, TimeUnit.SECONDS)
        return search()
    }

    /** The newest version of a project that fits this game; blocks on the network */
    private fun newestVersion(projectId: String): String {
        val project = (call("findStoreProject", storeQuery(""), projectId) as CompletableFuture<*>).get() as Optional<*>
        val version = (project.orElse(null)?.get("versions") as List<*>?)?.firstOrNull()
            ?: error(translated("oneconfig.page.hotwire.modrinth.no_version", "No version of this fits this game."))
        return version.get("id") as String
    }

    /** What adding [hit] would do, including what Hotwire adds along with it; blocks on the network */
    fun storePlan(hit: StoreHit): JarPlan {
        val version = newestVersion(hit.projectId)
        val add = (call("requestAdd", hit.projectId, version) as CompletableFuture<*>).get()!!
        var accepted = false
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), false) {
            accepted = true
            add.through("StoreAdd", "cancel")
            call("queueForNextStart", hit.projectId, version).asResults()
        }
        val save = JarAction(translated("oneconfig.page.hotwire.add.save", "Save to Downloads"), false) {
            accepted = true
            add.through("StoreAdd", "cancel")
            call("saveToDownloads", storeQuery(""), hit.projectId, version).asResults()
        }
        val now = JarAction(
            if (add.through("StoreAdd", "needsReopen") == true) {
                translated("oneconfig.page.hotwire.add.reopen", "Save and add")
            } else {
                translated("oneconfig.page.hotwire.add.now", "Add now")
            },
            true,
        ) {
            accepted = true
            add.through("StoreAdd", "accept").asResults()
        }
        @Suppress("UNCHECKED_CAST")
        return JarPlan(
            add.through("StoreAdd", "getTitle") as String,
            "",
            add.through("StoreAdd", "getLines") as List<String>,
            (if (add.through("StoreAdd", "canAdd") == true) listOf(now, later) else emptyList()) + save,
            onCancel = { if (!accepted) add.through("StoreAdd", "cancel") },
        )
    }

    /** Hotwire's update check against Modrinth for the installed mods; blocks on the network */
    fun findUpdates(): List<StoreUpdate> {
        val check = engine()!!.callMethod("getUpdateCheck")!!
        val found = check.callMethod("findNow") as List<*>
        offers = found
        return found.map {
            StoreUpdate(it!!, it.get("id") as String, it.get("projectId") as String, it.get("name") as String, it.get("installedVersion") as String, it.get("newVersion") as String)
        }
    }

    /** Downloads the mods an offered update newly needs and then the update's own jar, last; blocks on the network */
    @Suppress("UNCHECKED_CAST")
    fun downloadUpdate(update: StoreUpdate): List<Path> =
        engine()!!.callMethod("getUpdateCheck")!!.callMethod("fetchWithNeeds", update.offer, Consumer<String> {}) as List<Path>

    private fun manifestOf(jar: Path): Any? =
        Class.forName("dev.moon.hotwire.jar.JarLookup").getMethod("findManifest", Path::class.java).invoke(null, jar)

    private fun dryRunAdd(engine: Any, jar: Path): Any = (engine.callMethod("dryRunAdd", jar) as CompletableFuture<*>).get()!!

    private fun Any.lineOfAdd(): String = (get("lines") as List<*>).firstOrNull() as String? ?: get("name") as String

    /** The mods stopping an update, or none when Hotwire can't plan it, which is how it answers any other refusal */
    private fun unmetNeeds(engine: Any, jar: Path): List<String> {
        val changes = engine.javaClass.getDeclaredField("changes").apply { isAccessible = true }.get(engine)!!
        val plan = changes.callMethod("planUpdate", jar) ?: return emptyList()
        return (plan.get("blockers") as List<*>).filter { it!!.javaClass.simpleName == "DependencyUnmet" }.map { it!!.get("mod") as String }
    }

    /**
     * Adds to [steps] what has to land before [jar] can update, each after what it needs in turn, the way Hotwire's
     * own update queue meets needs; false when Hotwire refuses it for anything else
     */
    @Suppress("UNCHECKED_CAST")
    private fun collectNeeds(engine: Any, jar: Path, seen: MutableSet<String>, steps: MutableList<Path>): Boolean {
        val preview = (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<*>).get()!!
        val kind = preview.javaClass.simpleName
        if (kind != "Refused") return kind == "Ready"
        val unmet = unmetNeeds(engine, jar)
        if (unmet.isEmpty()) return false
        val check = engine.callMethod("getUpdateCheck")!!
        for (id in unmet) {
            if (!seen.add(id)) continue
            val jars = check.callMethod("fetchById", id, offers, Consumer<String> {}) as List<Path>
            if (jars.isEmpty()) return false
            for (need in jars) {
                if (need in steps) continue
                if (engine.callMethod("canUpdate", need) == true && !collectNeeds(engine, need, seen, steps)) return false
                steps.add(need)
            }
        }
        return true
    }

    /** What applying [jars] from [downloadUpdate] would do; blocks on Hotwire reading them and on the network */
    @Suppress("UNCHECKED_CAST")
    fun planUpdate(jars: List<Path>): JarPlan {
        val jar = jars.last()
        val plan = planFor(jar, update = true).get()
        val engine = engine()
        if (engine == null || engine.callMethod("canUpdate") != true) return plan
        val steps = jars.dropLast(1).toMutableList()
        val seen = jars.mapNotNull(::manifestOf).flatMapTo(HashSet()) { (it.get("provides") as List<String>) + (it.get("id") as String) }
        if (!collectNeeds(engine, jar, seen, steps) || steps.isEmpty()) return plan
        // a later add can be refused now only for lacking an earlier one, so those are read again as they run
        steps.first().takeIf { engine.callMethod("canUpdate", it) != true }?.let { dryRunAdd(engine, it) }
            ?.takeIf { (it.get("mode") as Enum<*>).name == "REFUSED" }
            ?.let { return JarPlan(plan.title, plan.detail, it.get("lines") as List<String>, emptyList()) }
        val action = JarAction(translated("oneconfig.page.hotwire.update.with_needs", "Update with dependencies"), true) {
            CompletableFuture.supplyAsync { runSteps(engine, steps, jar) }
        }
        val lines = steps.map {
            val name = manifestOf(it)?.get("name") as String? ?: it.fileName.toString()
            if (engine.callMethod("canUpdate", it) == true) {
                translated("oneconfig.page.hotwire.update.updates", "Updates %s, which this update needs.", name)
            } else {
                translated("oneconfig.page.hotwire.update.adds", "Adds %s, which this update needs.", name)
            }
        }
        return JarPlan(plan.title, plan.detail, lines + plan.lines.takeIf { plan.actions.isNotEmpty() }.orEmpty(), listOf(action))
    }

    /** Lands [steps] in order and then updates [jar], stopping at the first that fails, since each one's plan rests on those before */
    private fun runSteps(engine: Any, steps: List<Path>, jar: Path): Result {
        for (step in steps) {
            val result = if (engine.callMethod("canUpdate", step) == true) updateNow(step) else addNow(engine, step)
            if (!result.ok) return result
        }
        return updateNow(jar)
    }

    private fun <T> onUiThread(block: () -> T): T {
        val done = CompletableFuture<T>()
        Platform.screen().runOnUiThread { runCatching(block).fold(done::complete, done::completeExceptionally) }
        return done.get()
    }

    private fun updateNow(jar: Path): Result {
        val plan = planFor(jar, update = true).get()
        if (plan.upToDate) return Result(true, "")
        val action = plan.actions.firstOrNull { it.primary } ?: return Result(false, plan.lines.firstOrNull().orEmpty())
        return onUiThread(action::attempt)!!.get()
    }

    private fun addNow(engine: Any, jar: Path): Result {
        val preview = dryRunAdd(engine, jar)
        val mode = preview.get("mode") as Enum<*>
        if (mode.name == "REFUSED") return Result(false, preview.lineOfAdd())
        onUiThread { engine.callMethod("add", jar, mode) }
        dryRunAdd(engine, jar)
        if (mode.name == "NEXT_START" || engine.callMethod("canUpdate", jar) == true) return Result(true, "")
        return Result(false, translated("oneconfig.page.hotwire.add.failed", "Hotwire couldn't add %s. It posts why in chat.", preview.get("name") as String))
    }

    /**
     * How the update just handed to Hotwire ended, which its engine doesn't report: a dry run asked for now runs on
     * Hotwire's single worker after that update, and then sees the new version in, queued, or neither
     */
    @Suppress("UNCHECKED_CAST")
    private fun updateResult(engine: Any, jar: Path, name: String, version: String): CompletableFuture<Result> =
        (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<Any>).thenApply { after ->
            val queued = (engine.callMethod("getPendingChanges") as List<*>).any {
                it!!.javaClass.simpleName == "Update" && it.get("name") == name && it.get("toVersion") == version
            }
            when {
                after.javaClass.simpleName == "SameVersion" -> Result(true, translated("oneconfig.page.hotwire.update.done", "%s is updated.", name))
                queued -> Result(true, translated("oneconfig.page.hotwire.update.queued", "%s updates at the next launch.", name))
                after.javaClass.simpleName == "Refused" -> Result(false, after.get("reason") as String)
                else -> Result(false, translated("oneconfig.page.hotwire.update.failed", "Hotwire couldn't update %s. It posts why in chat.", name))
            }
        }

    /**
     * Reads [jar] the way Hotwire does before adding or updating
     *
     * An [update] is read as one straight away; otherwise it is one when Hotwire says that mod is already installed
     */
    fun planFor(jar: Path, update: Boolean = false): CompletableFuture<JarPlan> = runCatching {
        val engine = engine()!!
        // with no class access Hotwire's update dry run has nothing to run on, so say so instead of failing inside it
        if (update && engine.callMethod("canUpdate") != true) {
            return CompletableFuture.completedFuture(
                JarPlan(
                    jar.fileName.toString(),
                    "",
                    listOf(translated("oneconfig.page.hotwire.update.unavailable", "Hotwire can't update mods in this game session.")),
                    emptyList(),
                ),
            )
        }
        @Suppress("UNCHECKED_CAST")
        if (update || engine.callMethod("canUpdate", jar) == true) {
            (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<Any>).thenApply { updatePlan(engine, jar, it) }
        } else {
            (engine.callMethod("dryRunAdd", jar) as CompletableFuture<Any>).thenApply { addPlan(engine, jar, it) }
        }
    }.getOrElse { CompletableFuture.completedFuture(JarPlan(jar.fileName.toString(), "", listOf("Hotwire is not available."), emptyList())) }

    @Suppress("UNCHECKED_CAST")
    private fun addPlan(engine: Any, jar: Path, preview: Any): JarPlan {
        fun add(mode: String): () -> CompletableFuture<Result>? = {
            engine.callMethod("add", jar, engine.enumArg("add", mode))
            null
        }
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), false, add("NEXT_START"))
        val actions = when ((preview.get("mode") as Enum<*>).name) {
            "NOW" -> listOf(JarAction(translated("oneconfig.page.hotwire.add.now", "Add now"), true, add("NOW")), later)
            "SAVE_AND_REOPEN" -> listOf(JarAction(translated("oneconfig.page.hotwire.add.reopen", "Save and add"), true, add("SAVE_AND_REOPEN")), later)
            "NEXT_START" -> listOf(JarAction(later.label, true, add("NEXT_START")))
            else -> emptyList()
        }
        return JarPlan(preview.get("name") as String, preview.get("version") as String? ?: "", preview.get("lines") as List<String>, actions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun updatePlan(engine: Any, jar: Path, preview: Any): JarPlan {
        fun update(method: String): () -> CompletableFuture<Result>? = {
            engine.callMethod("update", jar, engine.enumArg("update", method))
            updateResult(engine, jar, preview.get("name") as String, preview.get("newVersion") as String)
        }
        return when (preview.javaClass.simpleName) {
            "Ready" -> {
                val method = (preview.get("method") as Enum<*>).name
                val inPlace = method == "IN_PLACE"
                val lines = preview.get("lines") as List<String>
                JarPlan(
                    preview.get("name") as String,
                    "${preview.get("installedVersion")} → ${preview.get("newVersion")}",
                    if (inPlace) {
                        lines
                    } else {
                        listOf(translated("oneconfig.page.hotwire.update.restart_only", "Hotwire can't swap this mod while the game runs, so it updates at the next launch.")) + lines
                    },
                    listOf(
                        JarAction(
                            if (inPlace) {
                                translated("oneconfig.page.hotwire.update.now", "Update now")
                            } else {
                                translated("oneconfig.page.hotwire.update.next_start", "Update at next launch")
                            },
                            true,
                            update(method),
                        ),
                    ),
                )
            }
            "Refused" -> JarPlan(preview.get("name") as String, "", listOf(preview.get("reason") as String), emptyList())
            "SameVersion" -> JarPlan(
                preview.get("name") as String,
                preview.get("version") as String,
                listOf(translated("oneconfig.page.hotwire.update.same", "This version is already installed.")),
                emptyList(),
                upToDate = true,
            )
            else -> JarPlan(jar.fileName.toString(), "", listOf(translated("oneconfig.page.hotwire.unreadable", "Hotwire can't read this jar.")), emptyList())
        }
    }

    /** Turns [ids] on or off, completing on whatever thread Hotwire finishes on */
    fun switch(ids: Collection<String>, on: Boolean): CompletableFuture<Result> = runCatching {
        call(if (on) "turnOn" else "turnOff", ids.toList()).asResults()
    }.getOrElse { CompletableFuture.completedFuture(Result(false, "Hotwire is not available.")) }

    private const val MAX_ICON_BYTES = 8 * 1024 * 1024

    private const val STORE_REVISION = 5
}
