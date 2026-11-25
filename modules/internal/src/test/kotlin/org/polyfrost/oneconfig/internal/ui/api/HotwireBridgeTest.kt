package org.polyfrost.oneconfig.internal.ui.api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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

    /** Hotwire with only planning and saving, which answers each plan from [plans] by the mods it updates */
    private inner class FakeHotwire(val plans: (HotwireBridge.Change) -> HotwireBridge.Plan) : HotwireBridge() {
        override fun plan(change: Change): CompletableFuture<Plan> {
            calls += "plan ${change.update + listOfNotNull(change.add)}"
            return CompletableFuture.completedFuture(plans(change))
        }

        override fun saveToDownloads(idOrSlug: String, anyVersion: Boolean) = done().also { calls += "save $idOrSlug any=$anyVersion" }

        override fun updateHotwire() = done().also { calls += "update hotwire" }

        override fun mods() = TODO()

        override fun icon(modId: String) = TODO()

        override fun preview(ids: Collection<String>, on: Boolean) = TODO()

        override fun switch(ids: Collection<String>, on: Boolean) = TODO()

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

        override fun checkUpdates() = TODO()

        override fun writeReport(reason: String): CompletableFuture<Path> = TODO()

        override fun awaitIdle() = TODO()
    }

    private val hit = HotwireBridge.StoreHit("project", "slug", "Mod", "", "", 0, null, installed = false, hasUpdate = false)

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
    fun anAddHotwireRefusedCanStillBeSavedForAnyVersion() {
        val plan = FakeHotwire { FakePlan(canApply = false) }.storePlan(hit, "tag").join()
        assertEquals(1, plan.actions.size)
        plan.actions.single().attempt().join()
        plan.onCancel()
        assertEquals(listOf("plan [project]", "cancel plan", "save slug any=true"), calls)
    }

    @Test
    fun anAddHotwireAllowsIsSavedForThisGame() {
        val plan = FakeHotwire { FakePlan() }.storePlan(hit, "tag").join()
        plan.actions.last().attempt().join()
        assertEquals("save slug any=false", calls.last())
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

    private fun download(id: Int, done: Long, over: Boolean = false, paused: Boolean = false) =
        HotwireBridge.Download(id, "mod.jar", done, 100, over, paused, !paused && !over)

    @Test
    fun finishedDownloadsAreNotProgress() {
        assertNull(HotwireBridge.downloadProgress(listOf(download(1, 100, over = true))))
    }

    @Test
    fun progressCoversEveryRunningJar() {
        val progress = HotwireBridge.downloadProgress(listOf(download(1, 100, over = true), download(2, 10), download(3, 30, paused = true)))!!
        assertEquals(0.2f, progress.fraction)
        assertFalse(progress.paused)
    }

    @Test
    fun aFinishedJarLeavingDoesNotPullTheBarBack() {
        val before = HotwireBridge.downloadProgress(listOf(download(1, 90), download(2, 90)))
        assertEquals(0.9f, HotwireBridge.downloadProgress(listOf(download(2, 50)))!!.shownAfter(before, 0.9f))
    }

    @Test
    fun aNewJarStartsTheBarOver() {
        val before = HotwireBridge.downloadProgress(listOf(download(1, 100)))
        assertEquals(0.1f, HotwireBridge.downloadProgress(listOf(download(2, 10)))!!.shownAfter(before, 1f))
    }
}
