package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.nio.file.Files
import java.nio.file.Path
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

    class JarAction(val label: String, val primary: Boolean, val run: () -> Unit)

    /** What Hotwire would do with a picked jar, and the choices it offers, as its own add and update panels show */
    class JarPlan(val title: String, val detail: String, val lines: List<String>, val actions: List<JarAction>)

    // shortcut: Hotwire's public API has no add or update calls, so the jar flow reaches into its engine and may
    // break on a Hotwire update, until Hotwire's API gains them
    private fun engine(): Any? = runCatching {
        Class.forName("dev.moon.hotwire.fabric.HotwireClient").getMethod("findEngine").invoke(null)
    }.getOrNull()

    private fun Any.callMethod(name: String, vararg args: Any): Any? =
        javaClass.methods.first { it.name == name && it.parameterCount == args.size }.invoke(this, *args)

    /** The constant named [constant] of the enum a two-argument [method] of the engine takes last */
    private fun Any.enumArg(method: String, constant: String): Any =
        javaClass.methods.first { it.name == method && it.parameterCount == 2 }.parameterTypes[1]
            .enumConstants.first { (it as Enum<*>).name == constant }

    class StoreHit(
        val projectId: String,
        val slug: String,
        val title: String,
        val author: String,
        val description: String,
        val downloads: Long,
        val iconUrl: String?,
    )

    class StoreUpdate(internal val offer: Any, val modId: String, val name: String, val installedVersion: String, val newVersion: String)

    /** Which Modrinth projects are installed, and which of those have a newer version for this game */
    class InstalledProjects(private val model: Any, val updatable: Set<String>) {
        // the same three checks Hotwire's own store cards make: a jar Modrinth knows by hash, or a loaded mod whose
        // id is the project's slug or id, which catches jars Modrinth doesn't know such as dev builds
        fun isInstalled(hit: StoreHit): Boolean =
            listOf(hit.projectId, hit.slug).any { it.isNotBlank() && model.callMethod("isInstalledProject", it) == true }
    }

    /** Hotwire's Modrinth store, created the way Hotwire's own store screen does when it hasn't been opened yet */
    private fun store(): Any {
        val engine = engine()!!
        engine.callMethod("getStore")?.let { return it }
        val screens = engine.javaClass.getDeclaredField("stores").apply { isAccessible = true }.get(engine)
        return screens.javaClass.getDeclaredMethod("findStore").apply { isAccessible = true }.invoke(screens)!!
    }

    /** Asks Hotwire which Modrinth projects are installed and which have updates; blocks on the network */
    fun installedProjects(): InstalledProjects {
        val store = store()
        val model = store.callMethod("getModel")!!
        store.callMethod("checkInstalled")
        // the store's worker is a single thread, so this runs once the check queued just before it has finished
        val checked = CompletableFuture<Unit>()
        model.callMethod("run", Runnable { checked.complete(Unit) })
        checked.get(60, TimeUnit.SECONDS)
        return InstalledProjects(model, (model.callMethod("getUpdates") as Map<*, *>).keys.mapTo(HashSet()) { it as String })
    }

    /** Downloads a Modrinth icon once and keeps it on disk for later opens; blocks on the network */
    fun iconFor(url: String): Path {
        val dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "oneconfig-hotwire", "icons"))
        // named by the url so a remote path can never pick where the file lands
        val ext = url.substringAfterLast('.').lowercase().takeIf { it.length in 3..4 && it.all(Char::isLetter) } ?: "png"
        val file = dir.resolve("${Integer.toHexString(url.hashCode())}-${url.length}.$ext")
        if (!Files.exists(file)) Files.write(file, modrinth!!.callMethod("fetchFromCdn", url) as ByteArray)
        return file
    }

    /** Hotwire's Modrinth client, already limited to this game version and loader */
    private val modrinth: Any? by lazy {
        runCatching { Class.forName("dev.moon.hotwire.fabric.store.ModStore").getMethod("makeClient").invoke(null) }.getOrNull()
    }

    /** Searches Modrinth for mods matching [query]; blocks on the network, so call it off the UI thread */
    fun searchModrinth(query: String): List<StoreHit> {
        val search = modrinth!!.callMethod("search", query, "", 0, 30)!!
        return (search.get("hits") as List<*>).map {
            StoreHit(
                it!!.get("projectId") as String,
                it.get("slug") as String? ?: "",
                it.get("title") as String,
                it.get("author") as String? ?: "",
                it.get("description") as String? ?: "",
                it.get("downloads") as Long,
                (it.get("iconUrl") as String?)?.takeIf { url -> url.isNotBlank() },
            )
        }
    }

    /**
     * Downloads the newest version of [projectId] that fits this game and returns the checked jar, ready for
     * [planFor]; blocks on the network
     */
    fun downloadLatest(projectId: String): Path {
        val client = modrinth!!
        val version = (client.callMethod("findVersions", projectId) as List<*>).firstOrNull()
            ?: error("No version of this mod fits this game.")
        val bytes = client.callMethod("fetch", version) as ByteArray
        // the download is untrusted until it matches the hash Modrinth published for it
        check(client.javaClass.getMethod("hashOf", ByteArray::class.java).invoke(null, bytes) == version.get("sha512")) {
            "The download didn't match Modrinth's checksum."
        }
        val dir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "oneconfig-hotwire"))
        return Files.write(dir.resolve(Path.of(version.get("fileName") as String).fileName), bytes)
    }

    /** Hotwire's update check against Modrinth for the installed mods; blocks on the network */
    fun findUpdates(): List<StoreUpdate> {
        val check = engine()!!.callMethod("getUpdateCheck")!!
        return (check.callMethod("findNow") as List<*>).map {
            StoreUpdate(it!!, it.get("id") as String, it.get("name") as String, it.get("installedVersion") as String, it.get("newVersion") as String)
        }
    }

    /** Downloads an offered update the way Hotwire does, returning the jar for [planFor]; blocks on the network */
    fun downloadUpdate(update: StoreUpdate): Path =
        engine()!!.callMethod("getUpdateCheck")!!.callMethod("fetch", update.offer) as Path

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
        fun add(mode: String) = { engine.callMethod("add", jar, engine.enumArg("add", mode)); Unit }
        val later = JarAction(translated("oneconfig.page.hotwire.add.next_start", "Add at next launch"), false, add("NEXT_START"))
        val actions = when ((preview.get("mode") as Enum<*>).name) {
            "NOW" -> listOf(JarAction(translated("oneconfig.page.hotwire.add.now", "Add now"), true, add("NOW")), later)
            "SAVE_AND_REOPEN" -> listOf(JarAction(translated("oneconfig.page.hotwire.add.reopen", "Save and add"), true, add("SAVE_AND_REOPEN")), later)
            "NEXT_START" -> listOf(JarAction(later.label, true, later.run))
            else -> emptyList()
        }
        return JarPlan(preview.get("name") as String, preview.get("version") as String? ?: "", preview.get("lines") as List<String>, actions)
    }

    @Suppress("UNCHECKED_CAST")
    private fun updatePlan(engine: Any, jar: Path, preview: Any): JarPlan {
        fun update(method: String) = { engine.callMethod("update", jar, engine.enumArg("update", method)); Unit }
        val later = JarAction(translated("oneconfig.page.hotwire.update.next_start", "Update at next launch"), false, update("NEXT_START"))
        return when (preview.javaClass.simpleName) {
            "Ready" -> {
                // Hotwire's own panel only offers a live swap when its preview allows one; the lines say what blocks it
                val inPlace = (preview.get("method") as Enum<*>).name == "IN_PLACE"
                val lines = preview.get("lines") as List<String>
                JarPlan(
                    preview.get("name") as String,
                    "${preview.get("installedVersion")} → ${preview.get("newVersion")}",
                    if (inPlace) {
                        lines
                    } else {
                        listOf(translated("oneconfig.page.hotwire.update.restart_only", "Hotwire can't swap this mod while the game runs, so it updates at the next launch.")) + lines
                    },
                    if (inPlace) {
                        listOf(JarAction(translated("oneconfig.page.hotwire.update.now", "Update now"), true, update("IN_PLACE")), later)
                    } else {
                        listOf(JarAction(later.label, true, later.run))
                    },
                )
            }
            "Refused" -> JarPlan(preview.get("name") as String, "", listOf(preview.get("reason") as String), emptyList())
            "SameVersion" -> JarPlan(
                preview.get("name") as String,
                preview.get("version") as String,
                listOf(translated("oneconfig.page.hotwire.update.same", "This version is already installed.")),
                emptyList(),
            )
            else -> JarPlan(jar.fileName.toString(), "", listOf(translated("oneconfig.page.hotwire.unreadable", "Hotwire can't read this jar.")), emptyList())
        }
    }

    /** Turns [ids] on or off, completing on whatever thread Hotwire finishes on */
    fun switch(ids: Collection<String>, on: Boolean): CompletableFuture<Result> = runCatching {
        @Suppress("UNCHECKED_CAST")
        (call(if (on) "turnOn" else "turnOff", ids.toList()) as CompletableFuture<Any>).thenApply { result ->
            val kind = (result.get("kind") as Enum<*>).name
            Result(kind == "DONE", result.get("message") as String? ?: "", needsReopen = kind == "NEEDS_REOPEN")
        }
    }.getOrElse { CompletableFuture.completedFuture(Result(false, "Hotwire is not available.")) }
}
