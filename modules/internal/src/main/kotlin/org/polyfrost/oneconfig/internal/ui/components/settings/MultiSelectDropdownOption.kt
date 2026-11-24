package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.MultiSelect
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.internal.ui.api.settings.MultiSelectDropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown

@Composable
fun MultiSelectDropdownOption(data: MultiSelectDropdownOptionData) {
    MultiSelectDropdownOption(data, true)
}

@Suppress("UNCHECKED_CAST")
@Composable
fun MultiSelectDropdownOption(data: MultiSelectDropdownOptionData, enabled: Boolean) {
    val stamp = languageStamp()
    val rawLabels: List<Any> = remember(data.prop, stamp) {
        data.optionLabels ?: data.options?.toList() ?: emptyList()
    }
    val labels: List<String> = remember(rawLabels) { rawLabels.map { it.asPlainLabel() } }
    val styled = remember(rawLabels) { rawLabels.styledLabelsOrNull() }
    val fieldModifier = Modifier.width(LocalOptionWidth.current).heightIn(min = OptionControlHeight)

    if (!data.checkable) {
        var selectedIdx by remember(data.prop) { mutableStateOf(data.prop.shown() as? Int ?: -1) }
        Select(
            selected = selectedIdx,
            options = labels.indices.toList(),
            onSelect = { index ->
                selectedIdx = index
                (data.prop as Property<Any>).set(index)
            },
            modifier = fieldModifier,
            label = { labels[it] },
            enabled = enabled,
            placeholder = "—",
            styledLabel = styled?.let { list -> { list[it] } },
        )
        return
    }

    var selectedFlags by remember(data.prop) {
        val arr: BooleanArray = when (val v = data.prop.shown()) {
            is BooleanArray -> v
            is Array<*> -> BooleanArray(labels.size) { i -> v.getOrNull(i) == true }
            else -> BooleanArray(labels.size)
        }
        mutableStateOf(arr.copyOf())
    }
    val selected = labels.indices.filterTo(mutableSetOf()) { selectedFlags.getOrElse(it) { false } }

    MultiSelect(
        selected = selected,
        options = labels.indices.toList(),
        onChange = { next ->
            val updated = BooleanArray(labels.size) { it in next }
            selectedFlags = updated
            (data.prop as Property<Any>).set(updated)
        },
        modifier = fieldModifier,
        label = { labels[it] },
        enabled = enabled,
        styledLabel = styled?.let { list -> { list[it] } },
        selectAll = true,
    )
}
