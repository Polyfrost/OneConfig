@file:OptIn(ExperimentalOneConfigUiApi::class)
@file:JvmName("HudPreview")

package org.polyfrost.oneconfig.api.hud.v1

import java.util.IdentityHashMap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

private object HeldPreviews {
    private val held = IdentityHashMap<Hud, HashMap<String, Any?>>()
    private var session = Int.MIN_VALUE

    @Suppress("UNCHECKED_CAST")
    fun <T> hold(hud: Hud, key: String, live: () -> T?): T? = synchronized(this) {
        val current = HudManager.previewSession
        if (session != current) {
            held.clear()
            session = current
        }
        val values = held.getOrPut(hud) { HashMap() }
        if (key !in values) {
            values[key] = try {
                live()
            } catch (e: Throwable) {
                if (e is VirtualMachineError) throw e
                null
            }
        }
        values[key] as T?
    }
}

@ExperimentalOneConfigUiApi
public fun <T> Hud.previewValue(key: String, live: () -> T?, demo: T): T {
    if (!HudManager.isEditing) return live() ?: demo
    return HeldPreviews.hold(this, key, live) ?: demo
}
