package org.polyfrost.oneconfig.internal.ui.api

import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HotwireSelfUpdateQueueTest {
    class Old(private val mod: String, private val dependency: String, private val wanted: String = "") {
        fun mod() = mod
        fun dependency() = dependency
        fun wanted() = wanted
    }

    class Breaks

    class MissingDependency(private val mod: String = "lib") {
        fun mod() = mod
    }

    class Plan(private val direction: String, private val order: List<*>, private val why: Map<*, *>, private val nested: List<*>, private val refusals: List<Any>) {
        fun direction() = direction
        fun order() = order
        fun why() = why
        fun enabledNested() = nested
        fun refusals() = refusals
    }

    class Planner(private val refused: List<Any>, private val old: Map<String, List<Any>>, private val nestedRefused: List<Any> = emptyList()) {
        @Suppress("UNUSED_PARAMETER")
        fun planOn(ids: Collection<String>, off: Set<String>) =
            Plan("ON", emptyList<Any>(), emptyMap<Any, Any>(), emptyList<Any>(), if ("hotwire" in ids) refused else nestedRefused)

        fun findOldDependencies(id: String) = old[id].orEmpty()
    }

    class Graph(private val dependents: Map<String, Set<String>>, private val roots: Set<String> = setOf("hotwire", "other"), private val present: Set<String> = emptySet()) {
        fun getDependents(id: String) = dependents[id].orEmpty()
        fun isInstalledOnItsOwn(id: String) = id in roots
        fun find(id: String): Optional<String> = Optional.ofNullable(id.takeIf { it in present })
    }

    private fun refusals(planner: Planner, graph: Graph, builtIns: List<Any> = emptyList()) =
        (HotwireBridge.refusedPlan(planner, graph, listOf("hotwire"), builtIns) as Plan?)?.refusals()

    @Test
    fun aNewVersionThatFitsIsNotRefused() {
        val unrelated = Old("other", "lib")
        assertNull(refusals(Planner(emptyList(), mapOf("other" to listOf(unrelated))), Graph(mapOf("hotwire" to setOf("other")))))
    }

    @Test
    fun aDependencyTooOldForTheNewVersionIsRefused() {
        val old = Old("hotwire", "fabric-api")
        assertEquals(listOf(old), refusals(Planner(emptyList(), mapOf("hotwire" to listOf(old))), Graph(emptyMap())))
    }

    @Test
    fun aModThatNeedsAnOlderVersionRefusesIt() {
        val old = Old("other", "hotwire")
        assertEquals(listOf(old), refusals(Planner(emptyList(), mapOf("other" to listOf(old))), Graph(mapOf("hotwire" to setOf("other")))))
    }

    @Test
    fun aRefusalOfThePlanIsKept() {
        assertEquals(listOf("breaks"), refusals(Planner(listOf("breaks"), emptyMap()), Graph(emptyMap())))
    }

    @Test
    fun aBuiltInTooOldForTheNewVersionIsReported() {
        val old = HotwireBridge.oldBuiltIns(
            "hotwire",
            mapOf("java" to listOf(">=25"), "minecraft" to listOf("26.3"), "fabric-api" to listOf(">=2")),
            mapOf("java" to "21", "minecraft" to "26.3")::get,
        ) { version, ranges -> version == ranges.single() }
        assertEquals(listOf(listOf("hotwire", "java", ">=25", "21")), old)
    }

    @Test
    fun aTooOldBuiltInRefusesThePlan() {
        assertEquals(listOf("old java"), refusals(Planner(emptyList(), emptyMap()), Graph(emptyMap()), listOf("old java")))
    }

    private fun nestedRefusals(planner: Planner, graph: Graph, vararg versions: String) =
        (HotwireBridge.refusedPlan(planner, graph, listOf("hotwire"), emptyList(), mapOf("lib" to versions.toList())) { version, ranges -> version in ranges } as Plan?)?.refusals()

    @Test
    fun aModNeedingAnotherVersionOfANestedModRefusesIt() {
        val old = Old("other", "lib", "1")
        assertEquals(listOf(old), nestedRefusals(Planner(emptyList(), mapOf("other" to listOf(old))), Graph(mapOf("lib" to setOf("other"))), "2"))
    }

    @Test
    fun aNestedModWithACopyThatFitsIsNotRefused() {
        val old = Old("other", "lib", "1 or 3")
        assertNull(nestedRefusals(Planner(emptyList(), mapOf("other" to listOf(old))), Graph(mapOf("lib" to setOf("other"))), "2", "3"))
    }

    @Test
    fun aDependentsOtherOldDependencyIsNotBlamedOnANestedMod() {
        val old = Old("other", "unrelated", "1")
        assertNull(nestedRefusals(Planner(emptyList(), mapOf("other" to listOf(old))), Graph(mapOf("lib" to setOf("other"))), "2"))
    }

    @Test
    fun aNestedModInAConflictRefusesIt() {
        val breaks = Breaks()
        assertEquals(listOf(breaks), nestedRefusals(Planner(emptyList(), emptyMap(), listOf(breaks)), Graph(emptyMap()), "2"))
    }

    @Test
    fun aConflictWithOneOfSeveralNestedVersionsIsNotRefused() {
        assertNull(nestedRefusals(Planner(emptyList(), emptyMap(), listOf(Breaks())), Graph(emptyMap()), "2", "3"))
    }

    @Test
    fun aNestedModsOwnMissingNeedIsNotRefused() {
        assertNull(nestedRefusals(Planner(emptyList(), emptyMap(), listOf(MissingDependency())), Graph(emptyMap()), "2"))
    }

    @Test
    fun aRequiredNestedModsMissingNeedRefusesIt() {
        val missing = MissingDependency()
        assertEquals(listOf(missing), nestedRefusals(Planner(emptyList(), emptyMap(), listOf(missing)), Graph(mapOf("lib" to setOf("hotwire"))), "2"))
    }

    @Test
    fun aMissingNeedOfANestedModOnlyAnOptionalModWantsIsNotRefused() {
        assertNull(nestedRefusals(Planner(emptyList(), emptyMap(), listOf(MissingDependency())), Graph(mapOf("lib" to setOf("unused"))), "2"))
    }

    @Test
    fun aRemovedNestedModAStayingModNeedsIsReported() {
        val graph = Graph(mapOf("lib" to setOf("other", "unused"), "kept" to setOf("other")), present = setOf("kept"))
        assertEquals(listOf("other" to "lib"), HotwireBridge.lostNeeds(graph, listOf("lib", "kept", "lib")))
    }

    @Test
    fun aFailedStepPutsTheQueueBack() {
        val queue = RollbackQueue(listOf("earlier"))
        val failed = HotwireBridge.Result(false, "no")

        val result = HotwireBridge.queueTogether(queue, listOf({ queue.current += "need"; HotwireBridge.Result(true, "") }, { failed }))

        assertEquals(failed, result)
        assertEquals(listOf("earlier"), queue.current)
    }

    @Test
    fun aThrowingStepPutsTheQueueBack() {
        val queue = RollbackQueue(emptyList())

        val result = HotwireBridge.queueTogether(queue, listOf({ queue.current += "need"; error("broken") }))

        assertEquals("broken", result?.message)
        assertEquals(emptyList<String>(), queue.current)
    }

    @Test
    fun stepsThatAllLandStayQueued() {
        val queue = RollbackQueue(emptyList())

        assertNull(HotwireBridge.queueTogether(queue, listOf({ queue.current += "need"; HotwireBridge.Result(true, "") })))
        assertEquals(listOf("need"), queue.current)
    }

    @Test
    fun aJarNameMayOnlyReplaceTheRunningJar(@TempDir mods: Path, @TempDir staging: Path) {
        val running = Files.writeString(mods.resolve("hotwire-1.jar"), "old")
        val other = Files.writeString(mods.resolve("other.jar"), "other")
        fun incoming(name: String, content: String = "new") = Files.writeString(staging.resolve(name), content)

        assertFalse(HotwireBridge.nameTaken(incoming("hotwire-1.jar"), running, running, emptyList()))
        assertFalse(HotwireBridge.nameTaken(incoming("hotwire-1.jar"), running, Path.of("/hotwire/added/hotwire-1.jar"), emptyList()))
        assertFalse(HotwireBridge.nameTaken(incoming("hotwire-2.jar"), mods.resolve("hotwire-2.jar"), running, emptyList()))
        assertTrue(HotwireBridge.nameTaken(incoming("other.jar"), other, running, emptyList()))
        assertTrue(HotwireBridge.nameTaken(incoming("other.jar"), other, null, emptyList()))
        assertTrue(HotwireBridge.nameTaken(incoming("hotwire-2.jar"), mods.resolve("hotwire-2.jar"), running, listOf("/staging/hotwire-2.jar")))
    }

    @Test
    fun theSameJarIsNotAClash(@TempDir mods: Path, @TempDir staging: Path) {
        val present = Files.writeString(mods.resolve("mod.jar"), "bytes")

        assertFalse(HotwireBridge.nameTaken(present, present, null, emptyList()))
        assertFalse(HotwireBridge.nameTaken(Files.writeString(staging.resolve("mod.jar"), "bytes"), present, null, emptyList()))
    }

    class RollbackQueue(start: List<String>) {
        var current = start

        fun getEntries() = current

        @Suppress("unused")
        private fun replace(entries: List<String>) {
            current = entries
        }
    }

    data class Entry(
        val modId: String,
        val name: String,
        val fromVersion: String,
        val toVersion: String,
        val runningJar: String,
        val runningSha256: String,
        val stagedJar: String,
        val stagedSha256: String,
        val reason: String,
        val queuedAtMillis: Long,
    )

    class Manifest {
        fun id() = "hotwire"
        fun name() = "Hotwire"
        fun version() = "2"
    }

    class Queue {
        val writes = mutableListOf<Entry>()

        fun add(entry: Entry) {
            writes.add(entry)
        }
    }

    @Test
    fun aSelfUpdateIsQueuedAsAReplacementInOneWrite() {
        val queue = Queue()

        HotwireBridge.queueReplacement(queue, Entry::class.java, Manifest(), "1", Path.of("/mods/hotwire-1.jar"), "old", Path.of("/staging/hotwire-2.jar"), "new")

        assertEquals(
            listOf(Entry("hotwire", "Hotwire", "1", "2", "hotwire-1.jar", "old", "/staging/hotwire-2.jar", "new", "", 0)),
            queue.writes.map { it.copy(queuedAtMillis = 0) },
        )
    }
}
