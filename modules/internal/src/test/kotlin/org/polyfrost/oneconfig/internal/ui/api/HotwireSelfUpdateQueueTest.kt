package org.polyfrost.oneconfig.internal.ui.api

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HotwireSelfUpdateQueueTest {
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
