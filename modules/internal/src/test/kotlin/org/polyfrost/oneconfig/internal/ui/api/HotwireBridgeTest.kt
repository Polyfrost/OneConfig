package org.polyfrost.oneconfig.internal.ui.api

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.oneconfig.internal.ui.screens.HotwireUpdates
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class HotwireBridgeTest {
    private val calls = ArrayList<String>()

    private fun done() = CompletableFuture.completedFuture(HotwireBridge.Result(true, ""))

    private inner class FakePlan(
        override val canApply: Boolean = true,
        override val restartOnly: Boolean = false,
        override val canWaitForNextStart: Boolean = true,
        val name: String = "plan",
    ) : HotwireBridge.Plan {
        override val lines = listOf("line")
        override val needsReopen = false

        override fun accept() = done().also { calls += "accept $name" }

        override fun acceptAtNextStart() = done().also { calls += "next start $name" }

        override fun cancel() {
            calls += "cancel $name"
        }
    }

    /** Hotwire with only planning and saving, which answers each plan from [plans] by the mods it updates, or with [answer] */
    private inner class FakeHotwire(
        val answer: ((HotwireBridge.Change) -> CompletableFuture<HotwireBridge.Plan>)? = null,
        override val updateCheckOn: Boolean = true,
        val plans: (HotwireBridge.Change) -> HotwireBridge.Plan = { FakePlan() },
    ) : HotwireBridge() {
        override fun plan(change: Change): CompletableFuture<Plan> {
            calls += "plan ${change.update + listOfNotNull(change.add)}"
            return answer?.invoke(change) ?: CompletableFuture.completedFuture(plans(change))
        }

        override fun saveToDownloads(idOrSlug: String) = done().also { calls += "save $idOrSlug" }

        override fun updateHotwire() = done().also { calls += "update hotwire" }

        override fun mods() = listOf(ModState("a", "Alpha", "1", null, on = true, switchable = true, offSinceStart = false))

        override fun icon(modId: String) = TODO()

        override fun progress() = TODO()

        override fun downloads(tag: String) = TODO()

        override fun pauseDownload(id: Int, paused: Boolean) = TODO()

        override fun stopDownload(id: Int) = TODO()

        override fun openingPrompt() = TODO()

        override fun downloadsWarning() = TODO()

        override fun openScreen() = TODO()

        override fun openDownloads() = TODO()

        override fun storeCategories() = TODO()

        override fun searchStore(words: String, category: String, offset: Int) = TODO()

        override fun checkUpdates() = CompletableFuture.completedFuture(listOf(update("a"), update("b"))).also { calls += "check" }

        override fun writeReport(reason: String): CompletableFuture<Path> = TODO()

        override fun awaitIdle() = CompletableFuture.completedFuture(null)

        override fun unavailable() = TODO()
    }

    @AfterEach
    fun forgetHotwire() {
        HotwireBridge.instance = null
    }

    private val hit = HotwireBridge.StoreHit("project", "Mod", "", "", 0, null, installed = false, hasUpdate = false, queued = false)

    private fun update(id: String) = HotwireBridge.StoreUpdate(id, "p-$id", id.uppercase(), "1", "2")

    @Test
    fun theMainButtonGoesLiveWhereItCanEvenWhenPartNeedsARestart() {
        val plan = FakeHotwire { FakePlan(restartOnly = true) }.planFor(Path.of("mod.jar")).join()
        val main = plan.actions.first()
        assertTrue(main.primary)
        main.attempt().join()
        assertEquals(listOf("plan []", "accept plan"), calls)
    }

    @Test
    fun waitingForTheNextLaunchIsOfferedWhenHotwireAllowsIt() {
        FakeHotwire { FakePlan() }.planFor(Path.of("mod.jar")).join().actions[1].attempt().join()
        assertEquals(listOf("plan []", "next start plan"), calls)
    }

    @Test
    fun anAddHotwireRefusedCanStillBeSaved() {
        val plan = FakeHotwire { FakePlan(canApply = false) }.storePlan(hit, "tag").join()
        assertEquals(1, plan.actions.size)
        plan.actions.single().attempt().join()
        plan.onCancel()
        assertEquals(listOf("plan [project]", "cancel plan", "save project"), calls)
    }

    @Test
    fun aDoneChangeSaysWhatHotwireSaidOfIt() {
        val said = "Alpha goes in when you close the game."
        assertEquals(said, FakeHotwire().describe(HotwireBridge.Result(true, said, atNextStart = listOf("a"))))
    }

    @Test
    fun withHotwiresChecksOffOnlyACheckThePlayerAskedForRuns() {
        HotwireBridge.instance = FakeHotwire(updateCheckOn = false)
        // so a check another test made doesn't count as current
        HotwireBridge.invalidate()
        HotwireUpdates.ensure()
        assertEquals(emptyList<String>(), calls)
        HotwireUpdates.check()
        assertEquals(listOf("check"), calls)
    }

    @Test
    fun withHotwiresChecksOffAnUpdatedModLeavesTheListWithoutACheck() {
        HotwireBridge.instance = FakeHotwire(updateCheckOn = false)
        HotwireUpdates.check()
        HotwireUpdates.refreshWhenDone(listOf("a"))
        assertEquals(setOf("b"), HotwireUpdates.modIds)
        assertEquals(listOf("check"), calls)
    }

    @Test
    fun aChangeStoppedForAReopenSaysWhatWentThrough() {
        val said = FakeHotwire().describe(HotwireBridge.Result(false, "Reopen the world.", needsReopen = true, appliedNow = listOf("a")))
        assertTrue("Alpha" in said)
        assertTrue("Reopen the world." in said)
    }

    @Test
    fun aChangeThatFailedPartwaySaysWhatWentThrough() {
        val said = FakeHotwire().describe(HotwireBridge.Result(false, "Beta failed.", appliedNow = listOf("a")))
        assertTrue("Alpha" in said)
        assertTrue("Beta failed." in said)
    }

    @Test
    fun leavingAPlanUnusedLetsItGo() {
        FakeHotwire { FakePlan() }.planFor(Path.of("mod.jar")).join().onCancel()
        assertEquals(listOf("plan []", "cancel plan"), calls)
    }

    @Test
    fun aPlanActedOnIsNotLetGoOnLeaving() {
        val plan = FakeHotwire { FakePlan() }.planFor(Path.of("mod.jar")).join()
        plan.actions.first().attempt().join()
        plan.onCancel()
        assertFalse("cancel plan" in calls)
    }

    @Test
    fun aRefusedBatchIsPlannedAgainOneModAtATime() {
        val hotwire = FakeHotwire { change -> FakePlan(canApply = change.update.size == 1, name = change.update.joinToString()) }
        val plans = runBlocking { hotwire.planUpdates(listOf(update("a"), update("b")), "tag") }
        assertEquals(listOf("A", "B"), plans.map { it.first })
        assertTrue(plans.all { HotwireBridge.skipReason(it.second) == null })
        assertTrue("cancel a, b" in calls)
    }

    @Test
    fun aBatchGivenUpOnComesBackWithNothing() {
        val hotwire = FakeHotwire(answer = { CompletableFuture() })
        var plans: Any? = null
        runBlocking {
            val planning = launch(start = CoroutineStart.UNDISPATCHED) { plans = hotwire.planUpdates(listOf(update("a"), update("b")), "tag") }
            planning.cancel()
            planning.join()
        }
        assertNull(plans)
        assertEquals(listOf("plan [a, b]"), calls)
    }

    @Test
    fun aBatchHotwireAllowsStaysOnePlan() {
        val plans = runBlocking { FakeHotwire { FakePlan() }.planUpdates(listOf(update("a"), update("b")), "tag") }
        assertEquals(listOf("A, B"), plans.map { it.first })
    }

    @Test
    fun hotwireUpdatesItselfApartFromTheBatch() {
        val plans = runBlocking { FakeHotwire { FakePlan() }.planUpdates(listOf(update("a"), update("hotwire")), "tag") }
        assertEquals(listOf("HOTWIRE", "A"), plans.map { it.first })
        plans.first().second.getOrThrow().actions.single().attempt().join()
        assertEquals(listOf("plan [a]", "update hotwire"), calls)
    }

    @Test
    fun aProjectModrinthMovedToTheNextPageIsListedOnce() {
        fun hit(id: String) = HotwireBridge.StoreHit(id, id, "", "", 0, null, installed = false, hasUpdate = false, queued = false)
        val listed = HotwireBridge.appendPage(listOf(hit("a"), hit("b")), listOf(hit("b"), hit("c")))
        assertEquals(listOf("a", "b", "c"), listed.map { it.projectId })
    }

    private fun download(id: Int, done: Long, over: Boolean = false, paused: Boolean = false) =
        HotwireBridge.Download(id, "mod.jar", done, 100, over, paused, !paused && !over)

    @Test
    fun finishedDownloadsAreNotProgress() {
        assertNull(HotwireBridge.downloadProgress(listOf(download(1, 100, over = true))))
    }

    @Test
    fun progressCoversEveryRunningJar() {
        val progress = HotwireBridge.downloadProgress(listOf(download(1, 100, over = true), download(2, 10), download(3, 30, paused = true)))
        assertEquals(0.2f, progress?.fraction)
        assertEquals(false, progress?.paused)
    }

    @Test
    fun aFinishedJarLeavingDoesNotPullTheBarBack() {
        val before = HotwireBridge.downloadProgress(listOf(download(1, 90), download(2, 90)))
        assertEquals(0.9f, HotwireBridge.downloadProgress(listOf(download(2, 50)))?.shownAfter(before, 0.9f))
    }

    @Test
    fun aNewJarStartsTheBarOver() {
        val before = HotwireBridge.downloadProgress(listOf(download(1, 100)))
        assertEquals(0.1f, HotwireBridge.downloadProgress(listOf(download(2, 10)))?.shownAfter(before, 1f))
    }
}
