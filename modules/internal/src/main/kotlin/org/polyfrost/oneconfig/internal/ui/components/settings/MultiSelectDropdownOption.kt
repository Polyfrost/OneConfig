package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.MultiSelectDropdownOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Suppress("UNCHECKED_CAST")
@Composable
fun MultiSelectDropdownOption(data: MultiSelectDropdownOptionData) {
    val theme = LocalTheme.current

    val options: List<Any> = remember(data.prop) {
        data.optionLabels ?: data.options?.toList() ?: emptyList()
    }

    // checkable means the property is an Array<Boolean> indexed by option
    // otherwise it is an Int holding the selected index where -1 is none
    var selectedFlags by remember(data.prop) {
        if (data.checkable) {
            val v = data.prop.shown()
            val arr: BooleanArray = when (v) {
                is BooleanArray -> v
                is Array<*> -> BooleanArray(options.size) { i -> v.getOrNull(i) == true }
                else -> BooleanArray(options.size)
            }
            mutableStateOf(arr.copyOf())
        } else {
            mutableStateOf(BooleanArray(0))
        }
    }

    var selectedIdx by remember(data.prop) {
        if (!data.checkable) {
            val v = data.prop.shown()
            mutableStateOf(if (v is Int) v else -1)
        } else {
            mutableStateOf(-1)
        }
    }

    var expanded by remember { mutableStateOf(false) }

    val triggerLabel = when {
        !data.checkable -> options.getOrElse(selectedIdx) { "—" }
        else -> {
            val count = selectedFlags.count { it }
            when {
                count == 0 -> "None selected"
                count == options.size -> "All selected"
                else -> "$count / ${options.size} selected"
            }
        }
    }

    DropdownFrame(triggerLabel, expanded, { expanded = it }) {
        if (data.checkable && options.size > 1) {
            val allSelected = selectedFlags.count { it } == options.size
            val toggleAll: () -> Unit = {
                val updated = BooleanArray(options.size) { !allSelected }
                selectedFlags = updated
                (data.prop as Property<Any>).set(updated)
            }
            DropdownItem(
                if (allSelected) "Deselect all" else "Select all",
                leading = { CheckboxIndicator(checked = allSelected, onToggle = toggleAll) },
                onClick = toggleAll,
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor))
        }
        options.forEachIndexed { index, option ->
            if (data.checkable) {
                val toggle: () -> Unit = {
                    val updated = selectedFlags.copyOf()
                    updated[index] = !updated[index]
                    selectedFlags = updated
                    (data.prop as Property<Any>).set(updated)
                }
                DropdownItem(
                    option,
                    icon = data.icon,
                    leading = { CheckboxIndicator(checked = selectedFlags.getOrElse(index) { false }, onToggle = toggle) },
                    onClick = toggle,
                )
            } else {
                DropdownItem(option, selected = selectedIdx == index, icon = data.icon) {
                    selectedIdx = index
                    (data.prop as Property<Any>).set(index)
                    expanded = false
                }
            }
        }
    }
}
