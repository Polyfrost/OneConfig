//? if > 1.21.10 && fabric && moul_compat {
/*package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import java.util.*

@MoulConfig
class MoulPropertyBuilder internal constructor(option: ProcessedOption) {
    val name: String? = resolveTextGetter(option, "getName")
    val description: String? = resolveTextGetter(option, "getDescription")

    var setter: (Any) -> Unit = option::set
    var getter: () -> Any = option::get

    val metadata: MutableMap<String, Any> = mutableMapOf()

    fun build() = Properties.functional(
        id = UUID.randomUUID().toString(),
        getter = getter,
        setter = setter,
        name = name,
        description = description
    ).apply {
        this@MoulPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
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