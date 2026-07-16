package org.polyfrost.oneconfig.internal.compat

import org.polyfrost.oneconfig.api.config.v1.Property

internal object CompatHudToggle {
    private val NAMES = setOf("enabled", "enable", "show", "visible")

    fun find(properties: List<Property<*>>): Property<Boolean>? {
        val boolean = properties.filter { it.type == Boolean::class.javaPrimitiveType || it.type == java.lang.Boolean::class.java }
        for (name in NAMES) {
            val match = boolean.firstOrNull { key(it) == name } ?: continue
            @Suppress("UNCHECKED_CAST")
            return match as Property<Boolean>
        }
        return null
    }

    private fun key(property: Property<*>): String {
        val id = property.getMetadata<String>("rconfig_id") ?: property.id
        return id.substringAfterLast('.').substringAfterLast('_').lowercase()
    }
}
