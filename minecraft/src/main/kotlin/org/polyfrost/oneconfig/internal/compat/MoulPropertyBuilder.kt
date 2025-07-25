package org.polyfrost.oneconfig.internal.compat

import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig
import java.util.*

@MoulConfig
class MoulPropertyBuilder internal constructor(option: ProcessedOption) {
    val name: String? = option.name
    val description: String? = option.description

    var setter: (Any) -> Unit = option::set
    var getter: () -> Any = option::get

    val metadata: MutableMap<String, Any> = mutableMapOf()

    fun build() = Properties.functional(
        id = UUID.randomUUID().toString(),
        getter = getter,
        setter =    setter,
        name = null,
        description = description
    ).apply {
        this@MoulPropertyBuilder.metadata.entries.forEach { (key, value) -> addMetadata(key, value) }
    }
}