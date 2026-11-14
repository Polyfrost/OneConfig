package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.MultiSelect
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.internal.ui.api.settings.MultiSelectDropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.translated

private const val ToggleAll = -1

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
    val allSelected = labels.isNotEmpty() && selectedFlags.count { it } == labels.size
    val selected = labels.indices.filterTo(mutableSetOf()) { selectedFlags.getOrElse(it) { false } }
    val options = if (labels.size > 1) listOf(ToggleAll) + labels.indices else labels.indices.toList()
    val toggleAllLabel = if (allSelected) {
        translated("oneconfig.option.multi_select.deselect_all", "Deselect all")
    } else {
        translated("oneconfig.menu.text.select_all", "Select all")
    }

    MultiSelect(
        selected = selected,
        options = options,
        onChange = { next ->
            val updated = if (ToggleAll in next) {
                BooleanArray(labels.size) { !allSelected }
            } else {
                BooleanArray(labels.size) { it in next }
            }
            selectedFlags = updated
            (data.prop as Property<Any>).set(updated)
        },
        modifier = fieldModifier,
        label = { if (it == ToggleAll) toggleAllLabel else labels[it] },
        enabled = enabled,
        styledLabel = styled?.let { list -> { if (it == ToggleAll) AnnotatedString(toggleAllLabel) else list[it] } },
    )
}
