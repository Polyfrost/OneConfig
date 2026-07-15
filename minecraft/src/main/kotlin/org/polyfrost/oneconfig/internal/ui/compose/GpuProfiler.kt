package org.polyfrost.oneconfig.internal.ui.compose

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL33
import org.slf4j.LoggerFactory

object GpuProfiler {
    private val LOG = LoggerFactory.getLogger(GpuProfiler::class.java)

    enum class Mode { TIMER_QUERY, FINISH, DISABLED }

    val enabled: Boolean = System.getProperty("oneconfig.debug.gpuprofile") == "true"
    val perSection: Boolean = enabled && System.getProperty("oneconfig.debug.gpuprofile.sections") == "true"
    private val modeOverride: String? = System.getProperty("oneconfig.debug.gpuprofile.mode")

    private const val REPORT_INTERVAL_MS = 2000L
    private const val MAX_PENDING = 8

    private var mode: Mode? = null

    private class Frame {
        val names = ArrayList<String>()
        val queries = ArrayList<Int>()
    }

    private val queryPool = ArrayList<Int>()
    private val pending = ArrayList<Frame>()
    private var current: Frame? = null

    private var finishActive = false
    private var lastMarkNanos = 0L
    private var frameStartNanos = 0L

    private class Stat {
        var totalNanos = 0L
        var samples = 0
    }

    private val stats = LinkedHashMap<String, Stat>()
    private var lastReportAt = 0L

    private fun resolveMode(): Mode {
        mode?.let { return it }
        val resolved = when (modeOverride) {
            "finish" -> Mode.FINISH.also { LOG.info("GpuProfiler: mode forced to glFinish") }
            "query" -> Mode.TIMER_QUERY.also { LOG.info("GpuProfiler: mode forced to timer queries") }
            else -> try {
                val bits = GL15.glGetQueryi(GL33.GL_TIMESTAMP, GL15.GL_QUERY_COUNTER_BITS)
                if (bits > 0) {
                    LOG.info("GpuProfiler: using GL_TIMESTAMP queries ({} counter bits)", bits)
                    Mode.TIMER_QUERY
                } else {
                    LOG.warn(
                        "GpuProfiler: driver reports 0 timer-query counter bits (expected on macOS); " +
                            "falling back to glFinish timing. CPU and GPU are serialised in this mode, " +
                            "so FPS is not meaningful - compare sections to each other only."
                    )
                    Mode.FINISH
                }
            } catch (e: Throwable) {
                LOG.warn("GpuProfiler: timer-query probe failed, falling back to glFinish timing", e)
                Mode.FINISH
            }
        }
        mode = resolved
        return resolved
    }

    private fun acquireQuery(): Int =
        if (queryPool.isEmpty()) GL33.glGenQueries() else queryPool.removeAt(queryPool.size - 1)

    fun frameBegin() {
        if (!enabled) return
        try {
            when (resolveMode()) {
                Mode.TIMER_QUERY -> {
                    current?.let { recycle(it); current = null }
                    poll()
                    if (pending.size >= MAX_PENDING) return
                    current = Frame()
                    mark("start")
                }
                Mode.FINISH -> {
                    GL11.glFinish()
                    finishActive = true
                    frameStartNanos = System.nanoTime()
                    lastMarkNanos = frameStartNanos
                }
                Mode.DISABLED -> return
            }
        } catch (e: Throwable) {
            disable(e)
        }
    }

    fun mark(name: String) {
        if (!enabled) return
        try {
            when (mode) {
                Mode.TIMER_QUERY -> {
                    val frame = current ?: return
                    val q = acquireQuery()
                    GL33.glQueryCounter(q, GL33.GL_TIMESTAMP)
                    frame.names.add(name)
                    frame.queries.add(q)
                }
                Mode.FINISH -> {
                    if (!finishActive) return
                    GL11.glFinish()
                    val now = System.nanoTime()
                    val stat = stats.getOrPut(name) { Stat() }
                    stat.totalNanos += now - lastMarkNanos
                    stat.samples++
                    lastMarkNanos = now
                }
                else -> return
            }
        } catch (e: Throwable) {
            disable(e)
        }
    }

    fun frameEnd() {
        if (!enabled) return
        try {
            when (mode) {
                Mode.TIMER_QUERY -> {
                    val frame = current ?: return
                    current = null
                    if (frame.queries.size < 2) {
                        recycle(frame)
                        return
                    }
                    pending.add(frame)
                }
                Mode.FINISH -> {
                    if (!finishActive) return
                    finishActive = false
                    val total = stats.getOrPut("TOTAL") { Stat() }
                    total.totalNanos += lastMarkNanos - frameStartNanos
                    total.samples++
                    report()
                }
                else -> return
            }
        } catch (e: Throwable) {
            disable(e)
        }
    }

    private fun poll() {
        while (pending.isNotEmpty()) {
            val frame = pending[0]
            val last = frame.queries[frame.queries.size - 1]
            if (GL15.glGetQueryObjecti(last, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) return
            pending.removeAt(0)
            record(frame)
            recycle(frame)
        }
    }

    private fun record(frame: Frame) {
        val first = GL33.glGetQueryObjecti64(frame.queries[0], GL15.GL_QUERY_RESULT)
        var prev = first
        for (i in 1 until frame.queries.size) {
            val now = GL33.glGetQueryObjecti64(frame.queries[i], GL15.GL_QUERY_RESULT)
            val stat = stats.getOrPut(frame.names[i]) { Stat() }
            stat.totalNanos += now - prev
            stat.samples++
            prev = now
        }
        val total = stats.getOrPut("TOTAL") { Stat() }
        total.totalNanos += prev - first
        total.samples++
        report()
    }

    private fun recycle(frame: Frame) {
        queryPool.addAll(frame.queries)
        frame.queries.clear()
        frame.names.clear()
    }

    private fun report() {
        val now = System.currentTimeMillis()
        if (lastReportAt == 0L) lastReportAt = now
        if (now - lastReportAt < REPORT_INTERVAL_MS) return
        lastReportAt = now

        val total = stats["TOTAL"]?.takeIf { it.samples > 0 } ?: return
        val totalAvgMs = total.totalNanos / total.samples / 1_000_000.0
        val sb = StringBuilder()
        sb.append("GPU [").append(if (mode == Mode.FINISH) "glFinish" else "timestamp")
            .append("] avg over ").append(total.samples).append(" frames")
        if (perSection) sb.append(", per-section flushes ACTIVE - total inflated")
        sb.append(": TOTAL ").append(String.format("%.3f", totalAvgMs)).append("ms")
        for ((name, stat) in stats) {
            if (name == "TOTAL" || stat.samples == 0) continue
            val perHitMs = stat.totalNanos / stat.samples / 1_000_000.0
            val perFrameMs = stat.totalNanos / total.samples / 1_000_000.0
            val pct = if (totalAvgMs > 0) perFrameMs / totalAvgMs * 100.0 else 0.0
            val hitRate = stat.samples * 100.0 / total.samples
            sb.append(" | ").append(name).append(' ')
                .append(String.format("%.3f", perHitMs)).append("ms/hit x")
                .append(String.format("%.0f", hitRate)).append("% = ")
                .append(String.format("%.3f", perFrameMs)).append("ms/frame (")
                .append(String.format("%.1f", pct)).append("%)")
        }
        LOG.info(sb.toString())
        stats.clear()
    }

    private fun disable(e: Throwable) {
        mode = Mode.DISABLED
        current = null
        finishActive = false
        pending.clear()
        LOG.warn("GpuProfiler disabled after error", e)
    }
}
