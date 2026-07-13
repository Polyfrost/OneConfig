//? if > 1.21.10 && fabric && moul_compat {
/*package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import java.lang.reflect.Field
import java.util.*

@MoulConfig
class MoulPropertyBuilder internal constructor(option: ProcessedOption) {
    val name: String? = resolveTextGetter(option, "getName")
    val description: String? = resolveTextGetter(option, "getDescription")

    var setter: (Any) -> Unit = option::set
    var getter: () -> Any = option::get

    val metadata: MutableMap<String, Any> = mutableMapOf()

    val backingField: Field? = resolveBackingField(option)

    val declaringClass: Class<*>? get() = backingField?.declaringClass

    private val snapshotKey: String? = backingField?.let { "${it.declaringClass.name}#${it.name}" }

    fun build() = Properties.functional(
        id = UUID.randomUUID().toString(),
        getter = getter,
        setter = setter,
        name = name,
        description = description
    ).apply {
        snapshotKey?.let { addMetadata("oc_snapshot_key", it) }
        if (isRepoConfigField(backingField)) addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        this@MoulPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
    }

    private fun resolveBackingField(option: Any): Field? = runCatching {
        val members = option.javaClass.fields.asSequence() + option.javaClass.declaredFields.asSequence()
        members
            .mapNotNull { m -> runCatching { m.isAccessible = true; m.get(option) as? Field }.getOrNull() }
            .firstOrNull()
    }.getOrNull()

    private fun isRepoConfigField(field: Field?): Boolean {
        val declaring = field?.declaringClass?.name ?: return false
        return declaring.contains("RepositoryConfig") || declaring.contains("RepositoryLocation")
    }

    private fun resolveTextGetter(target: Any, methodName: String): String? {
        val value = runCatching {
            target::class.java.getMethod(methodName).invoke(target)
        }.getOrNull()
        return resolveText(value)
    }

    private fun resolveText(value: Any?): String? {
        if (value == null) return null
        if (value is String) return value
        val fromGetText = runCatching {
            value::class.java.getMethod("getText").invoke(value)
        }.getOrNull()
        return when (fromGetText) {
            null -> value.toString()
            is String -> fromGetText
            else -> fromGetText.toString()
        }
    }
}
*///? }