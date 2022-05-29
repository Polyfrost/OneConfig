package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.utils.TickDelay

/**
 * Schedules a Runnable to be called after a certain amount of ticks.
 *
 * @see TickDelay
 */
fun tick(ticks: Int, block: () -> Unit) = TickDelay(block, ticks)