package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.Multithreading
import java.util.concurrent.TimeUnit

/**
 * Runs the given [block] asynchronously.
 *
 * @see Multithreading.runAsync
 */
fun runAsync(block: () -> Unit) = Multithreading.runAsync(block)

/**
 * Runs the given [block] asynchronously after the given [delay].
 *
 * @see Multithreading.schedule
 */
fun schedule(delay: Long, timeUnit: TimeUnit, block: () -> Unit) = Multithreading.schedule(block, delay, timeUnit)