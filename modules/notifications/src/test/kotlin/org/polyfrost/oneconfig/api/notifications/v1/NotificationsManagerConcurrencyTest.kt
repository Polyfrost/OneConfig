package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.TextNode
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.runtime.PolyComposeClock
import org.polyfrost.compose.runtime.PolyComposeRuntime
import java.lang.invoke.MethodHandles.publicLookup
import java.lang.invoke.MethodType.methodType
import java.net.URLClassLoader
import java.time.Duration.ofSeconds
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class NotificationsManagerConcurrencyTest {
    @BeforeEach
    @AfterEach
    fun reset() {
        assumeFalse(wedged.get(), "an earlier test left threads stuck in NotificationsManager")
        assertTimeoutPreemptively(ofSeconds(30)) {
            NotificationsManager.clearAll()
            NotificationsManager.clearHistory()
        }
    }

    @Test
    fun concurrentPushesKeepStateReadable() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val writing = AtomicBoolean(true)
        val writers = List(4) { w ->
            thread(isDaemon = true) {
                repeat(5000) { i ->
                    try {
                        val n = Notifications.send("t$w", "m$i")
                        if (i % 3 == 0) NotificationsManager.removeFromHistory(n)
                        if (i % 7 == 0) NotificationsManager.markAllRead()
                        NotificationsManager.dismiss(n)
                    } catch (t: Throwable) {
                        errors += t
                    }
                }
            }
        }
        val readers = List(2) {
            thread(isDaemon = true) {
                try {
                    while (writing.get()) {
                        for (list in listOf(NotificationsManager.active, NotificationsManager.history)) {
                            val size = list.size
                            assertEquals(size, list.count { true })
                            assertEquals(size, list.size)
                        }
                        assertTrue(NotificationsManager.unreadCount >= 0)
                    }
                } catch (t: Throwable) {
                    errors += t
                }
            }
        }
        writers.forEach { it.join(30_000) }
        writing.set(false)
        readers.forEach { it.join(5_000) }
        assertFinished(*(writers + readers).toTypedArray())

        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
        val ui = Snapshot.takeMutableSnapshot()
        try {
            ui.enter {
                assertTrue(NotificationsManager.unreadCount >= 0)
                assertEquals(50, NotificationsManager.history.size)
            }
        } finally {
            ui.dispose()
        }

        NotificationsManager.clearHistory()
        val sent = List(3) { Notifications.send("t", "m$it") }
        assertEquals(3, NotificationsManager.unreadCount)
        NotificationsManager.removeFromHistory(sent[1])
        assertEquals(listOf(sent[2], sent[0]), NotificationsManager.history)
        NotificationsManager.markAllRead()
        assertEquals(0, NotificationsManager.unreadCount)
    }

    @Test
    fun mutatingWhileObserverReadsGetterDoesNotDeadlock() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val observer = SnapshotStateObserver { it() }
        observer.start()
        val readerInObserver = CountDownLatch(1)
        val writerInLock = CountDownLatch(1)
        val derived = derivedStateOf {
            readerInObserver.countDown()
            assertTrue(writerInLock.await(5, SECONDS)) { "writer never took the lock" }
            NotificationsManager.count
        }
        val reader = worker(errors) { observer.observeReads(Unit, {}) { derived.value } }
        val writer = worker(errors) {
            NotificationsManager.mutate {
                writerInLock.countDown()
                assertTrue(readerInObserver.await(5, SECONDS)) { "reader never entered the observer" }
            }
        }
        reader.join(10_000)
        writer.join(10_000)
        observer.stop()
        assertFinished(reader, writer)
        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
    }

    @Test
    fun observedReadInsideMutateDoesNotDeadlockWithObserverCallback() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val n = Notifications.send("t", "m")
        val trigger = mutableIntStateOf(0)
        val writerInLock = CountDownLatch(1)
        val callbackRunning = CountDownLatch(1)
        val fired = AtomicBoolean(false)
        val observer = SnapshotStateObserver { it() }
        observer.start()
        observer.observeReads("callback", { _: String ->
            if (fired.compareAndSet(false, true)) {
                callbackRunning.countDown()
                assertTrue(writerInLock.await(5, SECONDS))
                NotificationsManager.count
            }
        }) { trigger.intValue }
        val writer = worker(errors) {
            observer.observeReads("writer", { _: String -> }) {
                NotificationsManager.mutate {
                    writerInLock.countDown()
                    assertTrue(callbackRunning.await(5, SECONDS)) { "callback never ran" }
                    n.title
                }
            }
        }
        val notifier = worker(errors) {
            assertTrue(writerInLock.await(5, SECONDS))
            Snapshot.global { trigger.intValue++ }
            Snapshot.sendApplyNotifications()
        }
        writer.join(10_000)
        notifier.join(10_000)
        observer.stop()
        assertFinished(writer, notifier)
        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
    }

    @Test
    fun updateIsSeenWhole() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val n = Notifications.send("t0", "m0")
        val sampling = CountDownLatch(1)
        val halfway = CountDownLatch(1)
        val sampledHalfway = CountDownLatch(1)
        val writer = worker(errors) {
            assertTrue(sampling.await(5, SECONDS)) { "reader never started" }
            for (i in 1..20_000) {
                n.update("t$i", "m$i", null, null)
                if (i == 10_000) {
                    halfway.countDown()
                    assertTrue(sampledHalfway.await(5, SECONDS)) { "reader never sampled halfway" }
                }
            }
        }
        val deadline = System.nanoTime() + SECONDS.toNanos(10)
        var torn: NotificationState? = null
        var midway = 0
        sampling.countDown()
        while (writer.isAlive && torn == null && System.nanoTime() < deadline) {
            val writerPaused = halfway.count == 0L
            val state = n.state
            if (state.title.drop(1) != state.message.drop(1)) torn = state
            if (state.title != "t0" && state.title != "t20000") midway++
            if (writerPaused) sampledHalfway.countDown()
        }
        writer.join(10_000)
        assertFinished(writer)
        assertNull(torn)
        assertTrue(midway >= 1) { "no sample overlapped the writer" }
        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
    }

    @Test
    fun concurrentUpdatesOfDifferentFieldsAllSurvive() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val n = Notifications.send("t0", "m0")
        val rounds = 5000
        val barrier = CyclicBarrier(2) { assertEquals(n.title.drop(1), n.message.drop(1)) }
        val writers = listOf<(Int) -> Unit>({ n.update(title = "t$it") }, { n.update(message = "m$it") }).map { write ->
            worker(errors) {
                for (i in 1..rounds) {
                    barrier.await(5, SECONDS)
                    write(i)
                }
                barrier.await(5, SECONDS)
            }
        }
        writers.forEach { it.join(30_000) }
        assertFinished(*writers.toTypedArray())
        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
        assertEquals("t$rounds", n.title)
        assertEquals("m$rounds", n.message)
    }

    @Test
    fun toastTimesOutRestartsOnUpdateAndLeavesOnDismiss() {
        ToastViewport.width = 1000f
        ToastViewport.height = 1000f
        val engine = PolyFont.engine
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = 0f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(0f, 0f, 0f, 0f, 0f)
        }
        val clock = PolyComposeClock()
        val runtime = PolyComposeRuntime(clock)
        try {
            runtime.setContent { NotificationToasts() }
            val n = Notifications.send("t", "m")
            var frame = 0L
            fun advance(millis: Long) = repeat((millis / 50).toInt()) { clock.frame(++frame * 50_000_000L) }
            fun texts(node: PolyNode): List<String> = listOfNotNull((node as? TextNode)?.text) + node.children.flatMap(::texts)
            advance(3000)
            assertEquals(listOf("t", "m"), texts(runtime.root))
            n.update("t2", "m2", null, null)
            advance(3000)
            assertEquals(listOf("t2", "m2"), texts(runtime.root))
            assertTrue(n in NotificationsManager.active) { "timer not restarted by update" }
            advance(2000)
            assertFalse(n in NotificationsManager.active) { "toast never timed out" }
            val pinned = Notifications.send("t", "m", persistent = true)
            advance(1000)
            pinned.dismiss()
            assertTrue(pinned.state.dismissRequested)
            advance(1000)
            assertFalse(pinned in NotificationsManager.active) { "toast not removed after dismiss" }
            val removed = Notifications.send("t", "m", persistent = true)
            advance(1000)
            NotificationsManager.remove(removed)
            advance(1000)
            assertFalse(removed in NotificationsManager.active) { "toast not removed after remove" }
            assertFalse(removed in NotificationsManager.history)
        } finally {
            runtime.dispose()
            PolyFont.engine = engine
        }
    }

    @Test
    fun contentUpdateMarksUnread() {
        val n = Notifications.send("t", "m")
        NotificationsManager.markAllRead()
        n.update(progress = 0.5f)
        assertTrue(n.read)
        n.update(message = "m2")
        assertFalse(n.read)
        assertEquals(1, NotificationsManager.unreadCount)
    }

    @Test
    fun observerCallbackMutatingWhileAnotherThreadMutatesDoesNotDeadlock() {
        val errors = ConcurrentLinkedQueue<Throwable>()
        val callbacks = AtomicInteger()
        val onChanged: (Unit) -> Unit = {
            callbacks.incrementAndGet()
            NotificationsManager.markAllRead()
        }
        val observer = SnapshotStateObserver { it() }
        observer.start()
        val frame = mutableIntStateOf(0)
        val writing = AtomicBoolean(true)
        val render = worker(errors) {
            while (writing.get()) {
                observer.observeReads(Unit, onChanged) { frame.intValue + NotificationsManager.count }
                frame.intValue++
                Snapshot.sendApplyNotifications()
            }
        }
        val writer = worker(errors) {
            while (writing.get() && callbacks.get() < 1000) NotificationsManager.dismiss(Notifications.send("t", "m"))
        }
        try {
            writer.join(10_000)
        } finally {
            writing.set(false)
            writer.join(1_000)
            render.join(10_000)
            observer.stop()
        }
        assertFinished(render, writer)
        assertTrue(callbacks.get() >= 1000) { "only ${callbacks.get()} callbacks" }
        assertTrue(errors.isEmpty()) { "failed: ${errors.firstOrNull()}" }
    }

    @Test
    fun concurrentMutationsAreAllPublished() {
        val before = NotificationsManager.revision
        val writers = List(4) { thread(isDaemon = true) { repeat(2000) { NotificationsManager.mutate {} } } }
        writers.forEach { it.join(30_000) }
        assertFinished(*writers.toTypedArray())
        assertEquals(before + 8000, NotificationsManager.revision)
    }

    @Test
    fun progressPushedFromCallerSnapshotIsPublishedStarted() {
        val caller = Snapshot.takeMutableSnapshot()
        val toast = try {
            caller.enter {
                val n = Notifications.progress("p", "m")
                assertEquals(0f, n.progressOrNull())
                assertTrue(n.tracksProgress)
                Snapshot.global {
                    assertTrue(n in NotificationsManager.active)
                    assertEquals(0f, n.progressOrNull())
                    assertTrue(n.tracksProgress)
                }
                n
            }.also { caller.apply().check() }
        } finally {
            caller.dispose()
        }
        toast.update(null, null, NotificationType.SUCCESS, null)
        assertFalse(toast.tracksProgress)
        assertNull(toast.progressOrNull())
        assertFalse(Notifications.send("t", "m").tracksProgress)
    }

    @Test
    fun callerSnapshotReadsItsOwnWrites() {
        val before = NotificationsManager.active
        val caller = Snapshot.takeMutableSnapshot()
        val n = try {
            caller.enter {
                val n = Notifications.progress("p", "m")
                n.update("t2", "m2", null, 0.5f)
                assertEquals("t2", n.title)
                assertEquals("m2", n.message)
                assertEquals(0.5f, n.progressOrNull())
                assertTrue(n in NotificationsManager.active)
                assertTrue(NotificationsManager.contains(n))
                assertEquals(1, NotificationsManager.count)
                assertEquals(n, NotificationsManager.history.first())
                n
            }.also { caller.apply().check() }
        } finally {
            caller.dispose()
        }
        assertTrue(before.isEmpty())
        assertEquals("t2", n.title)
        assertEquals(0.5f, n.progressOrNull())
        assertEquals(listOf(n), NotificationsManager.active)
    }

    @Test
    fun composeReadersAreInvalidatedByEveryMutation() {
        val n = Notifications.progress("p", "m")
        val getters = mapOf<String, () -> Any?>(
            "active" to { NotificationsManager.active },
            "history" to { NotificationsManager.history },
            "unreadCount" to { NotificationsManager.unreadCount },
            "count" to { NotificationsManager.count },
            "contains" to { NotificationsManager.contains(n) },
            "title" to { n.title },
            "message" to { n.message },
            "type" to { n.type },
            "read" to { n.read },
            "hovered" to { n.hovered },
            "dismissRequested" to { n.dismissRequested },
            "progressOrNull" to { n.progressOrNull() },
            "tracksProgress" to { n.tracksProgress },
        )
        val mutations = mapOf<String, () -> Unit>(
            "push" to { Notifications.send("t", "m") },
            "update" to { n.update(progress = 0.25f) },
            "markAllRead" to { NotificationsManager.markAllRead() },
            "dismiss" to { n.dismiss() },
            "clearHistory" to { NotificationsManager.clearHistory() },
        )
        for ((getter, read) in getters) {
            for ((mutation, mutate) in mutations) {
                assertTrue(invalidates(read, mutate)) { "$getter not invalidated by $mutation" }
            }
        }
        assertTrue(n.state.dismissRequested)
    }

    @Test
    fun snapshotsTakenBeforeManagerInitCanRead() {
        val pkg = NotificationsManager::class.java.packageName
        val loader = object : URLClassLoader(arrayOf(NotificationsManager::class.java.protectionDomain.codeSource.location), javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> =
                if (!name.startsWith(pkg)) super.loadClass(name, resolve)
                else synchronized(getClassLoadingLock(name)) { findLoadedClass(name) ?: findClass(name) }
        }
        val dirty = mutableIntStateOf(0)
        val old = Snapshot.takeSnapshot()
        val oldMutable = Snapshot.takeMutableSnapshot()
        val nested = oldMutable.takeNestedMutableSnapshot()
        try {
            loader.use {
                fun countOf(manager: Class<*>) = publicLookup().findVirtual(manager, "getCount", methodType(Int::class.java)).bindTo(manager.getField("INSTANCE").get(null))
                var observed = -1
                val observer = Snapshot.registerApplyObserver { _, _ -> observed = countOf(Class.forName("$pkg.NotificationsManager", true, it)).invokeWithArguments() as Int }
                Snapshot.global { dirty.intValue++ }
                val manager = try {
                    Class.forName("$pkg.NotificationsManager", true, it).also { Snapshot.sendApplyNotifications() }
                } finally {
                    observer.dispose()
                }
                assertEquals(0, observed)
                val notification = Class.forName("$pkg.Notification", false, it)
                val send = publicLookup().findStatic(Class.forName("$pkg.Notifications", false, it), "send", methodType(notification, String::class.java, String::class.java))
                val title = publicLookup().findVirtual(notification, "getTitle", methodType(String::class.java))
                val count = countOf(manager)
                val n = send.invokeWithArguments("t", "m")
                for (snapshot in listOf(old, oldMutable, nested)) {
                    snapshot.enter {
                        assertEquals("t", title.invokeWithArguments(n))
                        assertEquals(1, count.invokeWithArguments())
                    }
                }
            }
        } finally {
            nested.dispose()
            oldMutable.dispose()
            old.dispose()
        }
    }

    @Test
    fun rendererHoverIsPublished() {
        val n = Notifications.send("t", "m")
        assertTrue(invalidates({ n.hovered }) { NotificationsRenderer.updateHovered(n) }) { "hover enter not published" }
        assertTrue(n.hovered)
        assertTrue(invalidates({ n.hovered }) { NotificationsRenderer.updateHovered(null) }) { "hover exit not published" }
        assertFalse(n.hovered)
    }

    private fun invalidates(read: () -> Any?, mutate: () -> Unit): Boolean {
        val reads = HashSet<Any>()
        val snapshot = Snapshot.takeSnapshot { reads.add(it) }
        try {
            snapshot.enter { read() }
        } finally {
            snapshot.dispose()
        }
        Snapshot.sendApplyNotifications()
        val changed = HashSet<Any>()
        val observer = Snapshot.registerApplyObserver { set, _ -> changed.addAll(set) }
        try {
            mutate()
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }
        return reads.any { it in changed }
    }

    private fun assertFinished(vararg threads: Thread) {
        if (threads.none { it.isAlive }) return
        wedged.set(true)
        fail<Unit>("deadlocked: ${threads.joinToString { it.state.toString() }}")
    }

    private fun worker(errors: MutableCollection<Throwable>, block: () -> Unit) = thread(isDaemon = true) {
        try {
            block()
        } catch (t: Throwable) {
            errors += t
        }
    }

    private companion object {
        val wedged = AtomicBoolean(false)
    }
}
