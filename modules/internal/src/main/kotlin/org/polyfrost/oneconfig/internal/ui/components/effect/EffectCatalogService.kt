package org.polyfrost.oneconfig.internal.ui.components.effect

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import java.util.Locale
import java.util.ServiceLoader
import org.jetbrains.skia.Image
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerEntry
import org.polyfrost.oneconfig.internal.ui.components.picker.normalizeIds

enum class EffectCategory { BENEFICIAL, NEUTRAL, HARMFUL }

/** A stable status effect registry ID such as `minecraft:speed` and the localized name shown to the user */
data class EffectDescriptor(
    override val id: String,
    override val displayName: String,
    val category: EffectCategory,
) : PickerEntry

interface EffectCatalogService {
    fun effects(): List<EffectDescriptor>

    fun icon(id: String): Image?
}

object EffectCatalog {
    private val service: EffectCatalogService? by lazy {
        runCatching {
            ServiceLoader.load(EffectCatalogService::class.java, EffectCatalogService::class.java.classLoader)
                .iterator()
                .let { if (it.hasNext()) it.next() else null }
        }.getOrNull()
    }

    private val _revision = mutableIntStateOf(0)

    val revision: Int get() = _revision.intValue

    fun platformService(): EffectCatalogService? = service

    fun effects(): List<EffectDescriptor> = service?.effects().orEmpty()

    fun icon(id: String): Image? = service?.icon(normalizeEffectId(id))

    fun invalidate() {
        Snapshot.withMutableSnapshot { _revision.intValue++ }
    }
}

fun normalizeEffectId(id: String): String {
    val trimmed = id.trim().lowercase(Locale.ROOT)
    return if (trimmed.isEmpty() || ':' in trimmed) trimmed else "minecraft:$trimmed"
}

fun normalizeEffectIds(ids: Iterable<String>): List<String> = normalizeIds(ids.map(::normalizeEffectId))
