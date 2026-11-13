@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:JvmName("PropertyStates")

package org.polyfrost.oneconfig.api.ui.v1.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import java.util.function.Predicate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
@Composable
public fun <T> Property<T>.collectAsState(): State<T?> {
    val property = this
    val state = remember(property) { mutableStateOf<T?>(property.get(), neverEqualPolicy()) }
    DisposableEffect(property, state) {
        val current = property.get()
        if (state.value !== current) state.value = current
        val callback = Predicate<T> { value ->
            state.value = value
            false
        }
        property.addCallback(callback)
        onDispose { property.removeCallback(callback) }
    }
    return state
}

@ExperimentalOneConfigUiApi
public fun <T> Property<T>.asFlow(): Flow<T?> {
    val property = this
    return callbackFlow<T?> {
        val callback = Predicate<T> { value ->
            trySend(value)
            false
        }
        property.addCallback(callback)
        trySend(property.get())
        awaitClose { property.removeCallback(callback) }
    }.buffer(Channel.CONFLATED)
}
