package org.polyfrost.oneconfig.internal.compat

import dev.moon.hotwire.api.ApiStatus
import dev.moon.hotwire.api.ChangePlan
import dev.moon.hotwire.api.ChangeRequest
import dev.moon.hotwire.api.HotwireApi
import dev.moon.hotwire.api.Reason
import dev.moon.hotwire.api.StoreDownload
import dev.moon.hotwire.api.StoreKind
import dev.moon.hotwire.api.StoreQuery
import dev.moon.hotwire.api.SwitchResult
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.CustomValue.CvType
import net.fabricmc.loader.api.metadata.ModMetadata
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.internal.ui.api.HotwireBridge
import dev.moon.hotwire.api.OpeningPrompt as HotwirePrompt

/** [HotwireBridge] through Hotwire's API, which names Hotwire's types, so it must only be loaded when Hotwire is installed */
class HotwireBridgeImpl(private val api: HotwireApi) : HotwireBridge() {
    init {
        // every listener call comes on the game thread, which is the one the pages are composed on
        api.addListener { invalidate() }
    }

    // by mod id, as the loader no longer lists a mod while Hotwire has it off
    private val authors = HashMap<String, String>()
    private val libraries = HashSet<String>()

    override fun mods() = api.mods.map {
        FabricLoader.getInstance().getModContainer(it.id()).ifPresent { mod ->
            authors[it.id()] = mod.metadata.authors.joinToString(", ") { person -> person.name }
            if (isLibrary(mod.metadata)) libraries += it.id()
        }
        ModState(it.id(), it.name(), it.version(), authors[it.id()], it.on(), it.switchable(), it.offSinceStart(), it.id() in libraries)
    }

    override fun icon(modId: String): String? {
        val bytes = api.findIcon(modId).orElse(null) ?: return null
        // a new file each time, as a mod updated since must not show its old icon from a path already loaded
        val file = Files.createTempFile(iconDir, "mod-icon-", iconSuffix(bytes))
        file.toFile().deleteOnExit()
        Files.write(file, bytes)
        return file.toAbsolutePath().toString()
    }

    override fun plan(change: Change): CompletableFuture<Plan> {
        var request = ChangeRequest().off(change.off).on(change.on).update(change.update).tag(change.tag)
        change.jars.forEach { request = request.jar(it) }
        change.add?.let { request = request.add(it) }
        return api.planChange(request).thenApply(::PlanImpl)
    }

    private class PlanImpl(private val plan: ChangePlan) : Plan {
        override val lines: List<String> = plan.lines
        override val canApply = plan.canApply()
        override val restartOnly = plan.reason == Reason.NEEDS_RESTART
        override val needsReopen = plan.needsReopen()
        override val canWaitForNextStart = plan.canWaitForNextStart()

        override fun accept() = plan.accept().thenApply(::result)

        override fun acceptAtNextStart() = plan.acceptAtNextStart().thenApply(::result)

        override fun cancel() = plan.cancel()
    }

    // by status rather than progress, which leaves out changes made from Hotwire's own screen and its self update
    override fun progress() = api.status.takeIf { it.state() == ApiStatus.State.BUSY }?.progress()?.toFloat()

    override fun unavailable() = api.status.takeIf { it.state() == ApiStatus.State.UNAVAILABLE }?.message()

    override fun downloads(tag: String) = api.storeDownloads.filter { it.tag() == tag }.map {
        Download(it.id(), it.name(), it.done(), it.total(), it.over(), it.state() == StoreDownload.State.PAUSED, it.state() == StoreDownload.State.GETTING)
    }

    override fun pauseDownload(id: Int, paused: Boolean) {
        if (paused) api.pauseDownload(id) else api.resumeDownload(id)
    }

    override fun stopDownload(id: Int) {
        api.stopDownload(id)
    }

    override fun openingPrompt() = api.findOpeningPrompt()?.let(::prompt)

    override fun downloadsWarning() = api.findDownloadsWarning()?.let(::prompt)

    private fun prompt(prompt: HotwirePrompt) = OpeningPrompt(
        prompt.title,
        prompt.lines,
        prompt.acceptText,
        prompt.openHotwireText,
        prompt.waitSeconds,
        {
            prompt.accept()
            invalidate()
        },
        {
            prompt.acceptAndOpenHotwire()
            invalidate()
        },
    )

    override fun openScreen() = api.openScreen()

    override fun openDownloads() = api.openDownloads()

    override fun storeCategories() = api.getStoreCategories(StoreKind.MOD).map { StoreCategory(it.name(), it.parts()) }

    override fun searchStore(words: String, category: String, offset: Int) =
        api.searchStore(StoreQuery.of(words).withCategory(category).withOffset(offset)).thenApply { page ->
            StorePage(
                page.mods().map {
                    StoreHit(it.id(), it.name(), it.author(), it.description(), it.downloads(), it.iconUrl().ifBlank { null }, it.installed(), it.hasUpdate(), it.queued())
                },
                page.total(),
            )
        }

    override fun saveToDownloads(idOrSlug: String) = api.saveToDownloads(StoreQuery.of(""), idOrSlug).thenApply(::result)

    override fun checkUpdates() = api.checkUpdates().thenApply { found ->
        // without an answer from Modrinth there may still be updates, so this mustn't read as none
        check(found.failure().isBlank()) { found.failure() }
        found.updates().map { StoreUpdate(it.id(), it.projectId(), it.name(), it.installedVersion(), it.newVersion()) }
    }

    override val updateCheckOn get() = api.isUpdateCheckOn

    override fun updateHotwire() = api.updateHotwire().thenApply(::result)

    override fun writeReport(reason: String): CompletableFuture<Path> = api.writeReport(reason)

    override fun awaitIdle(): CompletableFuture<*> = api.awaitIdle()

    override val onServer get() = !Minecraft.getInstance().isLocalServer

    private companion object {
        // by the badge Mod Menu shows, as Hotwire's own list tells libraries
        fun isLibrary(metadata: ModMetadata): Boolean {
            val badges = metadata.getCustomValue("modmenu")?.takeIf { it.type == CvType.OBJECT }?.asObject?.get("badges")
            return badges != null && badges.type == CvType.ARRAY && badges.asArray.any { it.type == CvType.STRING && it.asString == "library" }
        }

        fun result(result: SwitchResult) = Result(
            result.isDone,
            result.message(),
            needsReopen = result.kind() == SwitchResult.Kind.NEEDS_REOPEN,
            refused = result.kind() == SwitchResult.Kind.REFUSED,
            appliedNow = result.appliedNow(),
            atNextStart = result.atNextStart(),
        )
    }
}
