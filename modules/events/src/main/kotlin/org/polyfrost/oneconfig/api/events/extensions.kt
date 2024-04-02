@file:JvmSynthetic

package org.polyfrost.oneconfig.api.events

import org.polyfrost.oneconfig.api.events.event.Event
import org.polyfrost.oneconfig.api.events.invoke.EventHandler


/**
 * Kotlin specific API for registering of event handlers. Intended usage:
 *
 * ```
 * eventHandler { event: RawKeyEvent ->
 *     println("Key event: $event")
 * }.register()
 * ```
 */
@JvmSynthetic // asm: method is hidden from Java API as it cannot be used (reified)
@EventHandlerKt
inline fun <reified E : Event> eventHandler(crossinline handler: (E) -> Unit) = object : EventHandler<E>() {
    override fun handle(event: E) = handler(event)

    override fun getEventClass() = E::class.java
}

/** makes code colored!! */
@DslMarker
private annotation class EventHandlerKt