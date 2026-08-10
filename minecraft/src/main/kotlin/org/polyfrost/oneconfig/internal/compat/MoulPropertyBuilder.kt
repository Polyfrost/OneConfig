//? if > 1.21.10 && fabric && moul_compat {
package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.internal.compat.CompatIds.idPart
import org.polyfrost.oneconfig.internal.compat.CompatIds.uniqueId
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import org.polyfrost.oneconfig.utils.v1.WrappingUtils
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

@MoulConfig
class MoulPropertyBuilder internal constructor(option: ProcessedOption) {
    val path: String? = runCatching { option.path }.getOrNull()?.takeIf { it.isNotBlank() }
    val name: String? = resolveTextGetter(option, "getName")
    val description: String? = resolveTextGetter(option, "getDescription")

    private var getterReplaced = false

    var setter: (Any) -> Unit = option::set
    var getter: () -> Any = option::get
        set(value) {
            field = value
            getterReplaced = true
        }

    val metadata: MutableMap<String, Any> = mutableMapOf()

    val backingField: Field? = resolveBackingField(option)

    val declaringClass: Class<*>? get() = backingField?.declaringClass

    private val snapshotKey: String? = backingField?.let { "${it.declaringClass.name}#${it.name}" }

    fun build(usedIds: MutableSet<String>) = Properties.functional(
        id = uniqueId(usedIds, idPart(path ?: snapshotKey ?: name, "option")),
        getter = getter,
        setter = setter,
        name = name,
        description = description
    ).apply {
        snapshotKey?.let { addMetadata("oc_snapshot_key", it) }
        if (isRepoConfigField(backingField)) addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        else codeDefault()?.let { addMetadata("default", it) }
        this@MoulPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
    }

    private fun codeDefault(): Any? {
        if (getterReplaced) return null
        val field = backingField ?: return null
        if (!WrappingUtils.isSimpleClass(field.type)) return null
        if (Modifier.isStatic(field.modifiers)) return null
        return runCatching { field.isAccessible = true; field.get(pristine(field.declaringClass)) }.getOrNull()
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

    private companion object {
        private val pristines = ConcurrentHashMap<Class<*>, Optional<Any>>()

        fun pristine(cls: Class<*>): Any? = pristines.computeIfAbsent(cls) {
            Optional.ofNullable(
                runCatching {
                    if (it.enclosingClass != null && !Modifier.isStatic(it.modifiers)) null
                    else it.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
                }.getOrNull()
            )
        }.orElse(null)
    }
}
//? }