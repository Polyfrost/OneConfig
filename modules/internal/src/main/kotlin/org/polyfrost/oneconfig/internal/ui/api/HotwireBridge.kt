package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.internal.ui.components.translated
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Result as KotlinResult

/**
 * Hotwire's API as OneConfig's Hotwire pages use it
 *
 * Only `minecraft` compiles against Hotwire's types, so it implements the calls and sets [instance] when Hotwire is
 * installed; everything the pages work out from those calls lives here
 */
@ApiStatus.Internal
abstract class HotwireBridge {
    /** A top-level mod as Hotwire lists it, with [authors] from its metadata and whether it calls itself a [library] */
    class ModState(
        val id: String,
        val name: String,
        val version: String,
        val authors: String?,
        val on: Boolean,
        val switchable: Boolean,
        val offSinceStart: Boolean,
        val library: Boolean = false,
    )

    /**
     * [needsReopen] means nothing failed but the change only finishes once the world is reopened, and [refused] that
     * Hotwire chose not to do it, so nothing went wrong that a report would help with; [appliedNow] and [atNextStart]
     * name the mods a change applied
     */
    class Result(
        val ok: Boolean,
        val message: String,
        val needsReopen: Boolean = false,
        val refused: Boolean = false,
        val appliedNow: List<String> = emptyList(),
        val atNextStart: List<String> = emptyList(),
    )

    /** A change Hotwire planned, which downloads what it needs and changes nothing until accepted */
    interface Plan {
        val lines: List<String>
        val canApply: Boolean

        /** Whether part of it only happens at the next launch */
        val restartOnly: Boolean
        val needsReopen: Boolean
        val canWaitForNextStart: Boolean

        fun accept(): CompletableFuture<Result>

        fun acceptAtNextStart(): CompletableFuture<Result>

        fun cancel()
    }

    class Change(
        val off: List<String> = emptyList(),
        val on: List<String> = emptyList(),
        val update: List<String> = emptyList(),
        val jars: List<Path> = emptyList(),
        val add: String? = null,
        val tag: String = "",
    )

    class Download(val id: Int, val name: String, val done: Long, val total: Long, val over: Boolean, val paused: Boolean, val getting: Boolean)

    class OpeningPrompt(
        val title: String,
        val lines: List<String>,
        val acceptText: String,
        val openHotwireText: String,
        val waitSeconds: Int,
        val accept: () -> Unit,
        val acceptAndOpenHotwire: () -> Unit,
    )

    class StoreCategory(val name: String, val parts: List<String>)

    class StoreHit(
        val projectId: String,
        val title: String,
        val author: String,
        val description: String,
        val downloads: Long,
        val iconUrl: String?,
        val installed: Boolean,
        val hasUpdate: Boolean,
        val queued: Boolean,
    )

    class StorePage(val hits: List<StoreHit>, val total: Int)

    class StoreUpdate(val modId: String, val projectId: String, val name: String, val installedVersion: String, val newVersion: String)

    /** Every top-level mod Hotwire knows */
    abstract fun mods(): List<ModState>

    /** A file holding the mod's icon, or null when its jar has none */
    abstract fun icon(modId: String): String?

    abstract fun plan(change: Change): CompletableFuture<Plan>

    /** How far the running switch, add or update is, from 0 to 1, or null while Hotwire isn't busy */
    abstract fun progress(): Float?

    /** Why Hotwire can't change mods this session, or null when it can or is still starting */
    abstract fun unavailable(): String?

    /** The downloads that plans with [tag] started, including ones that ended a moment ago */
    abstract fun downloads(tag: String): List<Download>

    abstract fun pauseDownload(id: Int, paused: Boolean)

    abstract fun stopDownload(id: Int)

    /** Hotwire's warning, which it refuses this mod's changes until it has been shown; null once it was accepted */
    abstract fun openingPrompt(): OpeningPrompt?

    /** Hotwire's warning before its first download from Modrinth; null once it was accepted */
    abstract fun downloadsWarning(): OpeningPrompt?

    abstract fun openScreen()

    /** Opens the folder Hotwire saves downloads to in the system's file manager */
    abstract fun openDownloads()

    /** Modrinth's mod categories as Hotwire groups them, where [StoreCategory.parts] are what a search can filter by */
    abstract fun storeCategories(): List<StoreCategory>

    abstract fun searchStore(words: String, category: String, offset: Int): CompletableFuture<StorePage>

