@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:JvmName("PropertyStates")

package org.polyfrost.oneconfig.api.ui.v1.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import java.util.function.Predicate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
@Composable
public fun <T> Property<T>.collectAsState(): State<T?> {
    val property = this
    val state = remember(property) { PropertyValueState(property, mutableIntStateOf(0)) }
    DisposableEffect(state) {
        if (property.get() !== state.lastRead) state.invalidate()
        val callback = Predicate<T> {
            state.invalidate()
            false
        }
        property.addCallback(callback)
        onDispose { property.removeCallback(callback) }
    }
    return state
}

private class PropertyValueState<T>(
    private val property: Property<T>,
    private val version: MutableIntState,
) : State<T?> {
    var lastRead: T? = null
        private set

    override val value: T?
        get() {
            version.intValue
            return property.get().also { lastRead = it }
        }

    fun invalidate() {
        version.intValue++
    }
}

@ExperimentalOneConfigUiApi
public fun <T> Property<T>.asFlow(): Flow<T?> {
    val property = this
    return callbackFlow<Unit> {
        val callback = Predicate<T> {
            trySend(Unit)
            false
        }
        property.addCallback(callback)
        trySend(Unit)
        awaitClose { property.removeCallback(callback) }
    }.buffer(Channel.CONFLATED).map { property.get() }
}
