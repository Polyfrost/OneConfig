package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.RadioGroup
import org.polyfrost.oneconfig.internal.ui.api.settings.RadioButtonOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown

@Composable
fun RadioButtonOption(data: RadioButtonOptionData) {
    RadioButtonOption(data, true)
}

@Composable
fun RadioButtonOption(data: RadioButtonOptionData, enabled: Boolean) {
    val enumClass = when {
        data.prop.type.isEnum -> data.prop.type
        data.prop.type.superclass?.isEnum == true -> data.prop.type.superclass
        else -> null
    }
    val values: List<Any?> = when {
        enumClass != null -> enumClass.enumConstants.toList()
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

    val initialValue = data.prop.shown()
    var selectedIdx by remember(data.prop) {
        mutableStateOf(if (initialValue is Enum<*>) initialValue.ordinal else initialValue as? Int ?: 0)
    }

    val select: (Int) -> Unit = { index ->
        selectedIdx = index
        @Suppress("UNCHECKED_CAST")
        if (enumClass != null) {
            (data.prop as Property<Any>).set(values[index])
        } else {
            (data.prop as Property<Any>).set(index)
        }
    }
    val labels = remember(options, languageStamp) { options.map { it.asPlainLabel() } }
    val styled = remember(options, languageStamp) { options.styledLabelsOrNull() }
    RadioGroup(
        selected = selectedIdx,
        options = labels.indices.toList(),
        onSelect = select,
        modifier = Modifier.widthIn(max = LocalOptionWidth.current),
        label = { labels[it] },
        orientation = Orientation.Horizontal,
        enabled = enabled,
        styledLabel = styled?.let { list -> { list[it] } },
    )
}
