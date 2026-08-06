package org.polyfrost.oneconfig.internal.ui.compose

import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.IdentityHashMap

object ComposeSupport {
    private val LOG = LoggerFactory.getLogger(ComposeSupport::class.java)

    @Volatile
    private var recordedFailure: String? = null

    private val probed: String? by lazy { probe() }

    fun unavailableReason(): String? = recordedFailure ?: probed

    val isAvailable: Boolean get() = unavailableReason() == null

    fun recordSceneFailure(error: Throwable) {
        if (recordedFailure != null) return
        val fatal = describe(error)
        if (fatal == null) {
            LOG.error("Compose scene creation failed; will retry on the next open.", error)
            return
        }
        recordedFailure = fatal
        LOG.error("Compose is unavailable on this system; the OneConfig UI has been disabled.", error)
    }

    private fun describe(error: Throwable): String? {
        var e: Throwable? = error
        val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        while (e != null && seen.add(e)) {
            val message = e.message.orEmpty()
            when {
                e is UnsatisfiedLinkError || "UnsatisfiedLinkError" in message ->
                    return "OneConfig's UI can't start: the Skia native library failed to load for " +
                        "$osLabel. This platform isn't supported by OneConfig's renderer."

                "java.awt" in message || "Toolkit" in message || "libawt" in message ->
                    return "OneConfig's UI can't start: this Java runtime has no working AWT support " +
                        "(java.awt.Toolkit failed to initialize). Try a full JDK/JRE rather than a " +
                        "headless or trimmed-down one."
            }
            e = e.cause
        }
        return null
    }

    private val osLabel: String
        get() = "${System.getProperty("os.name", "?")} ${System.getProperty("os.arch", "?")}"

    private fun probe(): String? {
        if (System.getProperty(DISABLE_PROPERTY) != null) {
            return "OneConfig's UI is disabled by the -D$DISABLE_PROPERTY JVM flag."
        }
        if (!classExists("java.awt.Toolkit") || !classExists("org.jetbrains.skiko.Library")) {
            return "OneConfig's UI can't start on $osLabel: a required library (AWT or Skiko) is " +
                "missing from this Java runtime. Try a full JDK/JRE."
        }
        return null
    }

    private const val DISABLE_PROPERTY = "oneconfig.ui.disable"

    private fun classExists(name: String): Boolean = runCatching {
        Class.forName(name, false, ComposeSupport::class.java.classLoader)
    }.isSuccess
}
