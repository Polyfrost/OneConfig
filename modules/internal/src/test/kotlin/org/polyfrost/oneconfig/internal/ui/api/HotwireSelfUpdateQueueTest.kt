package org.polyfrost.oneconfig.internal.ui.api

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HotwireSelfUpdateQueueTest {
    class Old(private val mod: String, private val dependency: String) {
        fun mod() = mod
        fun dependency() = dependency
    }

    class Plan(private val direction: String, private val order: List<*>, private val why: Map<*, *>, private val nested: List<*>, private val refusals: List<Any>) {
        fun direction() = direction
        fun order() = order
        fun why() = why
        fun enabledNested() = nested
        fun refusals() = refusals
    }

    class Planner(private val refused: List<Any>, private val old: Map<String, List<Any>>) {
        @Suppress("UNUSED_PARAMETER")
        fun planOn(ids: Collection<String>, off: Set<String>) = Plan("ON", emptyList<Any>(), emptyMap<Any, Any>(), emptyList<Any>(), refused)
        fun findOldDependencies(id: String) = old[id].orEmpty()
    }

    class Graph(private val dependents: Map<String, Set<String>>) {
        fun getDependents(id: String) = dependents[id].orEmpty()
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
    ) {
        fun modId() = modId
        fun name() = name
        fun toVersion() = toVersion
        fun stagedJar() = stagedJar
        fun stagedSha256() = stagedSha256
        fun reason() = reason
        fun queuedAtMillis() = queuedAtMillis
    }

    class Queue(val entries: MutableList<Entry>) {
        fun add(entry: Entry) {
            entries.removeIf { it.modId == entry.modId }
            entries.add(entry)
        }
    }

    @Test
    fun aQueuedAddBecomesAReplacementOfTheRunningJar() {
        val other = Entry("other", "Other", "", "2", "", "", "/staging/other.jar", "o", "", 1)
        val added = Entry("hotwire", "Hotwire", "", "2", "", "", "/staging/hotwire-2.jar", "new", "", 2)
        val queue = Queue(mutableListOf(other, added))

        HotwireBridge.replaceQueued(queue, Path.of("/store/hotwire-2.jar"), "1", Path.of("/mods/hotwire-1.jar"), "old")

        assertEquals(
            listOf(other, Entry("hotwire", "Hotwire", "1", "2", "hotwire-1.jar", "old", "/staging/hotwire-2.jar", "new", "", 2)),
            queue.entries,
        )
    }
}
