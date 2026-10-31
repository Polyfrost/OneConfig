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
import java.util.function.Function
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
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

    fun installedState(): List<Any?> = mods().orEmpty().map { listOf(it.id, it.version, it.on) } +
        runCatching { engine()!!.callMethod("getPendingChanges") as List<*> }.getOrDefault(emptyList())

    val isSwitching: Boolean get() = call("isSwitching") as? Boolean ?: false

    /** How far the running switch is, from 0 to 1 */
    fun progress(): Float = runCatching { (call("getProgress")!!.get("fraction") as Double).toFloat() }.getOrDefault(0f)

    /** What turning [ids] on or off would do, including the mods Hotwire switches along with them */
    fun preview(ids: Collection<String>, on: Boolean): Preview = runCatching {
        val kept = if (on) keptOff(ids) else emptyList()
        val preview = (ids - kept.toSet()).takeIf { it.isNotEmpty() }?.let { call(if (on) "previewOn" else "previewOff", it)!! }
        val addsBack = kept.takeIf { it.isNotEmpty() }?.let { mods ->
            translated("oneconfig.page.hotwire.switch.adds_back", "Adds back %s.", mods.joinToString(", ") { engine()!!.callMethod("nameOfMod", it) as String })
        }
        Preview(preview?.get("possible") as Boolean? ?: true, listOfNotNull(preview?.get("message") as String?, addsBack).joinToString(" "))
    }.getOrElse { Preview(false, "Hotwire is not available.") }

    @Suppress("UNCHECKED_CAST")
    fun previewTogether(off: List<String>, on: List<String>): Preview? {
        if (off.isEmpty() || on.isEmpty()) return null
        val conflict = translated("oneconfig.page.hotwire.switch.conflict", "Some of these mods need others you're turning off. Switch them separately.")
        val plan = runCatching {
            val engine = engine()!!
            val leaving = call("previewOff", off)!!.get("switched") as List<String>
            val disabled = engine.field("installed")!!.field("disabled") as Map<String, Any>
            val planner = plannerWith(engine, keptOff(on).map(disabled::getValue))
            planner.callMethod("planOn", on, (offMods() + leaving).toSet())!!.takeUnless { plan ->
                (plan.get("order") as List<String>).any { it in leaving }
            }
        }.getOrNull() ?: return Preview(false, conflict)
        val engine = engine()!!
        if ((plan.get("refusals") as List<*>).isNotEmpty()) {
            return Preview(false, runCatching { describeRefusal(engine, plan) }.getOrDefault(conflict))
        }
        refusalOnceMet(engine, keptOff(on).mapNotNull { engine.callMethod("findJarOf", it) as Path? })?.let { return Preview(false, it) }
        val alone = preview(on, true)
        if (alone.possible) return alone
        val names = (plan.get("order") as List<String>).joinToString(", ") { engine.callMethod("nameOfMod", it) as String }
        return Preview(true, translated("oneconfig.page.hotwire.switch.after_off", "Turns on %s once the others are off.", names))
    }

    fun keptOnWhenOff(ids: Collection<String>): Set<String> = runCatching {
        val plan = plannerWith(engine()!!, emptyList()).callMethod("planOff", ids, offMods().toSet())!!
        (plan.get("enabledNested") as List<*>).mapTo(HashSet()) { it!!.get("mod") as String }
    }.getOrDefault(emptySet())

    private fun describeRefusal(engine: Any, plan: Any): String {
        val refused = Class.forName("dev.moon.hotwire.switching.ModSwitcher\$Result\$Refused").constructors.single().newInstance(plan)
        val nameOf = Function<String, String> { engine.callMethod("nameOfMod", it) as String }
        return Class.forName("dev.moon.hotwire.switching.PlayerMessages").methods
            .first { it.name == "describe" && it.parameterCount == 2 }.invoke(null, refused, nameOf) as String
    }

    private fun offMods(): List<String> = mods().orEmpty().filterNot { it.on }.map { it.id }

    private fun plannerWith(engine: Any, jars: List<Any>): Any =
        engine.callStatic("ModPolicy", "makePlannerFor", engine.field("installed")!!.callMethod("makeGraphWith", jars)!!)!!

    private fun keptOff(ids: Collection<String>): List<String> = runCatching {
        val disabled = engine()!!.callHidden("getDisabled")!!
        ids.filter { disabled.callHidden("isDisabledSinceStart", it) == true }
    }.getOrDefault(emptyList())

    /** Whether Hotwire is running a switch, add or update right now */
    fun isWorking(): Boolean = runCatching { call("getProgress")!!.get("running") as Boolean }.getOrDefault(false)

    fun isStarting(ids: Collection<String>): Boolean = isWorking() && runCatching {
        val installed = engine()!!.field("installed")!!
        ids.any { installed.callMethod("isAddedThisSession", it) != true }
    }.getOrDefault(true)

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
        val reopens: Boolean = false,
    )

    // shortcut: Hotwire's public API can't add a jar from disk or list updates, so those reach into its engine, which
    // may break on a Hotwire update, until Hotwire's API gains them
    // the engine lives in a package per game version, the same one as the API implementation
    private fun engine(): Any? = runCatching {
        Class.forName("${api!!.javaClass.packageName}.HotwireClient").getMethod("findEngine").invoke(null)
    }.getOrNull()

    private fun Any.field(name: String): Any? = javaClass.getDeclaredField(name).apply { isAccessible = true }.get(this)

    private fun Any.callMethod(name: String, vararg args: Any): Any? =
        javaClass.methods.first { it.name == name && it.parameterCount == args.size }.invoke(this, *args)

    private fun Any.callHidden(name: String, vararg args: Any): Any? =
        javaClass.declaredMethods.first { it.name == name && it.parameterCount == args.size }.apply { isAccessible = true }.invoke(this, *args)

    private fun Any.callStatic(type: String, name: String, vararg args: Any): Any? =
        Class.forName("${javaClass.packageName}.$type").methods.first { it.name == name && it.parameterCount == args.size }.invoke(null, *args)

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

    private var storeChecked: CompletableFuture<Unit>? = null

    private var storeCheckedState: List<Any?>? = null

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
        installedCheck()?.get()
        return search()
    }

    @Synchronized
    private fun installedCheck(): CompletableFuture<Unit>? {
        val state = installedState()
        if (state == storeCheckedState) return storeChecked
        val engine = engine() ?: return null
        val made = engine.callMethod("getStore")
        val store = made ?: runCatching { onUiThread { engine.callHidden("findStore") } }.getOrNull() ?: return null
        if (made != null) store.callMethod("checkInstalled")
        storeCheckedState = state
        return storeWorkerDone(store.callMethod("getModel")!!)
            .completeOnTimeout(Unit, STORE_WORKER_TIMEOUT_SECONDS, TimeUnit.SECONDS).also { storeChecked = it }
    }

    private fun storeWorkerDone(model: Any): CompletableFuture<Unit> {
        val done = CompletableFuture<Unit>()
        model.callMethod("run", Runnable { done.complete(Unit) })
        return done
    }

    /** The newest version of a project that fits this game; blocks on the network */
    private fun newestVersion(projectId: String): String {
        val project = (call("findStoreProject", storeQuery(""), projectId) as CompletableFuture<*>).get() as Optional<*>
        val version = (project.orElse(null)?.get("versions") as List<*>?)?.firstOrNull()
            ?: error(translated("oneconfig.page.hotwire.modrinth.no_version", "No version of this fits this game."))
        return version.get("id") as String
    }

    private fun browseThisGame() {
        val model = engine()?.callMethod("getStore")?.callMethod("getModel") ?: return
        val client = model.callMethod("getClient")!!
        if (client.callMethod("isBrowsingOwn") == true) return
        model.callMethod("setBrowsing", client.callMethod("getLoader")!!, client.callMethod("getGameVersion")!!)
    }

    /** What adding [hit] would do, including what Hotwire adds along with it; blocks on the network */
    fun storePlan(hit: StoreHit): JarPlan {
        val version = newestVersion(hit.projectId)
        runCatching { onUiThread(::browseThisGame) }
        val add = (call("requestAdd", hit.projectId, version) as CompletableFuture<*>).get()!!
        val canAdd = add.through("StoreAdd", "canAdd") == true
        @Suppress("UNCHECKED_CAST")
        val jars = add.field("jars") as List<Path>
        val fitting = engine()?.takeIf { !canAdd && fitTogether(it, jars) }
        val refusal = fitting?.let { refusalOnceMet(it, jars) }
        val inOrder = fitting.takeIf { refusal == null }
        val nextLaunch = engine()?.takeIf { jars.isNotEmpty() && (canAdd || inOrder != null) }?.let { nextLaunchRefusal(it, jars) }
        val reopen = add.through("StoreAdd", "needsReopen") == true || inOrder != null && jars.any { reopensOnceMet(inOrder, it) }
        val modes =(add.field("previews") as List<*>).map { it!!.addMode() }
        val onlyLater = modes.lastOrNull() == "NEXT_START" || inOrder != null && "NEXT_START" in modes
        var accepted = false
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), onlyLater) {
            accepted = true
            add.through("StoreAdd", "cancel")
            CompletableFuture.supplyAsync { addInOrder(engine()!!, jars, hit.title, reopen = false, later = true) }
        }
        val save = JarAction(translated("oneconfig.page.hotwire.add.save", "Save to Downloads"), false) {
            accepted = true
            add.through("StoreAdd", "cancel")
            val query = if (jars.isEmpty()) directDownloadQuery() else storeQuery("")
            call("saveToDownloads", query, hit.slug.ifBlank { hit.projectId }, version).asResults()
        }
        val now = JarAction(
            if (reopen) {
                translated("oneconfig.page.hotwire.add.reopen", "Save and add")
            } else {
                translated("oneconfig.page.hotwire.add.now", "Add now")
            },
            true,
        ) {
            accepted = true
            val added = if (inOrder == null) {
                add.through("StoreAdd", "accept").asResults().thenApplyAsync { result ->
                    if (result.ok) lostJar(engine()!!, jars, modes) ?: result else result
                }
            } else {
                add.through("StoreAdd", "cancel")
                CompletableFuture.supplyAsync { addInOrder(inOrder, jars, hit.title, reopen) }
            }
            added.thenApplyAsync { result ->
                runCatching { installedCheck()?.get() }
                if (result.ok) engine()?.callMethod("getStore")?.callMethod("getModel")?.callMethod("markInstalled", hit.projectId)
                result
            }
        }
        @Suppress("UNCHECKED_CAST")
        val lines = if (refusal != null || nextLaunch != null) {
            listOf(nextLaunch ?: refusal!!)
        } else if (inOrder == null) {
            add.through("StoreAdd", "getLines") as List<String>
        } else {
            (add.field("previews") as List<*>).dropLast(1).map {
                translated("oneconfig.page.hotwire.add.adds", "Adds %s, which this mod needs.", it!!.get("name") as String)
            }
        }
        return JarPlan(
            add.through("StoreAdd", "getTitle") as String,
            "",
            lines,
            when {
                nextLaunch != null || !canAdd && inOrder == null -> listOf(save)
                onlyLater -> listOf(later, save)
                else -> listOf(now, later, save)
            },
            onCancel = { if (!accepted) add.through("StoreAdd", "cancel") },
        )
    }

    private fun readJar(jar: Path): Any =
        Class.forName("dev.moon.hotwire.jar.ModJarReader").getMethod("read", Path::class.java).invoke(null, jar)!!

    private fun Any.modId(): String = get("manifest")!!.get("id") as String

    @Suppress("UNCHECKED_CAST")
    private fun fitTogether(engine: Any, jars: List<Path>): Boolean = runCatching {
        if (jars.size < 2 || jars.any { engine.callMethod("canUpdate", it) == true }) return false
        val read = jars.map(::readJar)
        val ids = read.map { it.modId() }
        val planner = plannerWith(engine, read)
        (planner.callMethod("planOn", ids, (offMods() + ids).toSet())!!.get("refusals") as List<*>).isEmpty() &&
            ids.all { (planner.callMethod("findOldDependencies", it) as List<*>).isEmpty() }
    }.getOrDefault(false)

    private fun reopensOnceMet(engine: Any, jar: Path): Boolean = runCatching {
        val adds = engine.callHidden("findParts")!!.field("adds")!!
        if (adds.callMethod("isInWorld") != true) return false
        val scan = Class.forName("dev.moon.hotwire.jar.ContentScan")
        val classes = scan.getMethod("readClasses", Path::class.java).invoke(null, jar)
        val superOf = Function<String, String?> { adds.callMethod("findSuperOf", it) as String? }
        val findKinds = scan.methods.first { it.name == "findKinds" && it.parameterTypes[0].isInstance(classes) }
        (findKinds.invoke(null, classes, superOf) as Set<*>).isNotEmpty()
    }.getOrDefault(false)

    private fun Any.askAdder(type: String, method: String, arg: Any): String? =
        (Class.forName("${javaClass.packageName}.add.$type").declaredMethods.first { it.name == method }
            .apply { isAccessible = true }.invoke(null, arg) as Optional<*>).orElse(null) as String?

    private fun Any.parts(): List<Any> = listOf(this) + (get("nested") as List<*>).flatMap { it!!.parts() }

    private fun worldRefusal(engine: Any): String? =
        (engine.callStatic("add.WorldReopen", "findRefusal") as Optional<*>).orElse(null) as String?

    private fun refusalOnceMet(engine: Any, jars: List<Path>): String? = runCatching {
        jars.firstNotNullOfOrNull { jar ->
            engine.askAdder("AddFirstLook", "findRefusal", jar)
                ?: readJar(jar).parts().firstNotNullOfOrNull { engine.askAdder("AddDependencies", "findMissingRefusal", it) }
        } ?: worldRefusal(engine)?.takeIf { jars.any { reopensOnceMet(engine, it) } }
    }.getOrNull()

    private fun addInOrder(engine: Any, jars: List<Path>, name: String, reopen: Boolean, later: Boolean = false): Result {
        nextLaunchRefusal(engine, jars)?.let { return Result(false, it) }
        var queueing = later
        for (jar in jars) {
            queueing = queueing || dryRunAdd(engine, jar).addMode() == "NEXT_START"
            val result = if (queueing) queueAdd(engine, jar) else addNow(engine, jar, reopen)
            if (!result.ok) return result
        }
        return Result(
            true,
            if (queueing) {
                translated("oneconfig.page.hotwire.add.queued", "%s is added at the next launch.", name)
            } else {
                translated("oneconfig.page.hotwire.add.done", "Added %s.", name)
            },
        )
    }

    /** Hotwire's update check against Modrinth for the installed mods; blocks on the network */
    fun findUpdates(): List<StoreUpdate> {
        val engine = engine()!!
        val found = engine.callMethod("getUpdateCheck")!!.callMethod("findNow") as List<*>
        offers = found
        val queued = (engine.callMethod("getPendingChanges") as List<*>).filter { it!!.javaClass.simpleName == "Update" }.map { it!!.get("modId") }
        return found.filterNot { it!!.get("id") in queued }.map {
            StoreUpdate(it!!, it.get("id") as String, it.get("projectId") as String, it.get("name") as String, it.get("installedVersion") as String, it.get("newVersion") as String)
        }
    }

    /** Downloads the mods an offered update newly needs and then the update's own jar, last; blocks on the network */
    @Suppress("UNCHECKED_CAST")
    private fun downloadUpdate(update: StoreUpdate): List<Path> =
        engine()!!.callMethod("getUpdateCheck")!!.callMethod("fetchWithNeeds", update.offer, Consumer<String> {}) as List<Path>

    fun planUpdate(update: StoreUpdate): JarPlan =
        if (update.modId == "hotwire" || update.modId.startsWith("hotwire-")) planSelfUpdate(update) else planUpdate(downloadUpdate(update))

    private fun planSelfUpdate(update: StoreUpdate): JarPlan {
        val engine = engine()!!
        val jars = downloadUpdate(update)
        val detail = "${update.installedVersion} → ${update.newVersion}"
        nextLaunchRefusal(engine, jars)?.let { return JarPlan(update.name, detail, listOf(it), emptyList()) }
        val action = JarAction(translated("oneconfig.page.hotwire.update.next_start", "Update at next launch"), true) {
            CompletableFuture.supplyAsync {
                nextLaunchRefusal(engine, jars)?.let { Result(false, it) }
                    ?: jars.dropLast(1).firstNotNullOfOrNull { jar -> queueAdd(engine, jar).takeUnless { it.ok } }
                    ?: queueSelfUpdate(engine, update, jars.last()).takeUnless { it.ok }
                    ?: Result(true, translated("oneconfig.page.hotwire.update.queued", "%s updates at the next launch.", update.name))
            }
        }
        return JarPlan(
            update.name,
            detail,
            listOf(translated("oneconfig.page.hotwire.update.self", "Hotwire can't update itself while the game runs, so it updates at the next launch.")),
            listOf(action),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun nextLaunchRefusal(engine: Any, jars: List<Path>): String? = runCatching {
        val incoming = jars.map(::readJar)
        val ids = incoming.map { it.modId() }
        val queued = (engine.field("updateQueue")!!.callMethod("getEntries") as List<Any>).mapNotNull { entry ->
            runCatching { readJar(Path.of(entry.get("stagedJar") as String)) }.getOrNull()?.let { entry.get("modId") as String to it }
        }
        val installed = engine.field("installed")!!
        val leaving = ids + queued.flatMap { (id, jar) -> listOf(id, jar.modId()) } + (installed.field("disabled") as Map<String, Any>).keys
        val staying = (installed.callHidden("getGraphJars") as List<Any>).filterNot { it.modId() in leaving }
        // the jars that land go first, since the graph keeps the first copy of a mod it sees
        val graph = Class.forName("dev.moon.hotwire.plan.ModGraph").getConstructor(Collection::class.java)
            .newInstance(incoming + queued.map { it.second } + staying)
        val depends = Class.forName("dev.moon.hotwire.jar.ModManifest\$Relation").enumConstants.first { (it as Enum<*>).name == "DEPENDS" }
        val oldDependency = Class.forName("dev.moon.hotwire.plan.SwitchPlan\$Refusal\$OldDependency").constructors.single()
        val builtIns = incoming.flatMap { it.parts() }.flatMap { part ->
            oldBuiltIns(
                part.modId(),
                part.get("manifest")!!.callMethod("getRelation", depends) as Map<String, List<String>>,
                { id -> (engine.callStatic("LoadedMods", "findVersion", id) as String?).takeIf { (graph.callMethod("find", id) as Optional<*>).isEmpty } },
                { version, ranges -> engine.callStatic("ModPolicy", "satisfies", version, ranges) == true },
            )
        }.map { oldDependency.newInstance(*it.toTypedArray()) }
        refusedPlan(engine.callStatic("ModPolicy", "makePlannerFor", graph)!!, graph, ids, builtIns)?.let { describeRefusal(engine, it) }
    }.getOrElse {
        translated("oneconfig.page.hotwire.next_launch_unchecked", "Hotwire couldn't check this against your other mods, so nothing was changed.")
    }

    fun oldBuiltIns(mod: String, needs: Map<String, List<String>>, present: (String) -> String?, fits: (String, List<String>) -> Boolean): List<List<String>> =
        needs.mapNotNull { (id, ranges) ->
            present(id)?.takeUnless { fits(it, ranges) }?.let { listOf(mod, id, ranges.joinToString(" or "), it) }
        }

    @Suppress("UNCHECKED_CAST")
    fun refusedPlan(planner: Any, graph: Any, ids: List<String>, builtIns: List<Any> = emptyList()): Any? {
        val plan = planner.callMethod("planOn", ids, ids.toSet())!!
        val old = (ids + ids.flatMap { graph.callMethod("getDependents", it) as Collection<String> }).distinct()
            .flatMap { planner.callMethod("findOldDependencies", it) as List<Any> }
            .filter { it.get("mod") in ids || it.get("dependency") in ids }
        val refusals = plan.get("refusals") as List<Any> + old + builtIns
        if (refusals.isEmpty()) return null
        return plan.javaClass.constructors.single()
            .newInstance(plan.get("direction"), plan.get("order"), plan.get("why"), plan.get("enabledNested"), refusals)
    }

    private fun queueSelfUpdate(engine: Any, update: StoreUpdate, jar: Path): Result {
        val queued = queueAdd(engine, jar)
        if (!queued.ok) return queued
        val queue = engine.field("updateQueue")!!
        return runCatching {
            val running = engine.callMethod("findJarOf", update.modId) as Path
            val hash = Class.forName("dev.moon.hotwire.jar.FileHashes").getMethod("findSha256", Path::class.java).invoke(null, running) as String
            replaceQueued(queue, jar, update.installedVersion, running, hash)
            Result(true, "")
        }.getOrElse {
            removeQueued(queue, jar)
            queueFailed(jar)
        }
    }

    private fun queuedEntry(queue: Any, jar: Path): Any =
        (queue.callMethod("getEntries") as List<*>).first { Path.of(it!!.get("stagedJar") as String).fileName == jar.fileName }!!

    private fun removeQueued(queue: Any, jar: Path) {
        queue.callMethod("remove", queuedEntry(queue, jar).get("modId")!!)
    }

    fun replaceQueued(queue: Any, jar: Path, fromVersion: String, running: Path, runningSha256: String) {
        val entry = queuedEntry(queue, jar)
        queue.callMethod(
            "add",
            entry.javaClass.constructors.single().newInstance(
                entry.get("modId"),
                entry.get("name"),
                fromVersion,
                entry.get("toVersion"),
                running.fileName.toString(),
                runningSha256,
                entry.get("stagedJar"),
                entry.get("stagedSha256"),
                entry.get("reason"),
                entry.get("queuedAtMillis"),
            ),
        )
    }

    private fun manifestOf(jar: Path): Any? =
        Class.forName("dev.moon.hotwire.jar.JarLookup").getMethod("findManifest", Path::class.java).invoke(null, jar)

    private fun nameOfJar(jar: Path): String = manifestOf(jar)?.get("name") as String? ?: jar.fileName.toString()

    private fun dryRunAdd(engine: Any, jar: Path): Any = (engine.callMethod("dryRunAdd", jar) as CompletableFuture<*>).get()!!

    private fun Any.lineOfAdd(): String = (get("lines") as List<*>).firstOrNull() as String? ?: get("name") as String

    private fun Any.addMode(): String = (get("mode") as Enum<*>).name

    /** The mods stopping an update, or none when Hotwire can't plan it, which is how it answers any other refusal */
    private fun unmetNeeds(engine: Any, jar: Path): List<String> {
        val plan = engine.field("changes")!!.callMethod("planUpdate", jar) ?: return emptyList()
        return (plan.get("blockers") as List<*>).filter { it!!.javaClass.simpleName == "DependencyUnmet" }.map { it!!.get("mod") as String }
    }

    /**
     * Adds to [steps] what has to land before [jar] can update, each after what it needs in turn, the way Hotwire's
     * own update queue meets needs; false when Hotwire refuses it for anything else
     */
    private fun collectNeeds(engine: Any, jar: Path, seen: MutableSet<String>, steps: MutableList<Path>): Boolean {
        val preview = (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<*>).get()!!
        val kind = preview.javaClass.simpleName
        if (kind != "Refused") return kind == "Ready"
        val unmet = unmetNeeds(engine, jar)
        return unmet.isNotEmpty() && unmet.all { fetchNeed(engine, it, seen, steps) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchNeed(engine: Any, id: String, seen: MutableSet<String>, steps: MutableList<Path>): Boolean {
        if (!seen.add(id)) return true
        val jars = engine.callMethod("getUpdateCheck")!!.callMethod("fetchById", id, offers, Consumer<String> {}) as List<Path>
        if (jars.isEmpty()) return false
        for (need in jars) {
            if (need in steps) continue
            val met = if (engine.callMethod("canUpdate", need) == true) collectNeeds(engine, need, seen, steps) else meetOldDependencies(engine, need, seen, steps)
            if (!met) return false
            steps.add(need)
        }
        return true
    }

    private fun meetOldDependencies(engine: Any, jar: Path, seen: MutableSet<String>, steps: MutableList<Path>): Boolean {
        val read = readJar(jar)
        val old = plannerWith(engine, listOf(read)).callMethod("findOldDependencies", read.modId()) as List<*>
        return old.all { fetchNeed(engine, it!!.get("dependency") as String, seen, steps) }
    }

    /** What applying [jars] from [downloadUpdate] would do; blocks on Hotwire reading them and on the network */
    @Suppress("UNCHECKED_CAST")
    private fun planUpdate(jars: List<Path>): JarPlan {
        val jar = jars.last()
        val plan = planFor(jar, update = true).get()
        val engine = engine()
        if (engine == null || engine.callMethod("canUpdate") != true) return plan
        val steps = mutableListOf<Path>()
        val seen = jars.mapNotNull(::manifestOf).flatMapTo(HashSet()) { (it.get("provides") as List<String>) + (it.get("id") as String) }
        val needs = jars.dropLast(1)
        val collected = needs.all { need ->
            (engine.callMethod("canUpdate", need) == true || meetOldDependencies(engine, need, seen, steps)).also { if (it) steps.add(need) }
        } && collectNeeds(engine, jar, seen, steps)
        // a later add can be refused now only for lacking an earlier one, so those are read again as they run
        val firstAdds = (if (collected) steps.take(1) else needs).filter { engine.callMethod("canUpdate", it) != true }
            .map { dryRunAdd(engine, it) }
        firstAdds.firstOrNull { it.addMode() == "REFUSED" }
            ?.let { return JarPlan(plan.title, plan.detail, it.get("lines") as List<String>, emptyList()) }
        if (!collected || steps.isEmpty()) return plan
        val adds = steps.filter { engine.callMethod("canUpdate", it) != true }
        (refusalOnceMet(engine, adds) ?: adds.takeIf { it.isNotEmpty() }?.let { nextLaunchRefusal(engine, steps + jar) })
            ?.let { return JarPlan(plan.title, plan.detail, listOf(it), emptyList()) }
        val reopen = firstAdds.any { it.addMode() == "SAVE_AND_REOPEN" } || adds.any { reopensOnceMet(engine, it) }
        val label = if (reopen) {
            translated("oneconfig.page.hotwire.update.reopen", "Save and update")
        } else {
            translated("oneconfig.page.hotwire.update.with_needs", "Update with dependencies")
        }
        val action = JarAction(label, true) {
            CompletableFuture.supplyAsync { runSteps(engine, steps, jar, reopen) }
        }
        val lines = steps.map {
            val name = nameOfJar(it)
            if (engine.callMethod("canUpdate", it) == true) {
                translated("oneconfig.page.hotwire.update.updates", "Updates %s, which this update needs.", name)
            } else {
                translated("oneconfig.page.hotwire.update.adds", "Adds %s, which this update needs.", name)
            }
        }
        return JarPlan(plan.title, plan.detail, lines + plan.lines.takeIf { plan.actions.isNotEmpty() }.orEmpty(), listOf(action), reopens = reopen)
    }

    /** Lands [steps] in order and then updates [jar], stopping at the first that fails, since each one's plan rests on those before */
    private fun runSteps(engine: Any, steps: List<Path>, jar: Path, reopen: Boolean): Result {
        for (step in steps) {
            val result = if (engine.callMethod("canUpdate", step) == true) updateNow(step) else addNow(engine, step, reopen)
            if (!result.ok) return result
        }
        return updateNow(jar)
    }

    @Suppress("UNCHECKED_CAST")
    private fun waitingNeeds(engine: Any, jar: Path): List<String> {
        val manifest = manifestOf(jar) ?: return emptyList()
        val graph = engine.callHidden("getGraph")!!
        val depends = Class.forName("dev.moon.hotwire.jar.ModManifest\$Relation").enumConstants.first { (it as Enum<*>).name == "DEPENDS" }
        val needs = (manifest.callMethod("getRelation", depends) as Map<String, List<String>>).mapKeys { graph.callMethod("resolve", it.key) }
        return (engine.callMethod("getPendingChanges") as List<*>).filter { change ->
            if (change!!.javaClass.simpleName != "Update") return@filter false
            val id = change.get("modId") as String
            val ranges = needs[id] ?: return@filter false
            val running = engine.callStatic("LoadedMods", "findVersion", id)
            running == null || engine.callStatic("ModPolicy", "satisfies", running, ranges) != true
        }.map { it!!.get("name") as String }
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

    private fun reopenNotAgreed(name: String) =
        Result(false, translated("oneconfig.page.hotwire.reopen_confirm", "%s has to save and reopen your world. Try again to confirm.", name), needsReopen = true)

    private fun addNow(engine: Any, jar: Path, reopen: Boolean): Result {
        val preview = dryRunAdd(engine, jar)
        val mode = preview.get("mode") as Enum<*>
        val name = preview.get("name") as String
        when (mode.name) {
            "REFUSED" -> return Result(false, preview.lineOfAdd())
            "NEXT_START" -> return queueAdd(engine, jar)
            "SAVE_AND_REOPEN" -> if (!reopen) return reopenNotAgreed(name)
        }
        onUiThread { engine.callMethod("add", jar, mode) }
        dryRunAdd(engine, jar)
        if (engine.callMethod("canUpdate", jar) != true) {
            return Result(false, translated("oneconfig.page.hotwire.add.failed", "Hotwire couldn't add %s. It posts why in chat.", name))
        }
        return if (onUiThread { keptForNextLaunch(engine, jar) }) Result(true, "") else notKept(jar)
    }

    private fun keptForNextLaunch(engine: Any, jar: Path): Boolean {
        val kept = (engine.field("updateFolders")!!.get("mods") as Path).resolve(jar.fileName.toString())
        return Files.exists(kept) && Files.mismatch(kept, jar) == -1L
    }

    private fun notKept(jar: Path) =
        Result(false, translated("oneconfig.page.hotwire.add.not_kept", "%s is added for now, but it won't stay after a restart - please check chat!", nameOfJar(jar)))

    private fun lostJar(engine: Any, jars: List<Path>, modes: List<String>): Result? {
        val (queued, live) = jars.indices.partition { modes[it] == "NEXT_START" }
        return queued.firstOrNull { !isQueued(engine, jars[it]) }?.let { queueFailed(jars[it]) }
            ?: live.firstOrNull { at -> !onUiThread { keptForNextLaunch(engine, jars[at]) } }?.let { notKept(jars[it]) }
    }

    private fun unwrapped(engine: Any, jar: Path): Path = runCatching {
        engine.callHidden("findParts")!!.field("updates")!!.field("updater")!!.callMethod("unwrap", jar) as Path
    }.getOrDefault(jar)

    private fun isQueued(engine: Any, jar: Path): Boolean {
        val updates = (engine.callMethod("getPendingChanges") as List<*>).filter { it!!.javaClass.simpleName == "Update" }
        return listOf(jar, unwrapped(engine, jar)).any { queued ->
            val manifest = manifestOf(queued) ?: return@any false
            updates.any { it!!.get("newJar") == queued.fileName.toString() && it.get("toVersion") == manifest.get("version") }
        }
    }

    private fun queueFailed(jar: Path) =
        Result(false, translated("oneconfig.page.hotwire.add.queue_failed", "Hotwire couldn't queue %s - please check chat!", nameOfJar(jar)))

    private fun queueAdd(engine: Any, jar: Path): Result {
        onUiThread { engine.callMethod("add", jar, engine.enumArg("add", "NEXT_START")) }
        return if (isQueued(engine, jar)) Result(true, "") else queueFailed(jar)
    }

    /**
     * How the update just handed to Hotwire ended, which its engine doesn't report: a dry run asked for now runs on
     * Hotwire's single worker after that update, and then sees the new version in, queued, or neither
     */
    @Suppress("UNCHECKED_CAST")
    private fun updateResult(engine: Any, jar: Path, name: String): CompletableFuture<Result> =
        (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<Any>).thenApply { after ->
            val queued = isQueued(engine, jar)
            val live = after.javaClass.simpleName == "SameVersion"
            when {
                live && queued -> Result(true, translated("oneconfig.page.hotwire.update.done", "%s is updated.", name))
                live -> Result(false, translated("oneconfig.page.hotwire.update.not_kept", "%s is updated for now, but the update won't stay after a restart - please check chat!", name))
                queued -> Result(true, translated("oneconfig.page.hotwire.update.queued", "%s updates at the next launch.", name))
                after.javaClass.simpleName == "Refused" -> Result(false, after.get("reason") as String)
                else -> Result(false, translated("oneconfig.page.hotwire.update.failed", "Hotwire couldn't update %s. It posts why in chat.", name))
            }
        }

    /**
     * Reads [jar] the way Hotwire does before adding or updating
     *
     * An [update] is read as one straight away; otherwise it is one when Hotwire says that mod is already installed
     * or that this jar replaces an installed mod under a new id
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
            (engine.callMethod("dryRunUpdate", jar) as CompletableFuture<Any>).thenCompose { preview ->
                if (replacesInstalled(preview, jar)) {
                    CompletableFuture.completedFuture(updatePlan(engine, jar, preview))
                } else {
                    (engine.callMethod("dryRunAdd", jar) as CompletableFuture<Any>).thenApply { addPlan(engine, jar, it) }
                }
            }
        }
    }.getOrElse { CompletableFuture.completedFuture(JarPlan(jar.fileName.toString(), "", listOf("Hotwire is not available."), emptyList())) }

    private fun replacesInstalled(preview: Any, jar: Path): Boolean = when (preview.javaClass.simpleName) {
        "Ready" -> true
        "Refused" -> preview.get("name") != jar.fileName.toString()
        else -> false
    }

    @Suppress("UNCHECKED_CAST")
    private fun addPlan(engine: Any, jar: Path, preview: Any): JarPlan {
        fun add(mode: String): () -> CompletableFuture<Result>? = {
            engine.callMethod("add", jar, engine.enumArg("add", mode))
            null
        }
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), false, add("NEXT_START"))
        val mode = (preview.get("mode") as Enum<*>).name
        if (mode != "REFUSED") {
            nextLaunchRefusal(engine, listOf(jar))?.let { return JarPlan(preview.get("name") as String, preview.get("version") as String? ?: "", listOf(it), emptyList()) }
        }
        val actions = when (mode) {
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
            updateResult(engine, jar, preview.get("name") as String)
        }
        return when (preview.javaClass.simpleName) {
            "Ready" -> {
                val method = (preview.get("method") as Enum<*>).name
                val inPlace = method == "IN_PLACE"
                val lines = preview.get("lines") as List<String>
                val waiting = if (inPlace) waitingNeeds(engine, jar) else emptyList()
                if (waiting.isNotEmpty()) {
                    return JarPlan(
                        preview.get("name") as String,
                        "${preview.get("installedVersion")} → ${preview.get("newVersion")}",
                        listOf(translated("oneconfig.page.hotwire.update.waits", "This update needs %s, which only arrives at the next launch. Update this after restarting.", waiting.joinToString(", "))),
                        emptyList(),
                    )
                }
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

    fun reopensToTurnOn(ids: Collection<String>): Boolean = runCatching {
        val engine = engine()!!
        canReopenToAddBack(engine) && worldRefusal(engine) == null && keptOff(ids).any {
            val jar = engine.callMethod("findJarOf", it) as Path
            dryRunAdd(engine, jar).addMode() == "SAVE_AND_REOPEN" || reopensOnceMet(engine, jar)
        }
    }.getOrDefault(false)

    fun switch(ids: Collection<String>, on: Boolean, reopen: Boolean = false): CompletableFuture<Result> = runCatching {
        val kept = if (on) keptOff(ids) else emptyList()
        if (kept.isEmpty()) {
            call(if (on) "turnOn" else "turnOff", ids.toList()).asResults()
        } else {
            CompletableFuture.supplyAsync { turnOnKeptOff(engine()!!, ids, kept, reopen) }
        }
    }.getOrElse { CompletableFuture.completedFuture(Result(false, "Hotwire is not available.")) }

    private fun awaitWorker(engine: Any, jar: Path) {
        runCatching { dryRunAdd(engine, jar) }
    }

    private fun canReopenToAddBack(engine: Any): Boolean =
        engine.javaClass.declaredMethods.any { it.name == "add" && it.parameterCount == 3 }

    private fun addBackWithReopen(engine: Any, id: String, jar: Path): Result {
        val added = CompletableFuture<Result>()
        val mode = engine.enumArg("add", "SAVE_AND_REOPEN")
        onUiThread {
            engine.callHidden("add", jar, mode, Consumer<Any> { outcome ->
                added.complete(
                    runCatching {
                        if (outcome.javaClass.simpleName != "Added") return@runCatching Result(false, outcome.get("reason") as String)
                        val enabled = runCatching {
                            Class.forName("${engine.javaClass.packageName}.DisabledJars")
                                .getDeclaredMethod("enable", Path::class.java).apply { isAccessible = true }.invoke(null, jar)!!
                        }
                        engine.field("installed")!!.callMethod("onBroughtBack", id, enabled.getOrDefault(jar))
                        enabled.fold({ Result(true, "") }) {
                            val name = engine.callMethod("nameOfMod", id) as String
                            Result(false, translated("oneconfig.page.hotwire.switch.rename_failed", "%s is back on, but it will be off at the next launch: %s", name, failure(it).message))
                        }
                    }.getOrElse(::failure),
                )
            })
        }
        return added.get(REOPEN_TIMEOUT_MINUTES, TimeUnit.MINUTES)
    }

    private fun turnOnKeptOff(engine: Any, ids: Collection<String>, kept: List<String>, reopen: Boolean): Result {
        val waiting = kept.associateWithTo(LinkedHashMap()) { engine.callMethod("findJarOf", it) as Path }
        refusalOnceMet(engine, waiting.values.toList())?.let { return Result(false, it) }
        while (waiting.isNotEmpty()) {
            val modes = waiting.entries.map { it to dryRunAdd(engine, it.value).addMode() }
            val (entry, mode) = modes.firstOrNull { it.second == "NOW" }
                ?: modes.firstOrNull { it.second == "SAVE_AND_REOPEN" }
                ?: return Result(false, dryRunAdd(engine, waiting.values.first()).lineOfAdd())
            val (id, jar) = entry
            if (mode == "SAVE_AND_REOPEN") {
                val name = engine.callMethod("nameOfMod", id) as String
                if (!canReopenToAddBack(engine)) {
                    return Result(false, translated("oneconfig.page.hotwire.switch.reopen_unsupported", "Update Hotwire to turn %s back on from here.", name))
                }
                if (!reopen) return reopenNotAgreed(name)
            }
            val result = if (mode == "NOW") call("turnOn", listOf(id)).asResults().get() else addBackWithReopen(engine, id, jar)
            awaitWorker(engine, jar)
            val back = onUiThread {
                invalidate()
                mods().orEmpty().any { it.id == id && it.on }
            }
            if (!result.ok) return result
            if (!back) {
                return Result(false, translated("oneconfig.page.hotwire.switch.add_back_failed", "Hotwire couldn't add back %s - please check chat!", engine.callMethod("nameOfMod", id) as String))
            }
            waiting.remove(id)
        }
        val rest = ids - kept.toSet()
        return if (rest.isEmpty()) Result(true, "") else call("turnOn", rest).asResults().get()
    }

    private const val MAX_ICON_BYTES = 8 * 1024 * 1024

    private const val ICON_TIMEOUT_MS = 10_000

    private const val REOPEN_TIMEOUT_MINUTES = 15L

    private const val STORE_WORKER_TIMEOUT_SECONDS = 60L

    private const val STORE_REVISION = 5

    private const val MODRINTH_LOADER = "fabric"
}