    /** Saves the newest version that fits this game to Downloads */
    abstract fun saveToDownloads(idOrSlug: String): CompletableFuture<Result>

    /** Completes exceptionally with Hotwire's reason when it couldn't ask Modrinth */
    abstract fun checkUpdates(): CompletableFuture<List<StoreUpdate>>

    /** Whether the player left Hotwire's "Check for updates" on; while it is off, [checkUpdates] is only for a check the player asked for */
    abstract val updateCheckOn: Boolean

    abstract fun updateHotwire(): CompletableFuture<Result>

    /** Has Hotwire write a bug report zip, with the game log and what it did and personal details taken out */
    abstract fun writeReport(reason: String): CompletableFuture<Path>

    /** Completes once nothing is switching, adding or updating */
    abstract fun awaitIdle(): CompletableFuture<*>

    /** Whether the game is on someone else's server, where a switch that needs a reopen leaves it instead */
    abstract val onServer: Boolean

    /** Runs the action from a click, where a failure inside Hotwire comes back as a failed result, not a throw */
    class JarAction(val label: String, val primary: Boolean, private val run: () -> CompletableFuture<Result>) {
        fun attempt(): CompletableFuture<Result> = runCatching(run).getOrElse { CompletableFuture.completedFuture(failure(it)) }.exceptionally(::failure)
    }

    /** What Hotwire would do with a mod and the choices it offers, as its own add and update panels show */
    class JarPlan(
        val title: String,
        val detail: String,
        val lines: List<String>,
        val actions: List<JarAction>,
        val onCancel: () -> Unit = {},
        val reopens: Boolean = false,
    )

    /** What adding [hit] would do, including what Hotwire adds along with it, downloading under [tag] */
    fun storePlan(hit: StoreHit, tag: String): CompletableFuture<JarPlan> = plan(Change(add = hit.projectId, tag = tag)).thenApply { plan ->
        // offered even when Hotwire refuses the add, so the mod can still be added by hand
        val save = JarAction(translated("oneconfig.page.hotwire.add.save", "Save to Downloads"), false) {
            // by id, which stays the same if the project's slug changes
            saveToDownloads(hit.projectId)
        }
        plan.toJarPlan("add", hit.title, extra = listOf(save))
    }

    /** What Hotwire would do with [jar], adding it or updating the mod it already is */
    fun planFor(jar: Path): CompletableFuture<JarPlan> =
        plan(Change(jars = listOf(jar))).thenApply { it.toJarPlan("change", jar.fileName.toString()) }

    /** What applying [updates] together would do, with what they newly need, downloading under [tag] */
    fun planUpdate(updates: List<StoreUpdate>, tag: String): CompletableFuture<JarPlan> {
        val only = updates.singleOrNull()
        val detail = only?.let { "${it.installedVersion} → ${it.newVersion}" }.orEmpty()
        if (only != null && isSelf(only)) {
            return CompletableFuture.completedFuture(
                JarPlan(
                    only.name,
                    detail,
                    listOf(translated("oneconfig.page.hotwire.update.self", "Hotwire can't update itself while the game runs, so it updates at the next launch.")),
                    listOf(JarAction(translated("oneconfig.page.hotwire.update.next_start", "Update at next launch"), true, ::updateHotwire)),
                ),
            )
        }
        return plan(Change(update = updates.map { it.modId }, tag = tag)).thenApply { it.toJarPlan("update", updates.joinToString(", ") { u -> u.name }, detail) }
    }

    /**
     * Plans [updates] for review as one plan, so what one brings along isn't offered again; Hotwire updates itself
     * apart, since it can't go in a plan
     *
     * Hotwire refuses a whole plan for one mod it can't update, so a refused batch is planned again one mod at a time,
     * which fetches the jars again, so the others can still go
     */
    suspend fun planUpdates(updates: List<StoreUpdate>, tag: String): List<Pair<String, KotlinResult<JarPlan>>> {
        suspend fun plan(group: List<StoreUpdate>) = group.joinToString(", ") { it.name } to catching { planUpdate(group, tag).await() }
        val (self, mods) = updates.partition(::isSelf)
        val together = mods.takeIf { it.isNotEmpty() }?.let { plan(it) }
        if (together != null && mods.size > 1 && together.second.isSuccess && skipReason(together.second) != null) {
            together.second.getOrThrow().onCancel()
            return self.map { plan(listOf(it)) } + mods.map { plan(listOf(it)) }
        }
        return self.map { plan(listOf(it)) } + listOfNotNull(together)
    }

