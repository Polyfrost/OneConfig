package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.internal.ui.api.settings.TextOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown

@Composable
fun TextOption(data: TextOptionData) {
    var text by remember(data.prop) { mutableStateOf(data.strProp.shown() ?: "") }
    val regex = remember(data.regex) { data.regex?.takeIf { it.isNotBlank() }?.let { runCatching { Regex(it) }.getOrNull() } }
    val valid = regex == null || text.isEmpty() || regex.matches(text)

    TextField(
        text,
        { text = it; if (regex == null || it.isEmpty() || regex.matches(it)) data.strProp.set(it) },
        Modifier.width(LocalOptionWidth.current).heightIn(min = if (data.multiline) 96.dp else OptionControlHeight),
        data.placeholder,
        "text-input",
        Int.MAX_VALUE,
        !valid,
        true,
        !data.multiline,
        true,
        14.sp,
        null,
        false,
        null,
        null,
        null,
        null,
    )
}
