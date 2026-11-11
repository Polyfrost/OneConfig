package org.polyfrost.oneconfig.internal.ui

import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object UiErrors {
    private val LOGGER = LogManager.getLogger("OneConfig/UI")

    private const val MAX_TRACKED = 512

    private val reported = ConcurrentHashMap.newKeySet<String>()

    @JvmStatic
    @JvmOverloads
    fun error(context: String, failure: Throwable, logger: Logger = LOGGER) {
        if (firstTime(context, failure)) logger.error(context, failure)
        else logger.debug("{} (repeated): {}", context, failure.toString())
    }

    @JvmStatic
    @JvmOverloads
    fun warn(context: String, failure: Throwable, logger: Logger = LOGGER) {
        if (firstTime(context, failure)) logger.warn(context, failure)
        else logger.debug("{} (repeated): {}", context, failure.toString())
    }

    private fun firstTime(context: String, failure: Throwable): Boolean {
        if (reported.size >= MAX_TRACKED) reported.clear()
        return reported.add(signature(context, failure))
    }

    private fun signature(context: String, failure: Throwable): String {
        val root = generateSequence(failure) { it.cause.takeIf { cause -> cause !== it } }.take(16).last()
        return "$context|${failure.javaClass.name}|${failure.message}|${failure.stackTrace.firstOrNull()}|${root.javaClass.name}|${root.stackTrace.firstOrNull()}"
    }
}