    // names the mods a change applied now and at the next launch, or is blank when it applied none
    private fun applied(now: List<String>, nextLaunch: List<String>): String {
        if (now.isEmpty() && nextLaunch.isEmpty()) return ""
        val names = mods().associate { it.id to it.name }
        fun named(ids: List<String>) = ids.distinct().joinToString(", ") { names[it] ?: it }
        return listOfNotNull(
            now.takeIf { it.isNotEmpty() }?.let { translated("oneconfig.page.hotwire.applied.now", "Applied now: %s.", named(it)) },
            nextLaunch.takeIf { it.isNotEmpty() }?.let { translated("oneconfig.page.hotwire.applied.next_start", "Applies at next launch: %s.", named(it)) },
        ).joinToString(" ")
    }

    /** What came of a change, naming what went through before one that didn't finish, which Hotwire's message for it doesn't */
    fun describe(result: Result): String {
        val applied = applied(result.appliedNow, result.atNextStart)
        return when {
            result.ok -> result.message.ifBlank { translated("oneconfig.page.hotwire.result.done", "Done.") }
            result.needsReopen -> listOf(applied, result.message.ifBlank { translated("oneconfig.page.hotwire.result.reopen", "Reopen your world to finish this.") }).filter { it.isNotBlank() }.joinToString(" ")
            else -> listOf(applied, result.message.ifBlank { translated("oneconfig.page.hotwire.result.failed", "That didn't work.") }).filter { it.isNotBlank() }.joinToString(" ")
        }
    }

    /** How far the running downloads of [tag] are together, or null while none is running */
    fun downloadProgress(tag: String): DownloadProgress? = downloadProgress(downloads(tag))

    fun pauseDownloads(tag: String, paused: Boolean) = downloads(tag).filterNot { it.over }.forEach { pauseDownload(it.id, paused) }

    fun stopDownloads(tag: String) = downloads(tag).filterNot { it.over }.forEach { stopDownload(it.id) }

    /** [fraction] goes from 0 to 1, [paused] says every running download is paused, and [name] is the one being fetched */
    class DownloadProgress(val fraction: Float, val paused: Boolean, val name: String, private val ids: Set<Int>) {
        /** What a bar showing [shown] for [before] moves to: a finished jar leaving must not pull it back, but a new jar starts it over */
        fun shownAfter(before: DownloadProgress?, shown: Float): Float =
            if (before != null && before.ids.containsAll(ids)) maxOf(fraction, shown) else fraction
    }

