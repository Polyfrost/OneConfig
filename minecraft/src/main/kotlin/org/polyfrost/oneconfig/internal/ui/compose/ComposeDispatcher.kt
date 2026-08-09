package org.polyfrost.oneconfig.internal.ui.compose

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

class ComposeDispatcher : CoroutineDispatcher() {
    private val lock = SynchronizedObject()
    private val tasks = ArrayDeque<Runnable>()
    private val inProgressTasks = atomic(0)

    fun isIdle(): Boolean {
        val isEmpty = synchronized(lock) { tasks.isEmpty() }
        return isEmpty && inProgressTasks.value == 0
    }

    fun runNextTask() {
        val task = synchronized(lock) { tasks.removeFirst() }
        try {
            inProgressTasks.getAndIncrement()
            task.run()
        } finally {
            inProgressTasks.getAndDecrement()
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(lock) { tasks += block }
    }
}