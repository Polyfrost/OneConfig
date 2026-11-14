package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.internal.ui.api.settings.DropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.*

@Composable
fun DropdownOption(data: DropdownOptionData) {
    DropdownOption(data, true)
}

@Composable
fun DropdownOption(data: DropdownOptionData, enabled: Boolean) {
    val enumClass = when {
        data.prop.type.isEnum -> data.prop.type
        data.prop.type.superclass?.isEnum == true -> data.prop.type.superclass
        else -> null
    }
    val values: List<Any?> = when {
        enumClass != null -> data.optionValues?.takeIf { it.isNotEmpty() } ?: enumClass.enumConstants.toList()
        else -> emptyList()
    }
    val languageStamp = languageStamp()
    val options: List<Any> = remember(data.prop, languageStamp) {
        when {
            enumClass != null -> values.mapIndexed { index, value ->
                data.options?.getOrNull(index) ?: (value as Enum<*>).name
            }
            data.options != null -> data.options!!
            else -> emptyList()
        }
    }
    val optionValues = data.optionValues ?: options

    val initialValue = data.prop.shown()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(
            when (initialValue) {
                is Enum<*> -> values.indexOf(initialValue).takeIf { it >= 0 } ?: 0
                is String -> optionValues.indexOf(initialValue).takeIf { it >= 0 } ?: 0
                else -> initialValue as? Int ?: 0
            }
        )
    }

    Dropdown(options, selectedIdx, { index ->
        selectedIdx = index
        @Suppress("UNCHECKED_CAST")
        if (enumClass != null) {
            (data.prop as Property<Any>).set(values[index])
        } else if (data.prop.type == String::class.java) {
            (data.prop as Property<Any>).set(optionValues.getOrElse(index) { options[index] })
        } else {
            (data.prop as Property<Any>).set(index)
        }
    }, LocalOptionWidth.current, enabled)
}

/** [options] can be anything [Text] can show, such as translated text */
@Composable
fun Dropdown(options: List<Any>, selected: Int, onSelect: (Int) -> Unit, width: Dp = LocalOptionWidth.current) {
    Dropdown(options, selected, onSelect, width, true)
}

@Composable
fun Dropdown(options: List<Any>, selected: Int, onSelect: (Int) -> Unit, width: Dp, enabled: Boolean) {
    val stamp = languageStamp()
    val labels = remember(options, stamp) { options.map { it.asPlainLabel() } }
    val styled = remember(options, stamp) { options.styledLabelsOrNull() }
    Select(
        selected = selected,
        options = labels.indices.toList(),
        onSelect = onSelect,
        modifier = Modifier.width(width).heightIn(min = OptionControlHeight),
        label = { labels[it] },
        enabled = enabled,
        placeholder = "—",
        styledLabel = styled?.let { list -> { list[it] } },
    )
}