    companion object {
        /** Set by `minecraft` once Hotwire is installed */
        @JvmStatic
        var instance: HotwireBridge? = null

        /** Bumped by [invalidate], which runs whenever Hotwire finishes switching, starting, or updating mods, whoever asked */
        var revision by mutableIntStateOf(0)
            private set

        fun invalidate() {
            revision++
        }

        /** [invalidate] once Hotwire is idle, so a change it is still recording shows whole */
        @JvmStatic
        fun invalidateWhenIdle() {
            instance?.awaitIdle()?.whenComplete { _, _ -> invalidate() } ?: invalidate()
        }

        private val tags = AtomicInteger()

        /** A tag for the downloads of one plan, so pausing or stopping them leaves everyone else's alone */
        fun newTag(): String = "oneconfig-${tags.incrementAndGet()}"

        fun isSelf(update: StoreUpdate) = update.modId == "hotwire"

        /** Why a planned update won't be applied with the others, or null when it will */
        fun skipReason(plan: KotlinResult<JarPlan>): String? = when {
            plan.getOrNull()?.reopens == true -> translated("oneconfig.page.hotwire.updates.reopen", "It has to save and reopen your world, so update it on its own.")
            plan.getOrNull()?.actions?.any { it.primary } == true -> null
            // the refusal can come after lines saying what would have gone fine, so all of them are the reason
            else -> (plan.exceptionOrNull()?.let { it.cause ?: it }?.message ?: plan.getOrNull()?.lines?.joinToString(" ")).orEmpty()
        }

        /** [earlier] results followed by [page]'s, where a project Modrinth moved to the next page between the two shows once */
        fun appendPage(earlier: List<StoreHit>, page: List<StoreHit>): List<StoreHit> = (earlier + page).distinctBy { it.projectId }

        fun downloadProgress(downloads: List<Download>): DownloadProgress? {
            val running = downloads.filterNot { it.over }.ifEmpty { return null }
            return DownloadProgress(
                (running.sumOf { it.done }.toFloat() / running.sumOf { it.total }.coerceAtLeast(1)).coerceAtMost(1f),
                running.all { it.paused },
                (running.firstOrNull { it.getting } ?: running.first()).name,
                running.mapTo(HashSet()) { it.id },
            )
        }

        /** [runCatching] for a wait, where a cancelled wait ends the coroutine rather than coming back as a failure */
        internal inline fun <T> catching(block: () -> T): KotlinResult<T> = try {
            KotlinResult.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            KotlinResult.failure(e)
        }

        private fun failure(error: Throwable): Result {
            val reason = error.cause ?: error
            return Result(false, reason.message ?: reason.toString())
        }

        /**
         * The choices a change plan offers, labelled from the [kind] of change: "add", "update" or "change", followed
         * by [extra] ones that are offered even when Hotwire refuses
         *
         * The main one does what each line says, live where it can and at the next launch where it can't, so a plan
         * that needs a restart for part of it isn't labelled as all live or all waiting
         */
        fun Plan.toJarPlan(kind: String, title: String, detail: String = "", extra: List<JarAction> = emptyList()): JarPlan {
            var acted = false
            fun action(label: String, primary: Boolean, run: () -> CompletableFuture<Result>) = JarAction(label, primary) {
                acted = true
                run()
            }
            val now = action(
                when {
                    needsReopen -> translated("oneconfig.page.hotwire.$kind.reopen", "Save and apply")
                    restartOnly -> translated("oneconfig.page.hotwire.$kind.as_planned", "Apply")
                    else -> translated("oneconfig.page.hotwire.$kind.now", "Apply now")
                },
                true,
                ::accept,
            )
            val later = action(translated("oneconfig.page.hotwire.$kind.next_start", "Apply at next launch"), false, ::acceptAtNextStart)
            val actions = when {
                !canApply -> emptyList()
                canWaitForNextStart -> listOf(now, later)
                else -> listOf(now)
            }
            // an extra action leaves this plan behind, so it lets go of what the plan downloaded
            val others = extra.map { other ->
                action(other.label, other.primary) {
                    cancel()
                    other.attempt()
                }
            }
            return JarPlan(title, detail, lines, actions + others, onCancel = { if (!acted) cancel() }, reopens = needsReopen)
        }

        /** Where icons are written for the pages to show, a folder only this process can use, so no one can plant a file or link there */
        val iconDir: Path by lazy {
            Files.createTempDirectory("oneconfig-hotwire").also { it.toFile().deleteOnExit() }
        }

        /** Downloads a Modrinth icon once per session; blocks on the network */
        fun iconFor(url: String): Path {
            // named by the url so a remote path can never pick where the file lands
            val ext = url.substringAfterLast('.').lowercase().takeIf { it.length in 3..4 && it.all(Char::isLetter) } ?: "png"
            val file = iconDir.resolve("${Integer.toHexString(url.hashCode())}-${url.length}.$ext")
            if (!Files.exists(file)) {
                val uri = URI(url)
                require(uri.scheme == "https") { "Icons only load over https." }
                val connection = uri.toURL().openConnection().apply {
                    connectTimeout = ICON_TIMEOUT_MS
                    readTimeout = ICON_TIMEOUT_MS
                }
                val bytes = connection.getInputStream().use { it.readNBytes(MAX_ICON_BYTES) }
                // moved into place whole, so an icon fetched twice at once is never read half-written
                val temp = Files.createTempFile(iconDir, file.fileName.toString(), ".tmp")
                try {
                    Files.write(temp, bytes)
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    file.toFile().deleteOnExit()
                } finally {
                    Files.deleteIfExists(temp)
                }
            }
            return file
        }

        /** The file suffix for an icon's [bytes], as a jar's icon can be an SVG though Fabric asks for a PNG */
        fun iconSuffix(bytes: ByteArray) =
            if (String(bytes, 0, minOf(bytes.size, 64), Charsets.UTF_8).trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith('<')) ".svg" else ".png"

        private const val MAX_ICON_BYTES = 8 * 1024 * 1024

        private const val ICON_TIMEOUT_MS = 10_000
    }
}
