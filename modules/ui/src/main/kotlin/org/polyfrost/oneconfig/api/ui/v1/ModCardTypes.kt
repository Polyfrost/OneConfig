package org.polyfrost.oneconfig.api.ui.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager

public object ModCardTypes {
    private val LOGGER = LogManager.getLogger("OneConfig/ModCardTypes")
    private val lock = Any()

    @Volatile
    private var typesById: Map<String, ModCardType> = emptyMap()

    @Volatile
    private var sortedTypes: List<ModCardType> = emptyList()

    @Volatile
    private var assignments: Map<String, String> = emptyMap()

    @Volatile
    private var resolvers: List<ModCardTypeResolver> = emptyList()

    @JvmStatic
    public var revision: Int by mutableIntStateOf(0)
        private set

    @JvmStatic
    public fun register(type: ModCardType) {
        if (type.id.isBlank() || type.title.isBlank()) {
            LOGGER.warn("Ignoring mod card type with a blank id or title: {}", type)
            return
        }
        synchronized(lock) {
            if (typesById[type.id] != null) LOGGER.debug("Replacing mod card type {}", type.id)
            typesById = typesById + (type.id to type)
            sortedTypes = typesById.values.sortedWith(TypeOrder)
        }
        bump()
    }

    @JvmStatic
    public fun assign(modId: String, typeId: String) {
        val key = normalise(modId)
        if (key.isEmpty() || typeId.isBlank()) {
            LOGGER.warn("Ignoring mod card type assignment with a blank mod id or type id")
            return
        }
        synchronized(lock) { assignments = assignments + (key to typeId) }
        bump()
    }

    @JvmStatic
    public fun assignAll(typeId: String, vararg modIds: String) {
        if (typeId.isBlank()) {
            LOGGER.warn("Ignoring mod card type assignment with a blank type id")
            return
        }
        val keys = modIds.map(::normalise).filter { it.isNotEmpty() }
        if (keys.isEmpty()) return
        synchronized(lock) { assignments = assignments + keys.associateWith { typeId } }
        bump()
    }

    @JvmStatic
    public fun registerResolver(resolver: ModCardTypeResolver) {
        synchronized(lock) {
            if (resolver in resolvers) return
            resolvers = resolvers + resolver
        }
        bump()
    }

    @JvmStatic
    public fun types(): List<ModCardType> = sortedTypes

    @JvmStatic
    public fun byId(typeId: String): ModCardType? = typesById[typeId]

    @JvmStatic
    public fun typeFor(modId: String): ModCardType? {
        val explicit = keysFor(modId).firstNotNullOfOrNull(assignments::get)
        val id = explicit ?: resolvers.firstNotNullOfOrNull { safeResolve(it, modId) }
        return id?.let(typesById::get)
    }

    private val TypeOrder: Comparator<ModCardType> =
        compareByDescending<ModCardType> { it.priority }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            .thenBy { it.id }

    private fun normalise(modId: String) = modId.trim().removeSuffix(".json").lowercase()

    private fun keysFor(modId: String) = sequenceOf(
        modId,
        modId.removeSuffix(".json"),
        modId.substringBefore('/'),
        modId.substringBefore('/').removeSuffix(".json"),
    ).map { it.trim().lowercase() }.distinct()

    private fun safeResolve(resolver: ModCardTypeResolver, modId: String): String? =
        try {
            resolver.typeIdFor(modId)?.takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            synchronized(lock) { resolvers = resolvers - resolver }
            LOGGER.warn("Mod card type resolver {} failed; dropping it", resolver, t)
            null
        }

    private fun bump() {
        Snapshot.withMutableSnapshot { revision++ }
    }
}
